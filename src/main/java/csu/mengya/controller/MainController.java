package csu.mengya.controller;

import csu.mengya.common.EventBus;
import csu.mengya.common.FocusRequestedEvent;
import csu.mengya.common.FocusFinishedEvent;
import csu.mengya.service.StatsService;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * 主框架控制器（F0 主框架）。
 *
 * <p>左侧边栏提供 F1-F6 六个模块入口，中央内容区切换对应页面。
 * 页面采用缓存：首次加载 FXML 后复用，切换时只刷新数据，避免重复解析导致的卡顿。
 * F1–F6 均可从侧边栏进入，专注完成后按事件刷新关联模块。</p>
 *
 * @author 郭艾迪
 * @since V1.0
 */
public class MainController implements Initializable {

    @FXML
    private Label statusLabel, contentTitle, contentDescription, todayFocusLabel;

    @FXML
    private StackPane contentHost;

    /** 页面缓存：路径 -> 已加载的页面（根节点 + 控制器） */
    private final Map<String, CachedPage> pageCache = new HashMap<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 默认进入植物园（已实现）
        EventBus.subscribe(FocusRequestedEvent.class, event -> Platform.runLater(() -> openFocus(event.todoId())));
        EventBus.subscribe(FocusFinishedEvent.class, event -> Platform.runLater(this::refreshToday));
        refreshToday();
        showGarden(null);
    }

    /** F1 专注学习：保留页面与计时状态，切换页面不会中断计时。 */
    @FXML
    public void showFocus(ActionEvent e) {
        load("/fxml/focus.fxml", "F1  专注学习计时", "专注、休息与任务进度联动");
    }

    /** 从待办或提醒进入计时页，并选中对应任务。 */
    public void openFocus(Integer todoId) {
        showFocus(null);
        CachedPage page = pageCache.get("/fxml/focus.fxml");
        if (page != null && page.controller instanceof FocusController controller)
            controller.selectTodo(todoId);
    }

    /** F2 植物园：B 已实现 */
    @FXML
    public void showGarden(ActionEvent e) {
        load("/fxml/garden.fxml", "F2  植物园养成", "学习时长转化为能量，培育并收获作物");
    }

    /** F3 待办清单 */
    @FXML
    public void showTodo(ActionEvent e) {
        load("/fxml/todo.fxml", "F3  待办清单", "任务增删改查、优先级和预计番茄数");
    }

    /** F4 日程规划 */
    @FXML
    public void showSchedule(ActionEvent e) {
        load("/fxml/schedule.fxml", "F4  日程规划", "周视图日历、时间段规划和提醒");
    }

    /** F5 数据统计：B 已实现 */
    @FXML
    public void showStats(ActionEvent e) {
        load("/fxml/stats.fxml", "F5  数据统计", "用数据观察你的专注和成长");
    }

    /** F6 设置：时长与免打扰会影响计时和提醒模块。 */
    @FXML
    public void showSettings(ActionEvent e) {
        load("/fxml/settings.fxml", "F6  系统与设置", "调整专注时长、休息和提醒");
    }

    /** 按需调用 AI，帮助用户拆解任务与安排学习时间。 */
    @FXML
    public void showAssistant(ActionEvent e) {
        load("/fxml/assistant.fxml", "学习助手", "拆解待办、规划学习时间，或提出你的问题");
    }

    /** 占位模块：显示「待接入」提示卡片 */
    private void home(String title, String desc, String heading, String detail) {
        contentTitle.setText(title);
        contentDescription.setText(desc);
        VBox box = new VBox(14);
        box.setAlignment(Pos.CENTER);
        Label icon = new Label("🌱");
        icon.getStyleClass().add("module-icon");
        Label h = new Label(heading);
        h.getStyleClass().add("module-title");
        Label d = new Label(detail);
        d.getStyleClass().add("module-detail");
        box.getChildren().addAll(icon, h, d);
        contentHost.getChildren().setAll(box);
        statusLabel.setText("系统就绪 · " + title.substring(0, 2));
    }

    /**
     * 加载页面（带缓存）：首次加载 FXML，之后复用；显示时若控制器实现了
     * {@link PageRefreshable}，则调用其 refresh() 刷新数据。
     */
    private void load(String path, String title, String desc) {
        CachedPage page = pageCache.get(path);
        if (page == null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
                Parent root = loader.load();
                page = new CachedPage(root, loader.getController());
                pageCache.put(path, page);
            } catch (IOException e) {
                statusLabel.setText("页面加载失败");
                e.printStackTrace();
                return;
            }
        }
        contentHost.getChildren().setAll(page.root);
        contentTitle.setText(title);
        contentDescription.setText(desc);
        statusLabel.setText("系统就绪 · " + title.substring(0, 2));
        if (page.controller instanceof PageRefreshable) {
            ((PageRefreshable) page.controller).refresh();
        }
    }

    /** 主框架显示今天的真实专注分钟数。 */
    private void refreshToday() {
        int minutes = new StatsService().minutesByDay().getOrDefault(LocalDate.now().toString(), 0);
        todayFocusLabel.setText("今日专注  " + minutes + " 分钟");
    }

    /** 缓存的页面：根节点 + 控制器 */
    private static class CachedPage {
        final Parent root;
        final Object controller;

        CachedPage(Parent root, Object controller) {
            this.root = root;
            this.controller = controller;
        }
    }
}
