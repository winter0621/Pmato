package csu.mengya.common;

/**
 * 专注中断事件（F1 计时模块发布，F5 统计订阅）。
 *
 * @author 唐天乐
 * @since V1.0
 */
public class FocusAbortedEvent {

    private final int sessionId;
    private final int actualMin;

    public FocusAbortedEvent(int sessionId, int actualMin) {
        this.sessionId = sessionId;
        this.actualMin = actualMin;
    }

    public int getSessionId() {
        return sessionId;
    }

    public int getActualMin() {
        return actualMin;
    }
}
