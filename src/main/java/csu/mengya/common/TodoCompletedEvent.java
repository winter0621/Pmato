package csu.mengya.common;

/** F3 待办完成事件，供计时和统计模块订阅。 */
public record TodoCompletedEvent(long todoId, String title) {}
