package csu.mengya.service;

import csu.mengya.common.*;
import csu.mengya.dao.FocusSessionDao;
import csu.mengya.model.FocusSession;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * F1 专注计时内核。后台线程负责持久化和结算；单调时钟反算剩余时间，
 * 因此窗口最小化或刷新延迟不会让计时变慢。
 *
 * @author 郭艾迪
 * @since V1.0
 */
public final class FocusService {
    public record Snapshot(String mode, boolean running, boolean paused, long remainingSeconds,
                           int completedInCycle, Integer todoId, String message) {}

    private static final FocusService INSTANCE = new FocusService();
    private final ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "focus-timer");
        thread.setDaemon(true);
        return thread;
    });
    private final FocusSessionDao sessions = new FocusSessionDao();
    private volatile Consumer<Snapshot> listener;
    private volatile Snapshot latest = new Snapshot("focus", false, false, 25 * 60, 0, null, "选择任务并开始专注");

    // 下列状态只在 focus-timer 线程访问，避免重复结束或双计时线程。
    private FocusSession active;
    private String mode = "focus";
    private boolean paused;
    private long plannedNanos;
    private long elapsedBeforePause;
    private long resumedAt;
    private int completedInCycle;

    private FocusService() {
        worker.scheduleAtFixedRate(this::tickSafely, 0, 200, TimeUnit.MILLISECONDS);
    }

    public static FocusService getInstance() { return INSTANCE; }
    public Snapshot snapshot() { return latest; }
    public void setListener(Consumer<Snapshot> next) {
        listener = next;
        if (next != null) next.accept(latest);
    }
    public void stop() { worker.shutdownNow(); }

    /** 创建运行会话并启动专注；数据库写入在计时线程完成。 */
    public CompletableFuture<Result<String>> start(int minutes, Integer todoId) {
        return submit(() -> {
            if (active != null) return Result.fail("当前计时尚未结束");
            if (minutes < 1 || minutes > 180) return Result.fail("专注时长须为 1–180 分钟");
            mode = "focus";
            openSession(minutes, todoId);
            emit("专注已开始");
            return Result.ok("专注已开始");
        });
    }

    public CompletableFuture<Result<String>> pause() {
        return submit(() -> {
            if (active == null || paused) return Result.fail("当前没有可暂停的计时");
            elapsedBeforePause += System.nanoTime() - resumedAt;
            paused = true;
            emit("已暂停，暂停时间不计入专注");
            return Result.ok("已暂停");
        });
    }

    public CompletableFuture<Result<String>> resume() {
        return submit(() -> {
            if (active == null || !paused) return Result.fail("当前没有暂停的计时");
            resumedAt = System.nanoTime();
            paused = false;
            emit("已继续");
            return Result.ok("已继续");
        });
    }

    /** 放弃专注按已完成整分钟的 0.3 倍结算；放弃休息不结算。 */
    public CompletableFuture<Result<String>> abandon() {
        return submit(() -> {
            if (active == null) return Result.fail("当前没有运行中的计时");
            boolean focus = "focus".equals(mode);
            int actualMin = (int) TimeUnit.NANOSECONDS.toMinutes(elapsedNanos());
            int energy = focus ? EnergyCalculator.forAbortedFocus(actualMin) : 0;
            if (sessions.finish(active.getId(), "aborted", actualMin, energy) && focus) {
                if (energy > 0) GardenService.getInstance().applyFocusEnergy(energy);
                EventBus.publish(new FocusAbortedEvent(active.getId(), actualMin));
            }
            active = null;
            completedInCycle = 0;
            mode = "focus";
            paused = false;
            emit(focus ? "本次专注已放弃，连击已重置" : "休息已结束");
            return Result.ok("已放弃");
        });
    }

    /** 可跳过自动开始的休息，直接准备下一个番茄。 */
    public CompletableFuture<Result<String>> skipBreak() {
        return submit(() -> {
            if (active == null || "focus".equals(mode)) return Result.fail("当前不在休息");
            sessions.finish(active.getId(), "aborted", 0, 0);
            active = null;
            mode = "focus";
            paused = false;
            emit("休息已跳过，可以开始下一次专注");
            return Result.ok("休息已跳过");
        });
    }

    private void openSession(int minutes, Integer todoId) {
        FocusSession session = new FocusSession();
        session.setStartAt(LocalDateTime.now().toString());
        session.setPlannedMin(minutes);
        session.setType(mode);
        session.setStatus("running");
        session.setTodoId("focus".equals(mode) ? todoId : null);
        sessions.insert(session);
        active = session;
        plannedNanos = TimeUnit.MINUTES.toNanos(minutes);
        elapsedBeforePause = 0;
        resumedAt = System.nanoTime();
        paused = false;
    }

    private void tickSafely() {
        try {
            if (active != null && !paused && elapsedNanos() >= plannedNanos) complete();
            else if (active != null) emit(latest.message());
        } catch (RuntimeException ex) {
            emit("计时或保存失败：" + ex.getMessage());
        }
    }

    private void complete() {
        FocusSession finished = active;
        boolean focus = "focus".equals(mode);
        int minutes = finished.getPlannedMin();
        int energy = 0;
        if (focus) {
            // 能量计算已抽到 EnergyCalculator（纯函数，可单元测试）
            energy = EnergyCalculator.forCompletedFocus(minutes, completedInCycle, finished.getTodoId() != null);

        }
        if (!sessions.finish(finished.getId(), "completed", minutes, energy)) return;
        active = null;
        if (focus) {
            completedInCycle++;
            EventBus.publish(new FocusFinishedEvent(finished.getId(), minutes, energy, finished.getTodoId()));
            mode = completedInCycle % 4 == 0 ? "long_break" : "short_break";
            openSession("long_break".equals(mode) ? SettingsService.getInstance().longBreakMinutes()
                    : SettingsService.getInstance().shortBreakMinutes(), null);
            emit("专注完成，获得 " + energy + " EP；已进入休息");
        } else {
            mode = "focus";
            emit("休息结束，可以开始下一次专注");
        }
    }

    private long elapsedNanos() {
        return elapsedBeforePause + (paused ? 0 : System.nanoTime() - resumedAt);
    }

    private void emit(String message) {
        long remaining = active == null ? 0 : Math.max(0,
                (plannedNanos - elapsedNanos() + 999_999_999L) / 1_000_000_000L);
        Snapshot next = new Snapshot(mode, active != null, paused, remaining,
                completedInCycle, active == null ? null : active.getTodoId(), message);
        latest = next;
        Consumer<Snapshot> current = listener;
        if (current != null) current.accept(next);
    }

    private CompletableFuture<Result<String>> submit(java.util.concurrent.Callable<Result<String>> action) {
        CompletableFuture<Result<String>> future = new CompletableFuture<>();
        worker.execute(() -> {
            try { future.complete(action.call()); }
            catch (Exception ex) { future.complete(Result.fail("操作失败：" + ex.getMessage())); }
        });
        return future;
    }
}