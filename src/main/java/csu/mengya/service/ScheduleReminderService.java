package csu.mengya.service;

import csu.mengya.common.EventBus;
import csu.mengya.common.ScheduleRemindEvent;
import csu.mengya.dao.ScheduleDao;
import csu.mengya.model.ScheduleEvent;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** F4 提醒调度：即使切换页面也继续检查日程。 */
public final class ScheduleReminderService {
    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "schedule-reminder");
        thread.setDaemon(true);
        return thread;
    });
    private static final DateTimeFormatter DB_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Set<String> SENT = new HashSet<>();

    private ScheduleReminderService() {}

    public static void start() { EXECUTOR.scheduleAtFixedRate(ScheduleReminderService::check, 0, 20, TimeUnit.SECONDS); }
    public static void stop() { EXECUTOR.shutdownNow(); }

    private static void check() {
        try {
            LocalDateTime now = LocalDateTime.now();
            for (ScheduleEvent event : new ScheduleDao().findAll()) {
                LocalDateTime start = LocalDateTime.parse(event.getStartAt(), DB_TIME);
                if ("daily".equals(event.getRepeatRule())) {
                    while (start.isBefore(now.minusDays(1))) start = start.plusDays(1);
                } else if ("weekly".equals(event.getRepeatRule())) {
                    while (start.isBefore(now.minusDays(1))) start = start.plusWeeks(1);
                }
                LocalDateTime remindAt = start.minusMinutes(event.getRemindBefore());
                String key = event.getId() + "@" + start;
                if (!now.isBefore(remindAt) && now.isBefore(start.plusMinutes(1)) && SENT.add(key)) {
                    EventBus.publish(new ScheduleRemindEvent(event.getId(), event.getTitle(), start.format(DB_TIME), event.getTodoId()));
                }
            }
        } catch (RuntimeException ex) {
            System.err.println("日程提醒检查失败：" + ex.getMessage());
        }
    }
}
