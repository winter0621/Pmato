package csu.mengya.service;

import csu.mengya.common.EventBus;
import csu.mengya.common.FocusFinishedEvent;

/**
 * 能量结算服务（F2 植物园，对应计划书 D5：订阅事件 → 写入能量 → 计算生长阶段）。
 *
 * <p>订阅 F1 计时模块发布的 {@link FocusFinishedEvent}，把本次结算能量交给
 * {@link GardenService} 浇灌作物。本类不 import 任何 javafx 包。</p>
 *
 * @author B
 * @since V1.0
 */
public final class EnergyService {

    private static final EnergyService INSTANCE = new EnergyService();

    /** 构造时注册事件订阅（全局只订阅一次） */
    private EnergyService() {
        EventBus.subscribe(FocusFinishedEvent.class, this::onFocusFinished);
    }

    /** 获取单例（首次调用会完成订阅） */
    public static EnergyService getInstance() {
        return INSTANCE;
    }

    /**
     * 专注完成回调：把能量交给植物园浇灌作物。
     *
     * @param event 专注完成事件（含本次结算能量）
     */
    private void onFocusFinished(FocusFinishedEvent event) {
        GardenService.getInstance().applyFocusEnergy(event.getEnergyGained());
    }
}
