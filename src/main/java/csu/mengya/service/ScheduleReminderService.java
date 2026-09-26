package csu.mengya.service;

import csu.mengya.common.EventBus;
import csu.mengya.common.ScheduleRemindEvent;
import csu.mengya.dao.ScheduleDao;
import csu.mengya.model.ScheduleEvent;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 日程提醒调度服务（F4）。
 *
 * <p>独立于界面运行：即使切换到其他页面，也继续按固定周期检查日程是否到点。</p>
 *
 * <p>去重机制：轮询周期是 20 秒，而提醒窗口通常提前 5 分钟，
 * 同一场日程会在窗口内被检查十几次，因此必须去重。
 * 去重 key 由「日程 id + 本次发生时间」组成 —— 带上发生时间是为了让
 * 每天 / 每周重复的日程，在每一次新的发生时间上都能重新提醒一次。</p>
 *
 * @author 侯卓轩
 * @since V1.0
 */
public final class ScheduleReminderService {

    /** 提醒检查线程；守护线程，进程退出时自动结束 */
    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "schedule-reminder");
        thread.setDaemon(true);
        return thread;
    });

    /** 日程起止时间在数据库中的存储格式，须与 ScheduleController.DB_TIME 保持一致 */
    private static final DateTimeFormatter DB_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * 已提醒记录的集合，key 形如 {@code "12@2026-10-07T09:00"}。
     *
     * <p>只在 {@link #check()} 中访问，而该方法是单线程调度器上唯一的任务，
     * 不存在并发写入，因此用普通 HashSet 即可。</p>
     */
    private static final Set<String> SENT = new HashSet<>();

    private ScheduleReminderService() {
    }

    /** 启动提醒检查：立即执行一次，之后每 20 秒一次 */
    public static void start() {
        EXECUTOR.scheduleAtFixedRate(ScheduleReminderService::check, 0, 20, TimeUnit.SECONDS);
    }

    /** 停止提醒检查 */
    public static void stop() {
        EXECUTOR.shutdownNow();
    }

    /** 检查所有日程是否进入提醒窗口，是则发布提醒事件 */
    private static void check() {
        try {
            LocalDateTime now = LocalDateTime.now();
            for (ScheduleEvent event : new ScheduleDao().findAll()) {
                LocalDateTime start = LocalDateTime.parse(event.getStartAt(), DB_TIME);

                // 重复日程：把起始时间推进到「不早于昨天」的最近一次发生。
                // 不重复的日程（none）保持原样。
                if ("daily".equals(event.getRepeatRule())) {
                    while (start.isBefore(now.minusDays(1))) {
                        start = start.plusDays(1);
                    }
                } else if ("weekly".equals(event.getRepeatRule())) {
                    while (start.isBefore(now.minusDays(1))) {
                        start = start.plusWeeks(1);
                    }
                }

                LocalDateTime remindAt = start.minusMinutes(event.getRemindBefore());
                // 去重 key 带上本次发生时间，保证重复日程每天 / 每周都能重新提醒一次
                String key = event.getId() + "@" + start;
                // 三个条件同时成立才提醒：已进入提醒窗口、未超过开始时间过久、本次发生尚未提醒过
                if (!now.isBefore(remindAt) && now.isBefore(start.plusMinutes(1)) && SENT.add(key)) {
                    EventBus.publish(new ScheduleRemindEvent(
                            event.getId(), event.getTitle(), start.format(DB_TIME), event.getTodoId()));
                }
            }
        } catch (RuntimeException ex) {
            System.err.println("日程提醒检查失败：" + ex.getMessage());
        }
    }
}
