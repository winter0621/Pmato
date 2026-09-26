package csu.mengya.common;

/**
 * 能量结算计算器。
 *
 * <p>对应《项目分工计划书》5.1 节的能量换算公式。本类只做纯计算，
 * 不碰数据库、不碰界面、不依赖任何单例，因此可以被单元测试完整覆盖
 * —— 这是把公式从 {@code FocusService} 里抽出来的原因。</p>
 *
 * <p>公式约定：</p>
 * <pre>
 *   完成专注：EP = 分钟数 × 1.0 × 连击系数 × 任务加成
 *   中断专注：EP = 已完成分钟数 × 1.0 × 0.3（不叠加连击与任务加成）
 *   休息时段：EP = 0
 * </pre>
 *
 * @author 侯卓轩
 * @since V1.0
 */
public final class EnergyCalculator {

    /** 工具类不允许实例化 */
    private EnergyCalculator() {
    }

    /**
     * 计算一次「已完成专注」获得的能量。
     *
     * <p>连击系数按第几个番茄取值：第 1 个 1.0、第 2 个 1.1、第 3 个 1.2、
     * 第 4 个及以上固定 1.3。因为 {@link GameConstants#COMBO_RATIOS} 只有 4 档，
     * 超出部分取最后一档，所以连击有上限、不会无限增长。</p>
     *
     * @param minutes    实际专注分钟数
     * @param comboIndex 这是本轮的第几个番茄，从 0 开始计数
     * @param boundTodo  本次专注是否绑定了待办任务，绑定则额外乘 1.1
     * @return 结算能量（EP），分钟数非正时返回 0
     */
    public static int forCompletedFocus(int minutes, int comboIndex, boolean boundTodo) {
        if (minutes <= 0) {
            return 0;                                    // 防御：异常输入不产生能量
        }
        // 连击下标夹取到合法范围：负数按 0，超出长度按最后一档
        int index = Math.max(0, Math.min(comboIndex, GameConstants.COMBO_RATIOS.length - 1));
        double combo = GameConstants.COMBO_RATIOS[index];
        double task = boundTodo ? GameConstants.TODO_BONUS : 1.0;
        return (int) Math.round(minutes * GameConstants.ENERGY_PER_MINUTE * combo * task);
    }

    /**
     * 计算一次「中断（主动放弃）专注」获得的能量。
     *
     * <p>按已完成分钟数的 0.3 倍结算，且<b>不</b>叠加连击系数与任务加成
     * —— 放弃意味着连击已重置，不应再享受奖励。</p>
     *
     * @param elapsedMinutes 实际已专注的分钟数
     * @return 结算能量（EP），分钟数非正时返回 0
     */
    public static int forAbortedFocus(int elapsedMinutes) {
        if (elapsedMinutes <= 0) {
            return 0;
        }
        return (int) Math.round(elapsedMinutes * GameConstants.ENERGY_PER_MINUTE * GameConstants.ABORT_RATIO);
    }
}
