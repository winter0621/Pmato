package csu.mengya.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import csu.mengya.common.Result;
import csu.mengya.dao.ScheduleDao;
import csu.mengya.dao.TodoDao;
import csu.mengya.model.ScheduleEvent;
import csu.mengya.model.TodoItem;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;

/** 学习助手服务：按用户选择提取有限的本地摘要，并调用 OpenAI Responses API。 */
public final class AiAssistantService {
    private static final URI RESPONSES_URI = URI.create("https://api.openai.com/v1/responses");
    private static final String MODEL = "gpt-4.1-mini";
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();
    private static final String INSTRUCTIONS = "你是萌芽专注的中文学习助手。帮助学生拆解待办、规划现实可行的学习时间，"
            + "或回答与学习方法有关的问题。回答应简洁、具体、可执行，优先给出今天能开始的一步。"
            + "只依据提供的数据判断；没有日期或时长时不要编造。你只能提出建议，不能声称已修改待办、日程或开始计时。";

    /** 单次请求在后台线程调用；密钥只作为参数传入，不写入文件或日志。 */
    public Result<String> generate(String apiKey, String mode, String question, boolean includeContext) {
        String key = apiKey == null || apiKey.isBlank() ? System.getenv("OPENAI_API_KEY") : apiKey.trim();
        if (key == null || key.isBlank()) return Result.fail("请输入 OpenAI API Key，或设置 OPENAI_API_KEY 环境变量。");
        try {
            StringBuilder input = new StringBuilder("任务：").append(mode).append('\n');
            if (question != null && !question.isBlank())
                input.append("用户补充：").append(question.strip(), 0, Math.min(question.strip().length(), 2000)).append('\n');
            input.append(includeContext ? buildContext() : "用户未选择发送本地待办和日程。\n");

            JsonObject body = new JsonObject();
            body.addProperty("model", MODEL);
            body.addProperty("instructions", INSTRUCTIONS);
            body.addProperty("input", input.toString());
            body.addProperty("store", false);
            body.addProperty("max_output_tokens", 700);
            HttpRequest request = HttpRequest.newBuilder(RESPONSES_URI)
                    .timeout(Duration.ofSeconds(50))
                    .header("Authorization", "Bearer " + key)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = CLIENT.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() == 401) return Result.fail("API Key 无效，请检查后重试。");
            if (response.statusCode() == 429) return Result.fail("请求过于频繁或账户额度不足，请稍后重试。");
            if (response.statusCode() < 200 || response.statusCode() >= 300)
                return Result.fail("助手请求失败（HTTP " + response.statusCode() + "）。");
            String answer = extractText(response.body());
            return answer.isBlank() ? Result.fail("助手返回了空内容，请重试。") : Result.ok(answer);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Result.fail("请求已取消。");
        } catch (IOException | RuntimeException e) {
            return Result.fail("无法连接学习助手，请检查网络连接后重试。");
        }
    }

    /** 限量读取用户主动同意分享的数据；不发送任务备注或历史会话。 */
    private String buildContext() {
        StringBuilder context = new StringBuilder("本地学习摘要（只供本次建议）：\n");
        int count = 0;
        for (TodoItem todo : new TodoDao().findAll()) {
            if ("done".equals(todo.getStatus())) continue;
            context.append("待办：").append(limit(todo.getTitle(), 100))
                    .append("；优先级 ").append(todo.getPriority())
                    .append("；进度 ").append(todo.getDonePomodoro()).append('/')
                    .append(todo.getEstPomodoro()).append(" 番茄");
            if (todo.getDueDate() != null) context.append("；截止 ").append(todo.getDueDate());
            context.append('\n');
            if (++count == 8) break;
        }
        if (count == 0) context.append("暂无未完成待办。\n");
        count = 0;
        LocalDate today = LocalDate.now();
        for (ScheduleEvent event : new ScheduleDao().findAll()) {
            LocalDate startDate = LocalDate.parse(event.getStartAt().substring(0, 10));
            if ("none".equals(event.getRepeatRule())
                    && (startDate.isBefore(today) || startDate.isAfter(today.plusDays(7)))) continue;
            context.append("日程：").append(limit(event.getTitle(), 100))
                    .append("；").append(event.getStartAt()).append(" 至 ").append(event.getEndAt());
            if (!"none".equals(event.getRepeatRule())) context.append("；重复 ").append(event.getRepeatRule());
            context.append('\n');
            if (++count == 8) break;
        }
        if (count == 0) context.append("未来七天暂无日程。\n");
        return context.toString();
    }

    private String limit(String text, int max) {
        if (text == null) return "";
        String clean = text.replace('\n', ' ').replace('\r', ' ');
        return clean.length() > max ? clean.substring(0, max) : clean;
    }

    /** Responses API 的文本位于 output 中的 message.content[].text。 */
    private String extractText(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonArray output = root.getAsJsonArray("output");
        if (output == null) return "";
        StringBuilder answer = new StringBuilder();
        for (JsonElement item : output) {
            JsonObject message = item.getAsJsonObject();
            JsonArray content = message.getAsJsonArray("content");
            if (content == null) continue;
            for (JsonElement part : content) {
                JsonObject piece = part.getAsJsonObject();
                if (piece.has("type") && "output_text".equals(piece.get("type").getAsString()) && piece.has("text")) {
                    if (!answer.isEmpty()) answer.append("\n\n");
                    answer.append(piece.get("text").getAsString());
                }
            }
        }
        return answer.toString().trim();
    }
}
