package csu.mengya.common;

/**
 * 专注完成事件（F1 计时模块发布，F2 植物园 / F5 统计订阅）。
 *
 * <p>依据计划书 5.3 事件清单，携带会话 ID、实际专注分钟、本次结算能量、关联待办。</p>
 *
 * @author B（临时搭建，组长 A 定稿后接管）
 * @since V1.0
 */
public class FocusFinishedEvent {

    private final int sessionId;
    private final int actualMin;
    private final int energyGained;
    private final Integer todoId;

    public FocusFinishedEvent(int sessionId, int actualMin, int energyGained, Integer todoId) {
        this.sessionId = sessionId;
        this.actualMin = actualMin;
        this.energyGained = energyGained;
        this.todoId = todoId;
    }

    public int getSessionId() {
        return sessionId;
    }

    public int getActualMin() {
        return actualMin;
    }

    public int getEnergyGained() {
        return energyGained;
    }

    public Integer getTodoId() {
        return todoId;
    }
}
