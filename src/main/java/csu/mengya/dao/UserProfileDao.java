package csu.mengya.dao;

import csu.mengya.db.DBManager;
import csu.mengya.model.UserProfile;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

/**
 * 用户档案数据访问（对应表 user_profile）。
 *
 * <p>单用户程序，档案固定一行（id=1），首次访问时自动创建。</p>
 *
 * @author 唐天乐
 * @since V1.0
 */
public class UserProfileDao {

    /** 获取档案，不存在则创建（幂等） */
    public synchronized UserProfile getOrCreate() {
        UserProfile p = find();
        if (p != null) {
            return p;
        }
        return create();
    }

    /** 更新档案 */
    public synchronized void update(UserProfile p) {
        String sql = "UPDATE user_profile SET nickname = ?, total_energy = ?, streak_days = ? WHERE id = 1";
        try (PreparedStatement ps = DBManager.getInstance().getConnection().prepareStatement(sql)) {
            ps.setString(1, p.getNickname());
            ps.setInt(2, p.getTotalEnergy());
            ps.setInt(3, p.getStreakDays());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("更新用户档案失败", e);
        }
    }

    private UserProfile find() {
        String sql = "SELECT id, nickname, total_energy, streak_days, created_at FROM user_profile WHERE id = 1";
        try (PreparedStatement ps = DBManager.getInstance().getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                UserProfile p = new UserProfile();
                p.setId(rs.getInt("id"));
                p.setNickname(rs.getString("nickname"));
                p.setTotalEnergy(rs.getInt("total_energy"));
                p.setStreakDays(rs.getInt("streak_days"));
                p.setCreatedAt(rs.getString("created_at"));
                return p;
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询用户档案失败", e);
        }
        return null;
    }

    private UserProfile create() {
        UserProfile p = new UserProfile();
        p.setId(1);
        p.setNickname("学习者");
        p.setTotalEnergy(0);
        p.setStreakDays(0);
        p.setCreatedAt(LocalDateTime.now().toString());
        String sql = "INSERT INTO user_profile (id, nickname, total_energy, streak_days, created_at) VALUES (1,?,?,?,?)";
        try (PreparedStatement ps = DBManager.getInstance().getConnection().prepareStatement(sql)) {
            ps.setString(1, p.getNickname());
            ps.setInt(2, p.getTotalEnergy());
            ps.setInt(3, p.getStreakDays());
            ps.setString(4, p.getCreatedAt());
            ps.executeUpdate();
            return p;
        } catch (SQLException e) {
            throw new RuntimeException("创建用户档案失败", e);
        }
    }
}
