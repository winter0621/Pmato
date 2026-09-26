package csu.mengya.controller;

import csu.mengya.common.PlantEmoji;
import csu.mengya.common.PlantImage;
import csu.mengya.model.PlantSpecies;
import csu.mengya.service.StatsService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * 数据统计页面控制器（F5 数据统计）。
 *
 * <p>用 JavaFX 内置 LineChart / BarChart / PieChart 展示学习趋势、番茄数与收集进度。
 * 无数据时展示空状态文案，不报错、不出现 NaN（对应验收用例 TC-5.3）。</p>
 *
 * @author B
 * @since V1.0
 */
public class StatsController implements Initializable, PageRefreshable {

    @FXML
    private Label summaryLabel;

    @FXML
    private VBox chartContainer;

    private final StatsService stats = new StatsService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        refresh();
    }

    /** 刷新全部统计卡片（页面被再次显示时调用） */
    @Override
    public void refresh() {
        summaryLabel.setText("累计番茄 " + stats.totalPomodoro()
                + " 个 · 学习 " + stats.totalMinutes() + " 分钟"
                + " · 收集 " + stats.collectedSpeciesCount() + "/" + stats.totalSpeciesCount()
                + " · 连续打卡 " + stats.streakDays() + " 天");

        chartContainer.getChildren().clear();
        buildTrendChart();
        buildPomodoroChart();
        buildCollectionChart();
        buildCollectionAlbum();
    }

    /** 学习时长趋势面积图（包进统一卡片） */
    private void buildTrendChart() {
        Map<String, Integer> data = stats.minutesByDay();
        Node body;
        if (data.isEmpty()) {
            body = emptyHint("暂无学习记录，去植物园完成一次专注吧");
        } else {
            CategoryAxis x = new CategoryAxis();
            x.setLabel("日期");
            NumberAxis y = new NumberAxis();
            y.setLabel("分钟");
            configAxis(y, data.values().stream().mapToInt(Integer::intValue).max().orElse(0));
            AreaChart<String, Number> chart = new AreaChart<>(x, y);
            chart.setAnimated(false);
            chart.setPrefHeight(230);
            chart.setLegendVisible(false);

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("时长");
            data.forEach((day, min) -> series.getData().add(new XYChart.Data<>(day, min)));
            chart.getData().add(series);
            body = chart;
        }
        chartContainer.getChildren().add(wrapCard("学习时长趋势（分钟）", body));
    }

    /** 每日番茄数柱状图（包进统一卡片） */
    private void buildPomodoroChart() {
        Map<String, Integer> data = stats.pomodoroByDay();
        Node body;
        if (data.isEmpty()) {
            body = emptyHint("暂无番茄记录");
        } else {
            CategoryAxis x = new CategoryAxis();
            x.setLabel("日期");
            NumberAxis y = new NumberAxis();
            y.setLabel("番茄数");
            configAxis(y, data.values().stream().mapToInt(Integer::intValue).max().orElse(0));
            BarChart<String, Number> chart = new BarChart<>(x, y);
            chart.setAnimated(false);
            chart.setPrefHeight(230);
            chart.setLegendVisible(false);

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("番茄");
            data.forEach((day, count) -> series.getData().add(new XYChart.Data<>(day, count)));
            chart.getData().add(series);
            body = chart;
        }
        chartContainer.getChildren().add(wrapCard("每日番茄数", body));
    }

    /**
     * 配置 Y 轴为「整数刻度 + 上限贴合数据」，避免番茄数这种小数值出现 1.25/0.75 等小数刻度。
     *
     * @param y     目标 Y 轴
     * @param maxVal 数据最大值
     */
    private void configAxis(NumberAxis y, int maxVal) {
        int tick = Math.max(1, (int) Math.ceil(maxVal / 5.0));
        int upper = tick * Math.max(1, (int) Math.ceil(maxVal / (double) tick));
        y.setAutoRanging(false);
        y.setLowerBound(0);
        y.setUpperBound(upper);
        y.setTickUnit(tick);
    }

    /** 作物收集进度饼图（包进统一卡片） */
    private void buildCollectionChart() {
        int collected = stats.collectedSpeciesCount();
        int total = stats.totalSpeciesCount();
        int remaining = Math.max(0, total - collected);
        Node body;
        if (total == 0) {
            body = emptyHint("图鉴为空");
        } else {
            PieChart chart = new PieChart();
            chart.setPrefHeight(200);
            chart.setLegendVisible(false);
            if (collected > 0) {
                chart.getData().add(new PieChart.Data("已收集 " + collected, collected));
            }
            if (remaining > 0) {
                chart.getData().add(new PieChart.Data("未收集 " + remaining, remaining));
            }
            body = chart;
        }
        chartContainer.getChildren().add(wrapCard("作物收集进度（已收集 " + collected + "/" + total + "）", body));
    }

    /** 把图表包进统一卡片（白底圆角 + 标题），与收集册视觉一致 */
    private VBox wrapCard(String title, Node body) {
        VBox card = new VBox(8);
        card.getStyleClass().add("chart-card");
        Label t = new Label(title);
        t.getStyleClass().add("chart-card-title");
        card.getChildren().addAll(t, body);
        return card;
    }

    /**
     * 收集册：全部物种卡片，展示与 F2 图鉴同款的成熟期贴图。
     * 已收集 = 全彩 + 金色边框 + 「已收集」；未收集 = 图片可见但偏暗 + 灰色名字 + 「未收集」。
     * 与 F2 图鉴的分工：图鉴管「解锁」，统计页管「收集成就」。
     */
    private void buildCollectionAlbum() {
        List<PlantSpecies> album = stats.speciesAlbum();
        if (album.isEmpty()) {
            return;
        }
        Label title = new Label("收集册");
        title.getStyleClass().add("section-title");

        FlowPane pane = new FlowPane(10, 10);
        pane.setPrefWrapLength(720);
        for (PlantSpecies sp : album) {
            boolean collected = sp.isCollected();

            VBox card = new VBox(4);
            card.setPrefSize(150, 168);
            card.setAlignment(Pos.CENTER);
            card.getStyleClass().add("album-card");
            if (collected) {
                card.getStyleClass().add("album-card-collected");
            } else {
                card.getStyleClass().add("album-card-missing");
            }

            // 成熟期贴图作卡片主图；未收集时压暗但保持可见
            Node art;
            Image img = PlantImage.matureOf(sp.getId());
            if (img != null) {
                ImageView artImg = new ImageView(img);
                artImg.setFitWidth(92);
                artImg.setFitHeight(92);
                artImg.setPreserveRatio(true);
                if (!collected) {
                    artImg.setOpacity(0.45);
                }
                art = artImg;
            } else {
                Label emoji = new Label(PlantEmoji.of(sp.getId()));
                emoji.setFont(new Font(62));
                if (!collected) {
                    emoji.setOpacity(0.45);
                }
                art = emoji;
            }

            Label name = new Label(sp.getName());
            name.getStyleClass().add("album-name");
            if (!collected) {
                name.getStyleClass().add("album-name-missing");
            }

            Label state = new Label(collected ? "已收集" : "未收集");
            state.getStyleClass().add(collected ? "album-state-collected" : "album-state-missing");

            card.getChildren().addAll(art, name, state);
            pane.getChildren().add(card);
        }
        chartContainer.getChildren().addAll(title, pane);
    }

    /** 空状态提示标签 */
    private Label emptyHint(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("empty-hint");
        return label;
    }
}
