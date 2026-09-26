package csu.mengya;

import csu.mengya.common.EnergyCalculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 能量结算公式单元测试。
 *
 * <p>逐条验证《项目分工计划书》5.1 节的换算规则。
 * 这些用例同时是用户手册「能量规则」章节的数值依据，
 * 改公式时必须同步改手册，两处不能不一致。</p>
 *
 * @author 侯卓轩
 * @since V1.0
 */
class EnergyCalculatorTest {

    // ---------- 已完成专注 ----------

    @Test
    @DisplayName("第 1 个番茄，无任务绑定：25 分钟 = 25 EP")
    void firstPomodoroWithoutTodo() {
        assertEquals(25, EnergyCalculator.forCompletedFocus(25, 0, false));
    }

    @Test
    @DisplayName("连击系数 1.0 / 1.1 / 1.2 / 1.3 逐档验证")
    void comboRatiosPerPomodoro() {
        assertEquals(25, EnergyCalculator.forCompletedFocus(25, 0, false));  // 1.0
        assertEquals(28, EnergyCalculator.forCompletedFocus(25, 1, false));  // 1.1 → 27.5 → 28
        assertEquals(30, EnergyCalculator.forCompletedFocus(25, 2, false));  // 1.2 → 30.0
        assertEquals(33, EnergyCalculator.forCompletedFocus(25, 3, false));  // 1.3 → 32.5 → 33
    }

    @Test
    @DisplayName("连击封顶：第 5、第 6 个番茄仍按 1.3，不再增长")
    void comboIsCapped() {
        int fourth = EnergyCalculator.forCompletedFocus(25, 3, false);
        assertEquals(fourth, EnergyCalculator.forCompletedFocus(25, 4, false));
        assertEquals(fourth, EnergyCalculator.forCompletedFocus(25, 10, false));
    }

    @Test
    @DisplayName("绑定待办任务：额外乘 1.1")
    void todoBonusApplies() {
        assertEquals(28, EnergyCalculator.forCompletedFocus(25, 0, true));   // 25 × 1.1 = 27.5 → 28
    }

    @Test
    @DisplayName("连击与任务加成叠加：第 4 个番茄 + 绑定任务")
    void comboAndTodoBonusStack() {
        assertEquals(36, EnergyCalculator.forCompletedFocus(25, 3, true));   // 25 × 1.3 × 1.1 = 35.75 → 36
    }

    @Test
    @DisplayName("异常输入不产生能量")
    void invalidMinutesProduceZero() {
        assertEquals(0, EnergyCalculator.forCompletedFocus(0, 0, false));
        assertEquals(0, EnergyCalculator.forCompletedFocus(-5, 0, false));
    }

    @Test
    @DisplayName("负的连击下标按第 1 个番茄处理，不抛数组越界")
    void negativeComboIndexIsClamped() {
        assertEquals(25, EnergyCalculator.forCompletedFocus(25, -1, false));
    }

    // ---------- 中断专注 ----------

    @Test
    @DisplayName("中断专注按 0.3 倍结算：10 分钟 = 3 EP")
    void abortedFocusUsesAbortRatio() {
        assertEquals(3, EnergyCalculator.forAbortedFocus(10));
    }

    @Test
    @DisplayName("中断不足 1 分钟不产生能量")
    void abortedUnderOneMinuteProduceZero() {
        assertEquals(0, EnergyCalculator.forAbortedFocus(0));
    }
}
