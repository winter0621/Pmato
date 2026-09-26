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
 * @author B（合并时按 5.2 搭建，F3 归属 C）
 * @since V1.0
 */
public class TodoDao {

    /** 查询全部待办（按 id 升序） */
    public synchronized List<TodoItem> findAll() {
        List<TodoItem> list = new ArrayList<>();
        String sql = "SELECT id, title, note, priority, due_date, est_pomodoro, done_pomodoro, status, created_at, completed_at "
                + "FROM todo_item ORDER BY id";
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
        String sql = "INSERT INTO todo_item (title, note, priority, due_date, est_pomodoro, done_pomodoro, status, created_at) "
                + "VALUES (?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = DBManager.getInstance().getConnection().prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setString(2, null);
            ps.setInt(3, priority);
            ps.setString(4, null);
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
    public synchronized void update(long id, String title, int priority, int estPomodoro, boolean done) {
        String sql = "UPDATE todo_item SET title = ?, priority = ?, est_pomodoro = ?, status = ? WHERE id = ?";
        try (PreparedStatement ps = DBManager.getInstance().getConnection().prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setInt(2, priority);
            ps.setInt(3, estPomodoro);
            ps.setString(4, done ? "done" : "todo");
            ps.setLong(5, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("更新待办失败", e);
        }
    }

    /** 单独更新完成状态 */
    public synchronized void setStatus(long id, String status) {
        String sql = "UPDATE todo_item SET status = ? WHERE id = ?";
        try (PreparedStatement ps = DBManager.getInstance().getConnection().prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setLong(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("更新待办状态失败", e);
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
