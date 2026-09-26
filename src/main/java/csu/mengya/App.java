package csu.mengya;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

/**
 * JavaFX 应用主类（F0 工程骨架）。
 *
 * <p>职责：加载主界面 FXML、挂载全局样式、显示主窗口。
 * 后续 D3 将在此处扩展页面路由（主框架与多页面切换）。</p>
 *
 * <p>注意：本类由 {@link Launcher} 调用 {@link Application#launch(Class, String...)}
 * 启动，自身不提供 main，以免直接运行时触发 JavaFX 的 module-path 检查报错。</p>
 *
 * @author 组长 A（请替换为真实姓名）
 * @since V1.0
 */
public class App extends Application {

    /** 应用显示名称，供标题栏与后续软著材料引用 */
    private static final String APP_NAME = "pmato";

    /** 主窗口默认宽（像素） */
    private static final double WIDTH = 960;

    /** 主窗口默认高（像素） */
    private static final double HEIGHT = 640;

    /**
     * JavaFX 启动入口，由 {@link Launcher} 调用。
     *
     * @param stage 主舞台（窗口），由 JavaFX 运行时创建并传入
     * @throws Exception 加载 FXML 资源失败时抛出
     */
    @Override
    public void start(Stage stage) throws Exception {
        // 初始化数据库、种子图鉴、事件订阅（统一走计划书 5.2 的 schema）
        Bootstrap.init();
        // 加载主界面布局（FXML 内部会实例化 MainController）
        FXMLLoader loader = new FXMLLoader(
                Objects.requireNonNull(getClass().getResource("/fxml/main.fxml")));
        Parent root = loader.load();

        // 构建场景并挂载全局主题样式（颜色、圆角等集中定义在 theme.css）
        Scene scene = new Scene(root, WIDTH, HEIGHT);
        scene.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/css/theme.css")).toExternalForm());

        // 配置窗口并显示
        stage.setTitle(APP_NAME);
        stage.setScene(scene);
        stage.show();
    }
}
