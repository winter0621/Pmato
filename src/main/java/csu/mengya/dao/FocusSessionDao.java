package csu.mengya.dao;

import csu.mengya.db.DBManager;
import csu.mengya.model.FocusSession;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * 专注会话数据访问（对应表 focus_session，F5 统计的数据源）。
 *
 * @author B
 * @since V1.0
 */
public class FocusSessionDao {

    /** 查询全部会话（按开始时间升序） */
    public synchronized List<FocusSession> findAll() {
        List<FocusSession> list = new ArrayList<>();
        String sql = "SELECT id, start_at, end_at, planned_min, actual_min, type, status, todo_id, energy_gained "
                + "FROM focus_session ORDER BY start_at";
        try (Statement st = DBManager.getInstance().getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询会话失败", e);
        }
        return list;
    }

    /** 查询已完成且为专注类型的会话（统计只算有效专注，不算休息） */
    public synchronized List<FocusSession> findCompletedFocus() {
        List<FocusSession> list = new ArrayList<>();
        String sql = "SELECT id, start_at, end_at, planned_min, actual_min, type, status, todo_id, energy_gained "
                + "FROM focus_session WHERE status = 'completed' AND type = 'focus' ORDER BY start_at";
        try (PreparedStatement ps = DBManager.getInstance().getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询会话失败", e);
        }
        return list;
    }

    /** 插入会话并回填自增 ID */
    public synchronized void insert(FocusSession s) {
        String sql = "INSERT INTO focus_session (start_at, end_at, planned_min, actual_min, type, status, todo_id, energy_gained) "
                + "VALUES (?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = DBManager.getInstance().getConnection()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, s.getStartAt());
            ps.setString(2, s.getEndAt());
            ps.setInt(3, s.getPlannedMin());
            ps.setInt(4, s.getActualMin());
            ps.setString(5, s.getType());
            ps.setString(6, s.getStatus());
            if (s.getTodoId() == null) {
                ps.setNull(7, java.sql.Types.INTEGER);
            } else {
                ps.setInt(7, s.getTodoId());
            }
            ps.setInt(8, s.getEnergyGained());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    s.setId(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("插入会话失败", e);
        }
    }

    /** 结果集 -> 实体对象 */
    private FocusSession map(ResultSet rs) throws SQLException {
        FocusSession s = new FocusSession();
        s.setId(rs.getInt("id"));
        s.setStartAt(rs.getString("start_at"));
        s.setEndAt(rs.getString("end_at"));
        s.setPlannedMin(rs.getInt("planned_min"));
        s.setActualMin(rs.getInt("actual_min"));
        s.setType(rs.getString("type"));
        s.setStatus(rs.getString("status"));
        int todoId = rs.getInt("todo_id");
        s.setTodoId(rs.wasNull() ? null : todoId);
        s.setEnergyGained(rs.getInt("energy_gained"));
        return s;
    }
}
