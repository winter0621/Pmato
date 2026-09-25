package csu.mengya.controller;

import csu.mengya.common.EventBus;
import csu.mengya.common.FocusFinishedEvent;
import csu.mengya.common.GameConstants;
import csu.mengya.common.PlantEmoji;
import csu.mengya.common.PlantImage;
import csu.mengya.common.Result;
import csu.mengya.dao.FocusSessionDao;
import csu.mengya.dao.PlantSpeciesDao;
import csu.mengya.model.FocusSession;
import csu.mengya.model.GardenPlot;
import csu.mengya.model.PlantSpecies;
import csu.mengya.service.GardenService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * 植物园页面控制器（F2 植物园）。
 *
 * <p>负责：展示图鉴与地块、种植、模拟专注（开发用）、收获、重置。
 * 只调 Service，不含 SQL 与业务规则（遵循计划书 5.3 分层约定）。</p>
 *
 * @author B
 * @since V1.0
 */
public class GardenController implements Initializable {

    @FXML
    private ListView<String> speciesListView;

    @FXML
    private FlowPane plotPane;

    @FXML
    private Label totalEnergyLabel;

    @FXML
    private Label statusLabel;

    private final GardenService garden = GardenService.getInstance();
    private final PlantSpeciesDao speciesDao = new PlantSpeciesDao();
    private final FocusSessionDao sessionDao = new FocusSessionDao();

    /** 与 speciesListView 一一对应的作物列表 */
    private final List<PlantSpecies> species = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupSpeciesList();
        refreshAll();
        statusLabel.setText("就绪：选中图鉴作物点「种植」，或点「模拟专注」给作物加能量");
    }

    /**
     * 配置图鉴列表的自定义单元格：左侧显示作物成熟期缩略图，右侧显示名称与稀有度。
     * 图片资源缺失时自动回退为纯文本行，不影响列表可用性。
     */
    private void setupSpeciesList() {
        speciesListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                // 通过当前行索引找到对应的作物，取成熟期贴图作缩略图
                int idx = getIndex();
                PlantSpecies sp = (idx >= 0 && idx < species.size()) ? species.get(idx) : null;
                if (sp != null) {
                    Image img = PlantImage.matureOf(sp.getId());
                    if (img != null) {
                        ImageView thumb = new ImageView(img);
                        thumb.setFitWidth(36);
                        thumb.setFitHeight(36);
                        thumb.setPreserveRatio(true);
                        HBox box = new HBox(8);
                        box.setAlignment(Pos.CENTER_LEFT);
                        box.getChildren().addAll(thumb, new Label(item));
                        setGraphic(box);
                        setText(null);
                        return;
                    }
                }
                setText(item);
                setGraphic(null);
            }
        });
    }

    /** 种植选中作物到第一个空地块 */
    @FXML
    private void onPlant() {
        int idx = speciesListView.getSelectionModel().getSelectedIndex();
        if (idx < 0) {
            statusLabel.setText("请先在左侧图鉴选中一种作物");
            return;
        }
        PlantSpecies s = species.get(idx);
        Result<GardenPlot> r = garden.plant(s.getId());
        statusLabel.setText(r.isSuccess() ? "已种下「" + s.getName() + "」" : r.getMessage());
        refreshPlots();
    }

    /**
     * 模拟完成一次 25 分钟专注（开发用）。
     *
     * <p>A 的计时内核就绪前，用这个按钮临时触发「写会话 + 发事件」整条链路，
     * 便于独立验证能量结算与生长判定。等 F1 完成后删除此按钮，改由真实计时触发。</p>
     */
    @FXML
    private void onSimulateFocus() {
        FocusSession session = new FocusSession();
        session.setStartAt(LocalDateTime.now().toString());
        session.setEndAt(LocalDateTime.now().toString());
        session.setPlannedMin(25);
        session.setActualMin(25);
        session.setType("focus");
        session.setStatus("completed");
        session.setEnergyGained(25);
        sessionDao.insert(session);

        // 发布事件：EnergyService 订阅后会浇灌作物、结算能量
        EventBus.publish(new FocusFinishedEvent(session.getId(), 25, 25, null));

        statusLabel.setText("已模拟完成一次专注，+25 能量");
        refreshPlots();
        refreshTotalEnergy();
    }

    /** 收获第一个成熟作物 */
    @FXML
    private void onHarvest() {
        Result<GardenPlot> r = garden.harvestFirstMature();
        statusLabel.setText(r.isSuccess() ? "收获成功！" : r.getMessage());
        refreshPlots();
    }

    /** 重置植物园（开发用，便于重复测试种植流程） */
    @FXML
    private void onReset() {
        garden.resetAll();
        statusLabel.setText("植物园已重置");
        refreshPlots();
        refreshTotalEnergy();
    }

    /** 刷新整页 */
    private void refreshAll() {
        refreshSpecies();
        refreshPlots();
        refreshTotalEnergy();
    }

    /** 刷新左侧图鉴列表（带成熟期缩略图，图片缺失时回退纯文本） */
    private void refreshSpecies() {
        species.clear();
        speciesListView.getItems().clear();
        for (PlantSpecies s : speciesDao.findAll()) {
            species.add(s);
            String line = s.getName()
                    + "（" + rarityName(s.getRarity()) + "）· 解锁 " + s.getUnlockEnergy() + " 能量";
            speciesListView.getItems().add(line);
        }
    }

    /** 刷新地块网格（按 0..PLOT_COUNT-1 顺序渲染，空地显示占位） */
    private void refreshPlots() {
        plotPane.getChildren().clear();
        Map<Integer, GardenPlot> bySlot = new HashMap<>();
        for (GardenPlot p : garden.allPlots()) {
            bySlot.put(p.getSlotIndex(), p);
        }
        for (int i = 0; i < GameConstants.PLOT_COUNT; i++) {
            plotPane.getChildren().add(buildTile(i, bySlot.get(i)));
        }
    }

    /**
     * 构建单个地块卡片。
     * 空地为「土壤 + 空地」占位；已种地显示按阶段缩放的 Emoji + 阶段 + 进度条 + 能量。
     */
    private VBox buildTile(int slot, GardenPlot plot) {
        VBox tile = new VBox(6);
        tile.setPrefSize(150, 165);
        tile.setAlignment(Pos.CENTER);

        Label slotLabel = new Label("地块 " + slot);
        slotLabel.getStyleClass().add("plot-slot");

        if (plot == null) {
            tile.getStyleClass().addAll("plot-tile", "plot-empty");
            Label empty = new Label("空地");
            empty.getStyleClass().add("empty-hint");
            tile.getChildren().addAll(slotLabel, empty);
            return tile;
        }

        tile.getStyleClass().add("plot-tile");
        PlantSpecies sp = speciesDao.findById(plot.getSpeciesId());
        String name = sp != null ? sp.getName() : plot.getSpeciesId();
        boolean harvested = "harvested".equals(plot.getStatus());

        // 植物显示节点：优先加载对应阶段贴图，直观表现「生长」；图片缺失时回退 Emoji
        Node plantNode;
        Image img = PlantImage.of(plot.getSpeciesId(), plot.getStage());
        if (img != null) {
            ImageView plantImg = new ImageView(img);
            plantImg.setFitWidth(96);
            plantImg.setFitHeight(96);
            plantImg.setPreserveRatio(true);
            plantNode = plantImg;
        } else {
            // 回退方案：Emoji 字号随阶段放大（与 PlantEmoji 原占位行为一致）
            Label emoji = new Label(PlantEmoji.of(plot.getSpeciesId()));
            emoji.setFont(new Font(PlantEmoji.fontSize(plot.getStage())));
            plantNode = emoji;
        }
        if (harvested) {
            plantNode.setOpacity(0.4);   // 已收获的变淡，表示「已完成」
        }

        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("plot-name");

        String stageText = GameConstants.STAGE_NAMES[plot.getStage()] + (harvested ? " · 已收获" : "");
        Label stageLabel = new Label(stageText);
        stageLabel.getStyleClass().add("plot-stage");

        // 生长进度条：当前阶段内的成长比例，成熟满格
        ProgressBar bar = new ProgressBar(garden.stageProgress(plot));
        bar.setPrefWidth(120);
        bar.getStyleClass().add("plot-progress");

        Label energyLabel = new Label(plot.getEnergy() + " EP");
        energyLabel.getStyleClass().add("plot-energy");

        tile.getChildren().addAll(slotLabel, plantNode, nameLabel, stageLabel, bar, energyLabel);
        return tile;
    }

    /** 刷新累计能量显示 */
    private void refreshTotalEnergy() {
        totalEnergyLabel.setText("累计能量：" + garden.totalEnergy());
    }

    /** 稀有度 -> 中文名 */
    private String rarityName(String rarity) {
        if ("rare".equalsIgnoreCase(rarity)) {
            return "稀有";
        }
        if ("epic".equalsIgnoreCase(rarity)) {
            return "史诗";
        }
        return "普通";
    }
}
