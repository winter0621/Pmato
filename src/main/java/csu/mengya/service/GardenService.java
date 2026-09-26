package csu.mengya.service;

import csu.mengya.common.EventBus;
import csu.mengya.common.GameConstants;
import csu.mengya.common.PlantStageUpEvent;
import csu.mengya.common.Result;
import csu.mengya.dao.GardenPlotDao;
import csu.mengya.dao.PlantSpeciesDao;
import csu.mengya.dao.UserProfileDao;
import csu.mengya.model.GardenPlot;
import csu.mengya.model.PlantSpecies;
import csu.mengya.model.UserProfile;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 植物园业务逻辑（F2 植物园）。
 *
 * <p>负责种植、能量浇灌、生长判定、收获与解锁。只依赖 DAO，不 import 任何
 * javafx 包（遵循计划书 5.3 分层约定）。</p>
 *
 * @author 唐天乐
 * @since V1.0
 */
public final class GardenService {

    private static final GardenService INSTANCE = new GardenService();

    private final GardenPlotDao plotDao = new GardenPlotDao();
    private final PlantSpeciesDao speciesDao = new PlantSpeciesDao();
    private final UserProfileDao profileDao = new UserProfileDao();

    private GardenService() {
    }

    public static GardenService getInstance() {
        return INSTANCE;
    }

    /** 全部地块 */
    public List<GardenPlot> allPlots() {
        return plotDao.findAll();
    }

    /** 全部作物图鉴 */
    public List<PlantSpecies> allSpecies() {
        return speciesDao.findAll();
    }

    /** 累计总能量 */
    public int totalEnergy() {
        return profileDao.getOrCreate().getTotalEnergy();
    }

    /**
     * 判断某物种是否已解锁（门槛为 0 的普通作物默认解锁）。
     *
     * @param species 物种
     * @return 是否已解锁
     */
    public boolean isUnlocked(PlantSpecies species) {
        return species.getUnlockEnergy() == 0 || species.isUnlocked();
    }

    /**
     * 花能量解锁某物种（永久解锁）。
     *
     * @param speciesId 物种 ID
     * @return 成功返回物种；已解锁或能量不足返回失败
     */
    public Result<PlantSpecies> unlockSpecies(String speciesId) {
        PlantSpecies species = speciesDao.findById(speciesId);
        if (species == null) {
            return Result.fail("未知作物");
        }
        if (isUnlocked(species)) {
            return Result.fail("该作物已解锁");
        }
        if (totalEnergy() < species.getUnlockEnergy()) {
            return Result.fail("能量不足，解锁需要 " + species.getUnlockEnergy() + " 能量");
        }
        spendEnergy(species.getUnlockEnergy());
        speciesDao.markUnlocked(speciesId);
        return Result.ok(species);
    }

    /**
     * 种植作物到指定地块（需已解锁 + 扣除种植消耗）。
     *
     * @param speciesId 物种 ID
     * @param slotIndex 地块位置
     * @return 成功返回地块；失败返回原因
     */
    public Result<GardenPlot> plant(String speciesId, int slotIndex) {
        PlantSpecies species = speciesDao.findById(speciesId);
        if (species == null) {
            return Result.fail("未知作物");
        }
        if (!isUnlocked(species)) {
            return Result.fail("「" + species.getName() + "」还未解锁，需 " + species.getUnlockEnergy() + " 能量");
        }
        int cost = GameConstants.plantCost(species.getRarity());
        if (totalEnergy() < cost) {
            return Result.fail("能量不足，种植需要 " + cost + " 能量");
        }
        if (slotIndex < 0 || slotIndex >= GameConstants.PLOT_COUNT) {
            return Result.fail("无效的地块位置");
        }
        if (plotDao.findBySlot(slotIndex) != null) {
            return Result.fail("该地块已种了作物，不能覆盖");
        }
        spendEnergy(cost);
        GardenPlot plot = new GardenPlot();
        plot.setSpeciesId(speciesId);
        plot.setSlotIndex(slotIndex);
        plot.setPlantedAt(LocalDateTime.now().toString());
        plot.setEnergy(0);
        plot.setStage(0);
        plot.setStatus("growing");
        plotDao.insert(plot);
        return Result.ok(plot);
    }

    /** 第一个空地块的下标，无则返回 -1 */
    public int firstEmptySlot() {
        for (int i = 0; i < GameConstants.PLOT_COUNT; i++) {
            if (plotDao.findBySlot(i) == null) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 应用一次专注获得的能量：累加用户总能量，并浇灌第一个生长中的地块。
     * 若作物阶段升级，发布 PlantStageUpEvent。
     *
     * @param energy 本次结算能量
     * @return 被浇灌的地块；若无生长中的作物则返回 ok(null)
     */
    public Result<GardenPlot> applyFocusEnergy(int energy) {
        // 1. 累加用户总能量（用于解锁新物种）
        addTotalEnergy(energy);

        // 2. 浇灌第一个生长中的地块
        GardenPlot plot = firstGrowingPlot();
        if (plot == null) {
            return Result.ok(null);
        }
        int newEnergy = plot.getEnergy() + energy;
        plot.setEnergy(newEnergy);

        // 3. 反算生长阶段（阈值按稀有度系数放大）
        int newStage = calcStage(newEnergy, speciesScale(plot.getSpeciesId()));
        if (newStage > plot.getStage()) {
            plot.setStage(newStage);
            if (newStage >= GameConstants.STAGE_THRESHOLDS.length - 1) {
                plot.setStatus("mature");
            }
            plotDao.update(plot);
            EventBus.publish(new PlantStageUpEvent(plot.getId(), plot.getSpeciesId(), newStage));
        } else {
            plotDao.update(plot);
        }
        return Result.ok(plot);
    }

    /** 开发用：浇灌到下一生长阶段，便于快速查看五阶段生长过程 */
    public Result<GardenPlot> growToNextStage() {
        GardenPlot plot = firstGrowingPlot();
        if (plot == null) {
            return Result.fail("没有生长中的作物，先种植一株吧");
        }
        double scale = speciesScale(plot.getSpeciesId());
        int stage = calcStage(plot.getEnergy(), scale);
        if (stage >= GameConstants.STAGE_THRESHOLDS.length - 1) {
            return Result.fail("已成熟，可以收获了");
        }
        // 计算到达下一阶段还差多少能量（阈值按稀有度系数放大）
        int nextThreshold = (int) Math.ceil(GameConstants.STAGE_THRESHOLDS[stage + 1] * scale);
        int need = Math.max(1, nextThreshold - plot.getEnergy());
        return applyFocusEnergy(need);
    }

    /** 收获第一个成熟作物：标记物种已收集，并删除地块释放槽位供重新种植 */
    public Result<GardenPlot> harvestFirstMature() {
        for (GardenPlot p : plotDao.findAll()) {
            if ("mature".equals(p.getStatus())) {
                speciesDao.markCollected(p.getSpeciesId());   // 永久记录「已收集」
                plotDao.delete(p.getId());                    // 释放槽位
                return Result.ok(p);
            }
        }
        return Result.fail("没有可收获的成熟作物");
    }

    /** 已收集（收获过）的物种 ID 集合（从图鉴表读取，重种不影响） */
    public Set<String> collectedSpeciesIds() {
        Set<String> set = new HashSet<>();
        for (PlantSpecies s : speciesDao.findAll()) {
            if (s.isCollected()) {
                set.add(s.getId());
            }
        }
        return set;
    }

    /** 开发用：清空植物园，便于重复测试种植流程 */
    public void resetAll() {
        plotDao.deleteAll();
    }

    /**
     * 根据累计能量与稀有度系数计算生长阶段（0-4）。
     *
     * @param energy 累计能量
     * @param scale  稀有度系数（1.0 / 1.5 / 2.0）
     * @return 阶段号
     */
    public int calcStage(int energy, double scale) {
        for (int i = GameConstants.STAGE_THRESHOLDS.length - 1; i >= 0; i--) {
            if (energy >= GameConstants.STAGE_THRESHOLDS[i] * scale) {
                return i;
            }
        }
        return 0;
    }

    /**
     * 计算作物在当前阶段内的成长进度（0.0 ~ 1.0），用于界面进度条展示。
     * 成熟阶段（已达最高阈值）返回 1.0。
     *
     * @param plot 地块
     * @return 当前阶段内的进度比例
     */
    public double stageProgress(GardenPlot plot) {
        double scale = speciesScale(plot.getSpeciesId());
        int stage = calcStage(plot.getEnergy(), scale);
        int last = GameConstants.STAGE_THRESHOLDS.length - 1;
        if (stage >= last) {
            return 1.0;
        }
        double current = GameConstants.STAGE_THRESHOLDS[stage] * scale;
        double next = GameConstants.STAGE_THRESHOLDS[stage + 1] * scale;
        if (next <= current) {
            return 1.0;
        }
        double progress = (plot.getEnergy() - current) / (next - current);
        return Math.max(0.0, Math.min(1.0, progress));
    }

    /** 第一个生长中的地块，无则 null */
    private GardenPlot firstGrowingPlot() {
        for (GardenPlot p : plotDao.findAll()) {
            if ("growing".equals(p.getStatus())) {
                return p;
            }
        }
        return null;
    }

    /** 物种稀有度系数 */
    private double speciesScale(String speciesId) {
        PlantSpecies s = speciesDao.findById(speciesId);
        return s != null ? s.getStageScale() : GameConstants.SCALE_COMMON;
    }

    /** 累加用户总能量 */
    private void addTotalEnergy(int energy) {
        UserProfileDao dao = new UserProfileDao();
        UserProfile profile = dao.getOrCreate();
        profile.setTotalEnergy(profile.getTotalEnergy() + energy);
        dao.update(profile);
    }

    /** 扣除用户能量（种植/解锁消耗） */
    private void spendEnergy(int amount) {
        UserProfileDao dao = new UserProfileDao();
        UserProfile profile = dao.getOrCreate();
        profile.setTotalEnergy(profile.getTotalEnergy() - amount);
        dao.update(profile);
    }
}
