package csu.mengya.common;

/**
 * 游戏常量（F0 共享地基）。
 *
 * <p>依据计划书 5.1 节，能量换算公式与生长阈值等魔法数字【集中硬编码在此一处】，
 * 业务代码不得散写数字。具体数值待 D2 会议最终确认后以本类为准。</p>
 *
 * @author B（临时搭建，组长 A 定稿后接管）
 * @since V1.0
 */
public final class GameConstants {

    private GameConstants() {
    }

    // ---------- 能量换算（5.1） ----------

    /** 每分钟专注获得的能量 */
    public static final double ENERGY_PER_MINUTE = 1.0;

    /** 中断（主动放弃）会话的能量折算系数 */
    public static final double ABORT_RATIO = 0.3;

    /** 绑定待办任务时的能量加成 */
    public static final double TODO_BONUS = 1.1;

    /** 连续专注加成：下标 = 第 n 个番茄减一（第 1 个 1.0，第 2 个 1.1，第 3 个 1.2，第 4 个起 1.3） */
    public static final double[] COMBO_RATIOS = {1.0, 1.1, 1.2, 1.3};

    // ---------- 生长阶段阈值（累计能量 EP） ----------

    /** 各阶段所需累计能量阈值，下标即阶段号 */
    public static final int[] STAGE_THRESHOLDS = {0, 30, 80, 150, 240};

    /** 各阶段名称 */
    public static final String[] STAGE_NAMES = {"种子", "幼苗", "抽枝", "花苞", "成熟"};

    // ---------- 稀有度系数 ----------

    /** 普通作物 */
    public static final double SCALE_COMMON = 1.0;

    /** 稀有作物 */
    public static final double SCALE_RARE = 1.5;

    /** 史诗作物 */
    public static final double SCALE_EPIC = 2.0;

    // ---------- 植物园 ----------

    /** 地块数量 */
    public static final int PLOT_COUNT = 6;

    // ---------- 种植消耗（按稀有度） ----------

    /** 普通作物种植消耗 */
    public static final int PLANT_COST_COMMON = 10;

    /** 稀有作物种植消耗 */
    public static final int PLANT_COST_RARE = 20;

    /** 史诗作物种植消耗 */
    public static final int PLANT_COST_EPIC = 30;

    /**
     * 根据稀有度取种植消耗。
     *
     * @param rarity common / rare / epic
     * @return 种植所需能量
     */
    public static int plantCost(String rarity) {
        if ("rare".equalsIgnoreCase(rarity)) {
            return PLANT_COST_RARE;
        }
        if ("epic".equalsIgnoreCase(rarity)) {
            return PLANT_COST_EPIC;
        }
        return PLANT_COST_COMMON;
    }

    /**
     * 根据稀有度字符串取生长系数。
     *
     * @param rarity common / rare / epic
     * @return 对应系数，未知稀有度按 1.0 处理
     */
    public static double scaleOf(String rarity) {
        if ("rare".equalsIgnoreCase(rarity)) {
            return SCALE_RARE;
        }
        if ("epic".equalsIgnoreCase(rarity)) {
            return SCALE_EPIC;
        }
        return SCALE_COMMON;
    }
}
