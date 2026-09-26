package csu.mengya;

import csu.mengya.dao.FocusSessionDao;
import csu.mengya.dao.PlantSpeciesDao;
import csu.mengya.db.DBManager;
import csu.mengya.service.EnergyService;
import csu.mengya.service.TodoService;

/**
 * 应用引导（【临时实现，待组长 A 的 F0 定稿对齐】）。
 *
 * <p>负责在窗口启动前完成三件事：数据库初始化与建表、默认作物图鉴填充、
 * 事件订阅注册。B/C 独立开发期间先各自跑通，待 A 的 F0 骨架定稿后由 A 接管。</p>
 *
 * @author 郭艾迪
 * @since V1.0
 */
public final class Bootstrap {

    private Bootstrap() {
    }

    /** 启动引导：由 {@link App#start} 调用 */
    public static void init() {
        DBManager.getInstance().getConnection();   // 初始化数据库 + 建表
        new FocusSessionDao().abortStaleRunning();
        new PlantSpeciesDao().seedIfEmpty();       // 首次启动填充默认作物图鉴
        EnergyService.getInstance();               // 注册 FocusFinishedEvent 订阅
        TodoService.getInstance();
    }
}
