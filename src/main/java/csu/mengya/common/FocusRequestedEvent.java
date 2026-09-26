package csu.mengya.common;

/**
 * F3/F4 请求切换到专注页，并预选关联待办。
 *
 * @author 郭艾迪
 * @since V1.0
 */
public record FocusRequestedEvent(Integer todoId) {}