package csu.mengya.model;

/**
 * 专注会话实体（对应表 focus_session）。
 *
 * <p>统计模块的数据源。纯数据类，只含字段与 getter/setter。</p>
 *
 * @author 唐天乐
 * @since V1.0
 */
public class FocusSession {

    /** 主键 */
    private int id;

    /** 开始时间（ISO8601 本地时间） */
    private String startAt;

    /** 结束时间 */
    private String endAt;

    /** 计划时长（分钟） */
    private int plannedMin;

    /** 实际专注时长（分钟） */
    private int actualMin;

    /** 类型：focus | short_break | long_break */
    private String type;

    /** 状态：running | completed | aborted */
    private String status;

    /** 关联待办 ID，可空 */
    private Integer todoId;

    /** 本次结算能量 */
    private int energyGained;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getStartAt() {
        return startAt;
    }

    public void setStartAt(String startAt) {
        this.startAt = startAt;
    }

    public String getEndAt() {
        return endAt;
    }

    public void setEndAt(String endAt) {
        this.endAt = endAt;
    }

    public int getPlannedMin() {
        return plannedMin;
    }

    public void setPlannedMin(int plannedMin) {
        this.plannedMin = plannedMin;
    }

    public int getActualMin() {
        return actualMin;
    }

    public void setActualMin(int actualMin) {
        this.actualMin = actualMin;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getTodoId() {
        return todoId;
    }

    public void setTodoId(Integer todoId) {
        this.todoId = todoId;
    }

    public int getEnergyGained() {
        return energyGained;
    }

    public void setEnergyGained(int energyGained) {
        this.energyGained = energyGained;
    }
}
