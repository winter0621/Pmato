package csu.mengya.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * 主界面控制器（F0 工程骨架）。
 *
 * <p>Day1 仅用于验证「FXML + 控制器 + 样式」链路是否完整跑通；
 * 后续 D3 将在此扩展导航栏与页面切换逻辑。</p>
 *
 * @author 组长 A（请替换为真实姓名）
 * @since V1.0
 */
public class MainController implements Initializable {

    /** 版本/状态提示标签，绑定 main.fxml 中的 fx:id="statusLabel" */
    @FXML
    private Label statusLabel;

    /**
     * FXML 加载完成后由 JavaFX 自动回调，用于初始化控件。
     *
     * @param location  FXML 资源地址，本程序暂不关心
     * @param resources 国际化资源，本程序暂不使用
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Day1 验收标志：看到此行即代表 FXML 注入成功、环境已就绪
        statusLabel.setText("V1.0 · 环境就绪，工程骨架可运行");
    }
}
