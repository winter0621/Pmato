package csu.mengya.controller;

import csu.mengya.service.StatsService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.net.URL;
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
public class StatsController implements Initializable {

    @FXML
    private Label summaryLabel;

    @FXML
    private VBox chartContainer;

    private final StatsService stats = new StatsService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        refresh();
    }

    /** 刷新全部统计卡片 */
    private void refresh() {
        summaryLabel.setText("累计番茄 " + stats.totalPomodoro()
                + " 个 · 学习 " + stats.totalMinutes() + " 分钟"
                + " · 收集 " + stats.collectedSpeciesCount() + "/" + stats.totalSpeciesCount()
                + " · 连续打卡 " + stats.streakDays() + " 天");

        chartContainer.getChildren().clear();
        buildTrendChart();
        buildPomodoroChart();
        buildCollectionChart();
    }

    /** 学习时长趋势折线图 */
    private void buildTrendChart() {
        Map<String, Integer> data = stats.minutesByDay();
        if (data.isEmpty()) {
            chartContainer.getChildren().add(emptyHint("暂无学习记录，去植物园完成一次专注吧"));
            return;
        }
        CategoryAxis x = new CategoryAxis();
        NumberAxis y = new NumberAxis();
        x.setLabel("日期");
        y.setLabel("分钟");
        LineChart<String, Number> chart = new LineChart<>(x, y);
        chart.setTitle("学习时长趋势（分钟）");
        chart.setAnimated(false);
        chart.setPrefHeight(220);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("时长");
        data.forEach((day, min) -> series.getData().add(new XYChart.Data<>(day, min)));
        chart.getData().add(series);
        chartContainer.getChildren().add(chart);
    }

    /** 每日番茄数柱状图 */
    private void buildPomodoroChart() {
        Map<String, Integer> data = stats.pomodoroByDay();
        if (data.isEmpty()) {
            chartContainer.getChildren().add(emptyHint("暂无番茄记录"));
            return;
        }
        CategoryAxis x = new CategoryAxis();
        NumberAxis y = new NumberAxis();
        x.setLabel("日期");
        y.setLabel("番茄数");
        BarChart<String, Number> chart = new BarChart<>(x, y);
        chart.setTitle("每日番茄数");
        chart.setAnimated(false);
        chart.setPrefHeight(220);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("番茄");
        data.forEach((day, count) -> series.getData().add(new XYChart.Data<>(day, count)));
        chart.getData().add(series);
        chartContainer.getChildren().add(chart);
    }

    /** 作物收集进度饼图 */
    private void buildCollectionChart() {
        int collected = stats.collectedSpeciesCount();
        int total = stats.totalSpeciesCount();
        int remaining = Math.max(0, total - collected);
        if (total == 0) {
            chartContainer.getChildren().add(emptyHint("图鉴为空"));
            return;
        }
        PieChart chart = new PieChart();
        chart.setTitle("作物收集进度（已收集 " + collected + "/" + total + "）");
        if (collected > 0) {
            chart.getData().add(new PieChart.Data("已收集 " + collected, collected));
        }
        if (remaining > 0) {
            chart.getData().add(new PieChart.Data("未收集 " + remaining, remaining));
        }
        chartContainer.getChildren().add(chart);
    }

    /** 空状态提示标签 */
    private Label emptyHint(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("empty-hint");
        return label;
    }
}
