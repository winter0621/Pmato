package csu.mengya.dao;

import csu.mengya.db.DBManager;
import csu.mengya.model.PlantSpecies;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * 作物图鉴数据访问（对应表 plant_species，F2 植物园）。
 *
 * <p>单表增删改查，只返回 model 对象，不返回 ResultSet。</p>
 *
 * @author 唐天乐
 * @since V1.0
 */
public class PlantSpeciesDao {

    /**
     * 查询全部作物（按解锁能量升序，方便图鉴按门槛排列）。
     *
     * @return 作物列表
     */
    public synchronized List<PlantSpecies> findAll() {
        List<PlantSpecies> list = new ArrayList<>();
        String sql = "SELECT id, name, rarity, unlock_energy, stage_scale, collected, unlocked "
                + "FROM plant_species ORDER BY unlock_energy, id";
        try (Statement st = DBManager.getInstance().getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询作物图鉴失败", e);
        }
        return list;
    }

    /**
     * 按 ID 查询单个作物。
     *
     * @param id 物种 ID
     * @return 作物，不存在则返回 null
     */
    public synchronized PlantSpecies findById(String id) {
        String sql = "SELECT id, name, rarity, unlock_energy, stage_scale, collected, unlocked "
                + "FROM plant_species WHERE id = ?";
        try (PreparedStatement ps = DBManager.getInstance().getConnection().prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询作物失败", e);
        }
        return null;
    }

    /** 标记某物种为「已收集」（收获后调用） */
    public synchronized void markCollected(String id) {
        String sql = "UPDATE plant_species SET collected = 1 WHERE id = ?";
        try (PreparedStatement ps = DBManager.getInstance().getConnection().prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("标记作物已收集失败", e);
        }
    }

    /** 标记某物种为「已解锁」（花能量解锁后调用） */
    public synchronized void markUnlocked(String id) {
        String sql = "UPDATE plant_species SET unlocked = 1 WHERE id = ?";
        try (PreparedStatement ps = DBManager.getInstance().getConnection().prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("标记作物已解锁失败", e);
        }
    }

    /**
     * 首次启动时填充默认作物图鉴（表空才填充，幂等）。
     * 依据计划书 D4：至少 8 种作物，含稀有度。
     */
    public synchronized void seedIfEmpty() {
        if (!findAll().isEmpty()) {
            return;
        }
        // 普通作物（起始即解锁），稀有/史诗作物需累计能量解锁
        insert(species("sunflower", "向日葵", "common", 0, 1.0));
        insert(species("tomato", "番茄", "common", 0, 1.0));
        insert(species("cactus", "仙人掌", "common", 0, 1.0));
        insert(species("tulip", "郁金香", "common", 0, 1.0));
        insert(species("rose", "玫瑰", "common", 0, 1.0));
        insert(species("lily", "百合", "rare", 300, 1.5));
        insert(species("orchid", "兰花", "rare", 600, 1.5));
        insert(species("sakura", "樱花", "epic", 1000, 2.0));
    }

    /** 构建作物对象的便捷方法 */
    private PlantSpecies species(String id, String name, String rarity, int unlockEnergy, double scale) {
        PlantSpecies s = new PlantSpecies();
        s.setId(id);
        s.setName(name);
        s.setRarity(rarity);
        s.setUnlockEnergy(unlockEnergy);
        s.setStageScale(scale);
        s.setUnlocked(unlockEnergy == 0);   // 门槛为 0 的普通作物默认已解锁
        return s;
    }

    /** 插入单个作物 */
    private void insert(PlantSpecies s) {
        String sql = "INSERT INTO plant_species (id, name, rarity, unlock_energy, stage_scale, collected, unlocked) VALUES (?,?,?,?,?,0,?)";
        try (PreparedStatement ps = DBManager.getInstance().getConnection().prepareStatement(sql)) {
            ps.setString(1, s.getId());
            ps.setString(2, s.getName());
            ps.setString(3, s.getRarity());
            ps.setInt(4, s.getUnlockEnergy());
            ps.setDouble(5, s.getStageScale());
            ps.setInt(6, s.isUnlocked() ? 1 : 0);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("插入作物失败", e);
        }
    }

    /** 结果集 -> 实体对象 */
    private PlantSpecies map(ResultSet rs) throws SQLException {
        PlantSpecies s = new PlantSpecies();
        s.setId(rs.getString("id"));
        s.setName(rs.getString("name"));
        s.setRarity(rs.getString("rarity"));
        s.setUnlockEnergy(rs.getInt("unlock_energy"));
        s.setStageScale(rs.getDouble("stage_scale"));
        s.setCollected(rs.getInt("collected") != 0);
        s.setUnlocked(rs.getInt("unlocked") != 0);
        return s;
    }
}
