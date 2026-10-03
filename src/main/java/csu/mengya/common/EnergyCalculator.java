package csu.mengya.common;

/**
 * 能量结算计算器。
 *
 * <p>对应计划书 5.1 节的换算公式。纯计算，不碰数据库和界面。
 *
 * @author 侯卓轩
 * @since V1.0
 */
public final class EnergyCalculator {

    private EnergyCalculator() {
    }

    /**
     * 计算一次已完成专注获得的能量。
     *
     * @param minutes    实际专注分钟数
     * @param comboIndex 本轮第几个番茄，从 0 开始
     * @param boundTodo  是否绑定了待办任务
     * @return 结算能量；分钟数非法时返回 0
     */
    public static int forCompletedFocus(int minutes, int comboIndex, boolean boundTodo) {
        if (minutes <= 0) {
            return 0;
        }
        // 下标夹到合法区间：COMBO_RATIOS 只有 4 档，第 4 个番茄之后不再增长
        int index = Math.max(0, Math.min(comboIndex, GameConstants.COMBO_RATIOS.length - 1));
        double combo = GameConstants.COMBO_RATIOS[index];
        double task = boundTodo ? GameConstants.TODO_BONUS : 1.0;
        return (int) Math.round(minutes * GameConstants.ENERGY_PER_MINUTE * combo * task);
    }

    /**
     * 计算一次中断（主动放弃）专注获得的能量。
     *
     * <p>按已完成分钟数打三折，且不叠加连击与任务加成 —— 放弃意味着连击已重置。
     *
     * @param elapsedMinutes 实际已专注分钟数
     * @return 结算能量；分钟数非法时返回 0
     */
    public static int forAbortedFocus(int elapsedMinutes) {
        if (elapsedMinutes <= 0) {
            return 0;
        }
        return (int) Math.round(elapsedMinutes * GameConstants.ENERGY_PER_MINUTE * GameConstants.ABORT_RATIO);
    }
}
