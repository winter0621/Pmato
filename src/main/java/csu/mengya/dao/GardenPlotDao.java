package csu.mengya.dao;

import csu.mengya.db.DBManager;
import csu.mengya.model.GardenPlot;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * 植物园地块数据访问（对应表 garden_plot，F2 植物园）。
 *
 * @author B
 * @since V1.0
 */
public class GardenPlotDao {

    /** 查询全部地块，按位置排序 */
    public synchronized List<GardenPlot> findAll() {
        List<GardenPlot> list = new ArrayList<>();
        String sql = "SELECT id, species_id, slot_index, planted_at, energy, stage, status FROM garden_plot ORDER BY slot_index";
        try (Statement st = DBManager.getInstance().getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询地块失败", e);
        }
        return list;
    }

    /** 按位置查询单个地块，无则返回 null */
    public synchronized GardenPlot findBySlot(int slotIndex) {
        String sql = "SELECT id, species_id, slot_index, planted_at, energy, stage, status FROM garden_plot WHERE slot_index = ?";
        try (PreparedStatement ps = DBManager.getInstance().getConnection().prepareStatement(sql)) {
            ps.setInt(1, slotIndex);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询地块失败", e);
        }
        return null;
    }

    /** 插入地块并回填自增 ID */
    public synchronized void insert(GardenPlot p) {
        String sql = "INSERT INTO garden_plot (species_id, slot_index, planted_at, energy, stage, status) VALUES (?,?,?,?,?,?)";
        try (PreparedStatement ps = DBManager.getInstance().getConnection()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getSpeciesId());
            ps.setInt(2, p.getSlotIndex());
            ps.setString(3, p.getPlantedAt());
            ps.setInt(4, p.getEnergy());
            ps.setInt(5, p.getStage());
            ps.setString(6, p.getStatus());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    p.setId(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("插入地块失败", e);
        }
    }

    /** 更新地块（能量、阶段、状态） */
    public synchronized void update(GardenPlot p) {
        String sql = "UPDATE garden_plot SET energy = ?, stage = ?, status = ? WHERE id = ?";
        try (PreparedStatement ps = DBManager.getInstance().getConnection().prepareStatement(sql)) {
            ps.setInt(1, p.getEnergy());
            ps.setInt(2, p.getStage());
            ps.setString(3, p.getStatus());
            ps.setInt(4, p.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("更新地块失败", e);
        }
    }

    /** 清空全部地块（开发用重置） */
    public synchronized void deleteAll() {
        String sql = "DELETE FROM garden_plot";
        try (Statement st = DBManager.getInstance().getConnection().createStatement()) {
            st.executeUpdate(sql);
        } catch (SQLException e) {
            throw new RuntimeException("清空地块失败", e);
        }
    }

    /** 结果集 -> 实体对象 */
    private GardenPlot map(ResultSet rs) throws SQLException {
        GardenPlot p = new GardenPlot();
        p.setId(rs.getInt("id"));
        p.setSpeciesId(rs.getString("species_id"));
        p.setSlotIndex(rs.getInt("slot_index"));
        p.setPlantedAt(rs.getString("planted_at"));
        p.setEnergy(rs.getInt("energy"));
        p.setStage(rs.getInt("stage"));
        p.setStatus(rs.getString("status"));
        return p;
    }
}
