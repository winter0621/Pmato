package csu.mengya.controller;

/**
 * 页面刷新接口：实现它的控制器在页面被再次显示时刷新数据，
 * 配合 MainController 的页面缓存使用（FXML 只解析一次，切换时只刷数据）。
 *
 * @author 唐天乐
 * @since V1.0
 */
public interface PageRefreshable {

    void refresh();
}
