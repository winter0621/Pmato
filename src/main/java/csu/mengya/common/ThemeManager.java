package csu.mengya.common;

import javafx.scene.Scene;
import javafx.scene.control.DialogPane;

import java.util.Objects;

/**
 * 主题管理器（F6 系统与设置）。
 *
 * <p>负责把浅色 / 深色两套样式表挂到主场景上，并在切换时即时生效
 * （对应验收用例 TC-6.3）。</p>
 *
 * <p><b>实现方式：</b>深色样式表排在浅色之后挂载。两份样式表的选择器结构完全一致，
 * 同优先级时后者覆盖前者，因此切换主题不需要重新加载界面 ——
 * 用户停在哪个页面、计时是否正在走，都不受切换影响。</p>
 *
 * <p><b>注意：</b>JavaFX 的 {@code Dialog}（如日程提醒弹窗）使用独立的 Scene，
 * 不会继承主场景的样式表。需要深色弹窗时，调用
 * {@link #applyToDialog(DialogPane, String)} 单独指定。</p>
 *
 * @author 侯卓轩
 * @since V1.0
 */
public final class ThemeManager {

    /** 浅色主题标识（默认） */
    public static final String LIGHT = "light";

    /** 深色主题标识 */
    public static final String DARK = "dark";

    private static final String LIGHT_CSS = "/css/theme.css";
    private static final String DARK_CSS = "/css/theme-dark.css";

    /** 主场景；由 App 启动时注册 */
    private static Scene mainScene;

    private ThemeManager() {
    }

    /**
     * 注册主场景。必须在 {@link #apply(String)} 之前调用。
     *
     * @param scene 应用主场景
     */
    public static void register(Scene scene) {
        mainScene = scene;
    }

    /**
     * 把主题应用到主场景。
     *
     * @param theme {@link #LIGHT} 或 {@link #DARK}；传入其他值按浅色处理
     */
    public static void apply(String theme) {
        if (mainScene == null) {
            return;
        }
        if (DARK.equals(theme)) {
            // 顺序是关键：深色在后，才能在优先级相同时覆盖浅色
            mainScene.getStylesheets().setAll(resource(LIGHT_CSS), resource(DARK_CSS));
        } else {
            mainScene.getStylesheets().setAll(resource(LIGHT_CSS));
        }
    }

    /**
     * 让一个对话框跟随深色主题。
     *
     * <p>对话框有独立的 Scene，不挂载样式表会保持浅色，
     * 在深色主题下形成刺眼的「白块」。</p>
     *
     * @param pane  对话框面板，取自 {@code Dialog#getDialogPane()}
     * @param theme 当前主题；非深色时不做任何事
     */
    public static void applyToDialog(DialogPane pane, String theme) {
        if (pane == null || !DARK.equals(theme)) {
            return;
        }
        pane.getStylesheets().add(resource(DARK_CSS));
    }

    /**
     * 取样式表资源的外部路径。
     *
     * @param path 类路径下的资源路径
     * @return 可供 Scene 使用的 URL 字符串
     */
    private static String resource(String path) {
        return Objects.requireNonNull(ThemeManager.class.getResource(path), "缺少样式表：" + path)
                .toExternalForm();
    }
}
