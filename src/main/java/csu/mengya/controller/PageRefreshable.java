package csu.mengya.controller;

/**
 * 页面刷新接口。
 *
 * <p>实现该接口的控制器可在页面被「再次显示」时刷新自身数据。
 * 配合 {@link MainController} 的页面缓存使用：FXML 只加载一次，显示时刷新数据，
 * 既避免每次切换重复解析 FXML 造成的卡顿，又保证数据不过期。</p>
 *
 * @author 唐天乐
 * @since V1.0
 */
public interface PageRefreshable {

    /** 页面显示时刷新数据 */
    void refresh();
}
