package csu.mengya.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * 主框架控制器（导航壳）。
 *
 * <p>负责在顶部导航点击时把对应页面加载进中央内容区。
 * 【临时实现，待组长 A 的 F0 主框架定稿后对齐】。</p>
 *
 * @author B（临时搭建）
 * @since V1.0
 */
public class MainController implements Initializable {

    @FXML
    private StackPane contentPane;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 默认进入植物园页
        load("/fxml/garden.fxml");
    }

    /** 切换到植物园页 */
    @FXML
    private void onGarden() {
        load("/fxml/garden.fxml");
    }

    /** 切换到数据统计页 */
    @FXML
    private void onStats() {
        load("/fxml/stats.fxml");
    }

    /** 加载指定 FXML 页面到内容区 */
    private void load(String path) {
        try {
            Parent page = FXMLLoader.load(getClass().getResource(path));
            contentPane.getChildren().setAll(page);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
