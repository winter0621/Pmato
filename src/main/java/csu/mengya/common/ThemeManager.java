package csu.mengya.common;

import javafx.scene.Scene;
import javafx.scene.control.DialogPane;

import java.util.Objects;

/**
 * 主题管理器。
 *
 * <p>浅色与深色两套样式表的选择器结构完全一致，深色挂在后面覆盖浅色，
 * 所以切换主题不需要重建界面，正在跑的计时也不会被打断。
 *
 * @author 侯卓轩
 * @since V1.0
 */
public final class ThemeManager {

    public static final String LIGHT = "light";
    public static final String DARK = "dark";

    private static final String LIGHT_CSS = "/css/theme.css";
    private static final String DARK_CSS = "/css/theme-dark.css";

    private static Scene mainScene;

    private ThemeManager() {
    }

    /** 注册主场景，必须在 apply 之前调用 */
    public static void register(Scene scene) {
        mainScene = scene;
    }

    /**
     * 应用主题。
     *
     * @param theme LIGHT 或 DARK，其他值按浅色处理
     */
    public static void apply(String theme) {
        if (mainScene == null) {
            return;
        }
        if (DARK.equals(theme)) {
            // 顺序关键：深色在后才能覆盖浅色
            mainScene.getStylesheets().setAll(resource(LIGHT_CSS), resource(DARK_CSS));
        } else {
            mainScene.getStylesheets().setAll(resource(LIGHT_CSS));
        }
    }

    /**
     * 让弹窗跟随深色主题。
     *
     * <p>JavaFX 的 Dialog 有独立 Scene，不挂样式表会在深色下弹出白块。
     *
     * @param pane  取自 Dialog#getDialogPane()
     * @param theme 当前主题
     */
    public static void applyToDialog(DialogPane pane, String theme) {
        if (pane == null || !DARK.equals(theme)) {
            return;
        }
        pane.getStylesheets().add(resource(DARK_CSS));
    }

    private static String resource(String path) {
        return Objects.requireNonNull(ThemeManager.class.getResource(path), "缺少样式表：" + path)
                .toExternalForm();
    }
}
