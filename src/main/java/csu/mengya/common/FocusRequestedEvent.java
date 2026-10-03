package csu.mengya.common;

/** F3/F4 请求切换到专注页，并预选关联待办。 */
public record FocusRequestedEvent(Integer todoId) {}