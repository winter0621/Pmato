package csu.mengya.dao;

import csu.mengya.db.DBManager;
import csu.mengya.model.TodoItem;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 待办事项数据访问（对应表 todo_item，计划书 5.2）。
 *
 * <p>职责：单表增删改查，只返回 {@link csu.mengya.model.TodoItem} 实体，
 * 不向调用方暴露 ResultSet。异常统一包成 RuntimeException 上抛，
 * 由 Service 层捕获后转成 Result（计划书 5.3 分层约定）。</p>
 *
 * <p>并发：所有方法用 synchronized 保证串行，避免 SQLite 单连接被多线程争用。</p>
 *
 * @author 侯卓轩
 * @since V1.0
 */
public class TodoDao {

    /** 查询全部待办（按 id 升序） */
    public synchronized List<TodoItem> findAll() {
        List<TodoItem> list = new ArrayList<>();
        String sql = "SELECT id, title, note, priority, due_date, est_pomodoro, done_pomodoro, status, created_at, completed_at "
                + "FROM todo_item ORDER BY CASE WHEN status = 'done' THEN 1 ELSE 0 END, priority DESC, "
                + "CASE WHEN due_date IS NULL OR due_date = '' THEN 1 ELSE 0 END, due_date, id";
        try (Statement st = DBManager.getInstance().getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询待办失败", e);
        }
        return list;
    }

    /** 新增待办（默认 note/due_date 为空，status=todo） */
    public synchronized void insert(String title, int priority, int estPomodoro) {
        insert(title, priority, null, estPomodoro);
    }

    /** 新增带截止日期的待办。 */
    public synchronized void insert(String title, int priority, String dueDate, int estPomodoro) {
        String sql = "INSERT INTO todo_item (title, note, priority, due_date, est_pomodoro, done_pomodoro, status, created_at) "
                + "VALUES (?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = DBManager.getInstance().getConnection().prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setString(2, null);
            ps.setInt(3, priority);
            ps.setString(4, dueDate);
            ps.setInt(5, estPomodoro);
            ps.setInt(6, 0);
            ps.setString(7, "todo");
            ps.setString(8, LocalDateTime.now().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("新增待办失败", e);
        }
    }

    /** 更新待办（标题/优先级/预计番茄数/完成状态） */
    public synchronized void update(long id, String title, int priority, String dueDate, int estPomodoro, boolean done) {
        String sql = "UPDATE todo_item SET title = ?, priority = ?, due_date = ?, est_pomodoro = ?, status = ?, completed_at = ? WHERE id = ?";
        try (PreparedStatement ps = DBManager.getInstance().getConnection().prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setInt(2, priority);
            ps.setString(3, dueDate);
            ps.setInt(4, estPomodoro);
            ps.setString(5, done ? "done" : "todo");
            ps.setString(6, done ? LocalDateTime.now().toString() : null);
            ps.setLong(7, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("更新待办失败", e);
        }
    }

    /** 单独更新完成状态 */
    public synchronized void setStatus(long id, String status) {
        String sql = "UPDATE todo_item SET status = ?, completed_at = ? WHERE id = ?";
        try (PreparedStatement ps = DBManager.getInstance().getConnection().prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, "done".equals(status) ? LocalDateTime.now().toString() : null);
            ps.setLong(3, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("更新待办状态失败", e);
        }
    }

    /** 完成一个番茄后递增任务进度，达到预计数时自动完成任务。 */
    public synchronized void incrementDonePomodoro(long id) {
        String sql = "UPDATE todo_item SET done_pomodoro = MIN(done_pomodoro + 1, est_pomodoro), "
                + "status = CASE WHEN done_pomodoro + 1 >= est_pomodoro THEN 'done' ELSE 'doing' END, "
                + "completed_at = CASE WHEN done_pomodoro + 1 >= est_pomodoro THEN ? ELSE completed_at END "
                + "WHERE id = ? AND status != 'done'";
        try (PreparedStatement ps = DBManager.getInstance().getConnection().prepareStatement(sql)) {
            ps.setString(1, LocalDateTime.now().toString());
            ps.setLong(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("更新待办番茄进度失败", e);
        }
    }

    /** 删除待办 */
    public synchronized void delete(long id) {
        String sql = "DELETE FROM todo_item WHERE id = ?";
        try (PreparedStatement ps = DBManager.getInstance().getConnection().prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("删除待办失败", e);
        }
    }

    /** 结果集 -> 实体 */
    private TodoItem map(ResultSet rs) throws SQLException {
        TodoItem t = new TodoItem();
        t.setId(rs.getLong("id"));
        t.setTitle(rs.getString("title"));
        t.setNote(rs.getString("note"));
        t.setPriority(rs.getInt("priority"));
        t.setDueDate(rs.getString("due_date"));
        t.setEstPomodoro(rs.getInt("est_pomodoro"));
        t.setDonePomodoro(rs.getInt("done_pomodoro"));
        t.setStatus(rs.getString("status"));
        t.setCreatedAt(rs.getString("created_at"));
        t.setCompletedAt(rs.getString("completed_at"));
        return t;
    }
}
