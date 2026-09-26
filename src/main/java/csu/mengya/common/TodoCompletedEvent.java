package csu.mengya.common;

/**
 * F3 待办完成事件，供计时和统计模块订阅。
 *
 * @author 郭艾迪
 * @since V1.0
 */
public record TodoCompletedEvent(long todoId, String title) {}
