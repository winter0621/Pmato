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
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * F5 数据统计：紧凑展示指标、近七天趋势与作物收集册。
 *
 * @author 唐天乐 / 郭艾迪
 * @since V1.0
 */
public class StatsController implements Initializable, PageRefreshable {
    @FXML private Label pomodoroValue, minutesValue, collectionValue, streakValue;
    @FXML private VBox chartContainer;
    private final StatsService stats = new StatsService();
    private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("MM/dd");

    @Override public void initialize(URL location, ResourceBundle resources) { refresh(); }

    /** 页面重新显示时重新读取会话与图鉴，避免停留在旧数据。 */
    @Override public void refresh() {
        int collected = stats.collectedSpeciesCount();
        int total = stats.totalSpeciesCount();
        pomodoroValue.setText(stats.totalPomodoro() + " 个");
        minutesValue.setText(stats.totalMinutes() + " 分钟");
        collectionValue.setText(collected + "/" + total);
        streakValue.setText(stats.streakDays() + " 天");
        chartContainer.getChildren().setAll(buildChartsRow(), buildCollectionRow(collected, total));
    }

    /** 两张趋势图并排，缩短首屏长度。 */
    private HBox buildChartsRow() {
        VBox minutes = wrapCard("近 7 天学习时长", buildMinutesChart());
        VBox pomodoros = wrapCard("近 7 天每日番茄", buildPomodoroChart());
        minutes.getStyleClass().add("stats-chart-card");
        pomodoros.getStyleClass().add("stats-chart-card");
        HBox row = new HBox(14, minutes, pomodoros);
        row.getStyleClass().add("stats-chart-row");
        HBox.setHgrow(minutes, Priority.ALWAYS);
        HBox.setHgrow(pomodoros, Priority.ALWAYS);
        return row;
    }

    private Node buildMinutesChart() {
        Map<String, Integer> data = recentSeven(stats.minutesByDay());
        if (data.values().stream().allMatch(value -> value == 0))
            return emptyHint("近七天暂无学习记录，完成一次专注后这里会显示趋势");
        CategoryAxis x = new CategoryAxis();
        NumberAxis y = new NumberAxis();
        configAxis(y, data.values().stream().mapToInt(Integer::intValue).max().orElse(0));
        AreaChart<String, Number> chart = new AreaChart<>(x, y);
        chart.setAnimated(false);
        chart.setLegendVisible(false);
        chart.setCreateSymbols(true);
        chart.setPrefHeight(205);
        chart.setMinHeight(205);
        chart.setMaxWidth(Double.MAX_VALUE);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        data.forEach((day, count) -> series.getData().add(new XYChart.Data<>(day, count)));
        chart.getData().add(series);
        return chart;
    }

    private Node buildPomodoroChart() {
        Map<String, Integer> data = recentSeven(stats.pomodoroByDay());
        if (data.values().stream().allMatch(value -> value == 0))
            return emptyHint("近七天暂无番茄记录");
        CategoryAxis x = new CategoryAxis();
        NumberAxis y = new NumberAxis();
        configAxis(y, data.values().stream().mapToInt(Integer::intValue).max().orElse(0));
        BarChart<String, Number> chart = new BarChart<>(x, y);
        chart.setAnimated(false);
        chart.setLegendVisible(false);
        chart.setCategoryGap(12);
        chart.setBarGap(4);
        chart.setPrefHeight(205);
        chart.setMinHeight(205);
        chart.setMaxWidth(Double.MAX_VALUE);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        data.forEach((day, count) -> series.getData().add(new XYChart.Data<>(day, count)));
        chart.getData().add(series);
        return chart;
    }

    /** 补齐无记录日期，单日数据也会呈现为正常宽度的柱子。 */
    private Map<String, Integer> recentSeven(Map<String, Integer> source) {
        Map<String, Integer> result = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            result.put(date.format(DAY_LABEL), source.getOrDefault(date.toString(), 0));
        }
        return result;
    }

    private void configAxis(NumberAxis axis, int maximum) {
        int tick = Math.max(1, (int) Math.ceil(maximum / 4.0));
        axis.setAutoRanging(false);
        axis.setLowerBound(0);
        axis.setUpperBound(Math.max(tick, tick * (int) Math.ceil(maximum / (double) tick)));
        axis.setTickUnit(tick);
    }

    /** 饼图和图鉴同排，作物较多时图鉴自然换行。 */
    private HBox buildCollectionRow(int collected, int total) {
        VBox progress = wrapCard("收集进度  " + collected + "/" + total, buildPie(collected, total));
        progress.getStyleClass().add("stats-progress-card");
        VBox album = buildAlbum();
        HBox row = new HBox(14, progress, album);
        row.getStyleClass().add("stats-collection-row");
        HBox.setHgrow(album, Priority.ALWAYS);
        return row;
    }

    private Node buildPie(int collected, int total) {
        if (total == 0) return emptyHint("图鉴暂无作物");
        PieChart chart = new PieChart();
        chart.setAnimated(false);
        chart.setLegendVisible(false);
        chart.setLabelsVisible(false);
        chart.setPrefHeight(180);
        chart.setMinHeight(180);
        chart.getData().add(new PieChart.Data("已收集", collected));
        chart.getData().add(new PieChart.Data("未收集", total - collected));
        return chart;
    }

    private VBox buildAlbum() {
        VBox panel = new VBox(12);
        panel.getStyleClass().add("stats-album-panel");
        panel.setMaxWidth(Double.MAX_VALUE);
        Label title = new Label("作物收集册");
        title.getStyleClass().add("chart-card-title");
        FlowPane pane = new FlowPane(10, 10);
        pane.prefWrapLengthProperty().bind(panel.widthProperty().subtract(32));
        List<PlantSpecies> species = stats.speciesAlbum();
        if (species.isEmpty()) {
            panel.getChildren().addAll(title, emptyHint("图鉴暂无作物"));
            return panel;
        }
        for (PlantSpecies plant : species) pane.getChildren().add(albumCard(plant));
        panel.getChildren().addAll(title, pane);
        return panel;
    }

    private VBox albumCard(PlantSpecies species) {
        boolean collected = species.isCollected();
        VBox card = new VBox(4);
        card.setPrefSize(122, 150);
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().addAll("album-card",
                collected ? "album-card-collected" : "album-card-missing");
        Node art;
        Image image = PlantImage.matureOf(species.getId());
        if (image != null) {
            ImageView view = new ImageView(image);
            view.setFitWidth(82);
            view.setFitHeight(82);
            view.setPreserveRatio(true);
            art = view;
        } else {
            Label emoji = new Label(PlantEmoji.of(species.getId()));
            emoji.setFont(new Font(54));
            art = emoji;
        }
        if (!collected) art.setOpacity(0.45);
        Label name = new Label(species.getName());
        name.getStyleClass().add("album-name");
        if (!collected) name.getStyleClass().add("album-name-missing");
        Label status = new Label(collected ? "已收集" : "未收集");
        status.getStyleClass().add(collected ? "album-state-collected" : "album-state-missing");
        card.getChildren().addAll(art, name, status);
        return card;
    }

    private VBox wrapCard(String title, Node body) {
        VBox card = new VBox(8);
        card.getStyleClass().add("chart-card");
        card.setMaxWidth(Double.MAX_VALUE);
        Label heading = new Label(title);
        heading.getStyleClass().add("chart-card-title");
        card.getChildren().addAll(heading, body);
        return card;
    }

    private Label emptyHint(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("empty-hint");
        label.setWrapText(true);
        label.setMinHeight(205);
        label.setAlignment(Pos.CENTER);
        label.setTextAlignment(TextAlignment.CENTER);
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }
}