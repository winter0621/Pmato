package csu.mengya.common;

/**
 * F4 到点提醒事件，供专注模块和桌面通知订阅。
 *
 * @author 郭艾迪
 * @since V1.0
 */
public record ScheduleRemindEvent(long eventId, String title, String startAt, Integer todoId) {}
