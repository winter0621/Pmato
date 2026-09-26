package csu.mengya.dao;

import csu.mengya.db.DBManager;
import csu.mengya.model.ScheduleEvent;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * 日程事件数据访问（对应表 schedule_event，计划书 5.2）。
 *
 * <p>职责：单表增删改查。注意 {@code insert} 有两个重载 ——
 * 简版供早期代码使用，全参版支持重复规则、提前提醒与待办关联，
 * 新增日程应使用全参版。</p>
 *
 * <p>时间字段统一以 {@code yyyy-MM-dd HH:mm} 字符串存储，与
 * {@code ScheduleController.DB_TIME} 保持一致。</p>
 *
 * @author 侯卓轩
 * @since V1.0
 */
public class ScheduleDao {

    /** 查询全部日程（按开始时间升序） */
    public synchronized List<ScheduleEvent> findAll() {
        List<ScheduleEvent> list = new ArrayList<>();
        String sql = "SELECT id, title, start_at, end_at, repeat_rule, remind_before, todo_id, color "
                + "FROM schedule_event ORDER BY start_at";
        try (Statement st = DBManager.getInstance().getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询日程失败", e);
        }
        return list;
    }

    /** 新增日程（repeat_rule 默认 none，remind_before 默认 5） */
    public synchronized void insert(String title, String startAt, String endAt, String color) {
        insert(title, startAt, endAt, "none", 5, null, color);
    }

    /** 新增包含重复、提醒和待办关联的日程。 */
    public synchronized void insert(String title, String startAt, String endAt, String repeatRule,
                                    int remindBefore, Integer todoId, String color) {
        String sql = "INSERT INTO schedule_event (title, start_at, end_at, repeat_rule, remind_before, todo_id, color) "
                + "VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement ps = DBManager.getInstance().getConnection().prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setString(2, startAt);
            ps.setString(3, endAt);
            ps.setString(4, repeatRule);
            ps.setInt(5, remindBefore);
            if (todoId == null) ps.setNull(6, Types.INTEGER); else ps.setInt(6, todoId);
            ps.setString(7, color);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("新增日程失败", e);
        }
    }

    /** 删除日程 */
    public synchronized void delete(long id) {
        String sql = "DELETE FROM schedule_event WHERE id = ?";
        try (PreparedStatement ps = DBManager.getInstance().getConnection().prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("删除日程失败", e);
        }
    }

    /** 结果集 -> 实体 */
    private ScheduleEvent map(ResultSet rs) throws SQLException {
        ScheduleEvent e = new ScheduleEvent();
        e.setId(rs.getLong("id"));
        e.setTitle(rs.getString("title"));
        e.setStartAt(rs.getString("start_at"));
        e.setEndAt(rs.getString("end_at"));
        e.setRepeatRule(rs.getString("repeat_rule"));
        e.setRemindBefore(rs.getInt("remind_before"));
        int todoId = rs.getInt("todo_id");
        e.setTodoId(rs.wasNull() ? null : todoId);
        e.setColor(rs.getString("color"));
        return e;
    }
}
