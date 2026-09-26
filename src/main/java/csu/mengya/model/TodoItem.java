package csu.mengya.model;

/**
 * 待办事项实体（对应表 todo_item，计划书 5.2）。
 *
 * <p>纯数据载体：只有字段与 getter / setter，不含业务逻辑。
 * {@code status} 取值 todo / doing / done；{@code priority} 取值 0 低 / 1 中 / 2 高。</p>
 *
 * @author 侯卓轩
 * @since V1.0
 */
public class TodoItem {

    private long id;
    private String title;
    private String note;
    /** 优先级：0 低 / 1 中 / 2 高 */
    private int priority;
    private String dueDate;
    private int estPomodoro;
    private int donePomodoro;
    /** 状态：todo | doing | done */
    private String status;
    private String createdAt;
    private String completedAt;

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

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public int getEstPomodoro() {
        return estPomodoro;
    }

    public void setEstPomodoro(int estPomodoro) {
        this.estPomodoro = estPomodoro;
    }

    public int getDonePomodoro() {
        return donePomodoro;
    }

    public void setDonePomodoro(int donePomodoro) {
        this.donePomodoro = donePomodoro;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(String completedAt) {
        this.completedAt = completedAt;
    }
}
