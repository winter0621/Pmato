package csu.mengya.model;

/**
 * 日程事件实体（对应表 schedule_event，计划书 5.2）。
 *
 * @author B（合并时按 5.2 搭建，F4 归属 C）
 * @since V1.0
 */
public class ScheduleEvent {

    private long id;
    private String title;
    private String startAt;
    private String endAt;
    /** 重复规则：none | daily | weekly */
    private String repeatRule;
    /** 提前提醒分钟数 */
    private int remindBefore;
    private Integer todoId;
    private String color;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public String getRepeatRule() {
        return repeatRule;
    }

    public void setRepeatRule(String repeatRule) {
        this.repeatRule = repeatRule;
    }

    public int getRemindBefore() {
        return remindBefore;
    }

    public void setRemindBefore(int remindBefore) {
        this.remindBefore = remindBefore;
    }

    public Integer getTodoId() {
        return todoId;
    }

    public void setTodoId(Integer todoId) {
        this.todoId = todoId;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }
}
