package csu.mengya.controller;

import csu.mengya.common.Result;
import csu.mengya.service.AiAssistantService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;

/** 学习助手页面：只在用户点击生成时发起网络请求，结果回到 JavaFX 线程展示。 */
public class AssistantController {
    @FXML private PasswordField apiKeyField;
    @FXML private CheckBox includeContextBox;
    @FXML private TextArea questionArea, answerArea;
    @FXML private Button breakdownButton, planButton, askButton;
    @FXML private Label statusLabel;

    private final AiAssistantService assistant = new AiAssistantService();
    private boolean running;

    @FXML private void breakDownTasks() {
        submit("拆解未完成待办：按紧急程度和预计番茄数排序，给出今天可执行的前 3 步。");
    }

    @FXML private void planWeek() {
        submit("结合待办和日程，给出未来七天现实可行的学习安排，并保留休息时间。");
    }

    @FXML private void askQuestion() {
        if (questionArea.getText().isBlank()) {
            statusLabel.setText("请先输入你的问题。");
            return;
        }
        submit("回答用户的学习问题，并给出可执行建议。");
    }

    private void submit(String mode) {
        if (running) return;
        String key = apiKeyField.getText();
        String question = questionArea.getText();
        boolean includeContext = includeContextBox.isSelected();
        running = true;
        setButtonsDisabled(true);
        statusLabel.setText("正在生成建议…");
        answerArea.setText("");

        Task<Result<String>> task = new Task<>() {
            @Override protected Result<String> call() {
                return assistant.generate(key, mode, question, includeContext);
            }
        };
        task.setOnSucceeded(event -> {
            Result<String> result = task.getValue();
            if (result.isSuccess()) {
                answerArea.setText(result.getData());
                statusLabel.setText("建议已生成 · 请核对后采纳");
            } else {
                statusLabel.setText(result.getMessage());
            }
            running = false;
            setButtonsDisabled(false);
        });
        task.setOnFailed(event -> {
            statusLabel.setText("生成失败，请稍后重试。");
            running = false;
            setButtonsDisabled(false);
        });
        Thread worker = new Thread(task, "ai-assistant-request");
        worker.setDaemon(true);
        worker.start();
    }

    private void setButtonsDisabled(boolean disabled) {
        breakdownButton.setDisable(disabled);
        planButton.setDisable(disabled);
        askButton.setDisable(disabled);
    }
}
