package csu.mengya.controller;

import csu.mengya.common.GameConstants;
import csu.mengya.common.PlantEmoji;
import csu.mengya.common.PlantImage;
import csu.mengya.common.Result;
import csu.mengya.dao.PlantSpeciesDao;
import csu.mengya.model.GardenPlot;
import csu.mengya.model.PlantSpecies;
import csu.mengya.service.GardenService;
import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.util.Duration;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

import java.net.URL;
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
 * @author 唐天乐
 * @since V1.0
 */
public class GardenController implements Initializable, PageRefreshable {

    @FXML
    private FlowPane collectionPane;

    @FXML
    private FlowPane plotPane;

    @FXML
    private Label totalEnergyLabel;

    @FXML
    private Label statusLabel;

    private final GardenService garden = GardenService.getInstance();
    private final PlantSpeciesDao speciesDao = new PlantSpeciesDao();

    /** 与图鉴卡片一一对应的作物列表 */
    private final List<PlantSpecies> species = new ArrayList<>();

    /** 当前选中的图鉴卡片下标，-1 表示未选中 */
    private int selectedIndex = -1;

    /** 记录上一轮各槽位的生长阶段，用于检测阶段升级并触发动画 */
    private final Map<Integer, Integer> prevStage = new HashMap<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        refreshAll();
        selectedIndex = 0;      // 默认选中第一张卡片
        refreshCollection();    // 重建以高亮默认选中
        statusLabel.setText("就绪：解锁并种下作物，完成真实专注后获得能量");
    }

    /**
     * 构建一张图鉴卡片：成熟期贴图 + 名称 + 状态行。
     * 未解锁灰暗带锁；已解锁显示稀有度边框色；已收集附金色圆点标记（对应 TC-2.7）。
     * 点击选中（供解锁/种植按钮使用），拖拽可将该作物种到空地。
     */
    private VBox buildCollectionCard(PlantSpecies sp, int idx) {
        boolean locked = sp.getUnlockEnergy() > 0 && !sp.isUnlocked();

        VBox card = new VBox(4);
        card.setPrefSize(100, 132);
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("collection-card");
        card.getStyleClass().add("collection-card-" + rarityKey(sp.getRarity()));
        if (locked) {
            card.getStyleClass().add("collection-card-locked");
        }
        if (idx == selectedIndex) {
            card.getStyleClass().add("collection-card-selected");
        }

        // 成熟期贴图作卡片主图；未解锁时压暗
        Node art;
        Image img = PlantImage.matureOf(sp.getId());
        if (img != null) {
            ImageView artImg = new ImageView(img);
            artImg.setFitWidth(64);
            artImg.setFitHeight(64);
            artImg.setPreserveRatio(true);
            if (locked) {
                artImg.setOpacity(0.35);
            }
            art = artImg;
        } else {
            Label emoji = new Label(PlantEmoji.of(sp.getId()));
            emoji.setFont(new Font(44));
            if (locked) {
                emoji.setOpacity(0.35);
            }
            art = emoji;
        }

        Label name = new Label(sp.getName());
        name.getStyleClass().add("collection-name");

        Label state = new Label();
        if (locked) {
            state.setText("🔒 需 " + sp.getUnlockEnergy() + " 能量");
            state.getStyleClass().add("collection-state-locked");
        } else if (sp.isCollected()) {
            state.setText("● 已收集");
            state.getStyleClass().add("collection-state-collected");
        } else {
            state.setText("已解锁");
            state.getStyleClass().add("collection-state-unlocked");
        }

        card.getChildren().addAll(art, name, state);

        // 点击选中（只切换高亮样式，避免整网格重建）
        card.setOnMouseClicked(e -> {
            if (selectedIndex == idx) {
                return;
            }
            selectedIndex = idx;
            for (int i = 0; i < collectionPane.getChildren().size(); i++) {
                collectionPane.getChildren().get(i).getStyleClass().remove("collection-card-selected");
            }
            card.getStyleClass().add("collection-card-selected");
        });
        // 拖拽到空地种植（未解锁不允许拖）
        card.setOnDragDetected(e -> {
            if (locked) {
                statusLabel.setText("「" + sp.getName() + "」还未解锁，需 " + sp.getUnlockEnergy() + " 能量");
                return;
            }
            Dragboard db = card.startDragAndDrop(TransferMode.COPY);
            ClipboardContent content = new ClipboardContent();
            content.putString(sp.getId());
            db.setContent(content);
            e.consume();
        });
        return card;
    }

    /** 刷新图鉴卡片网格（每次从 DAO 重建，保持与数据库一致） */
    private void refreshCollection() {
        species.clear();
        collectionPane.getChildren().clear();
        for (PlantSpecies s : speciesDao.findAll()) {
            species.add(s);
            collectionPane.getChildren().add(buildCollectionCard(s, species.size() - 1));
        }
    }

    /** 稀有度 -> CSS 类后缀（边框色） */
    private String rarityKey(String rarity) {
        if ("rare".equalsIgnoreCase(rarity)) {
            return "rare";
        }
        if ("epic".equalsIgnoreCase(rarity)) {
            return "epic";
        }
        return "common";
    }

    /** 解锁选中的作物（花能量） */
    @FXML
    private void onUnlock() {
        int idx = selectedIndex;
        if (idx < 0) {
            statusLabel.setText("请先在左侧图鉴选中一种作物");
            return;
        }
        PlantSpecies s = species.get(idx);
        Result<PlantSpecies> r = garden.unlockSpecies(s.getId());
        statusLabel.setText(r.isSuccess() ? "已解锁「" + s.getName() + "」" : r.getMessage());
        refreshCollection();
        refreshTotalEnergy();
    }

    /** 种植选中作物到第一个空地块（拖拽为另一种更直观的方式） */
    @FXML
    private void onPlant() {
        int idx = selectedIndex;
        if (idx < 0) {
            statusLabel.setText("请先在左侧图鉴选中一种作物");
            return;
        }
        PlantSpecies s = species.get(idx);
        int slot = garden.firstEmptySlot();
        if (slot < 0) {
            statusLabel.setText("地块已满，先收获成熟作物");
            return;
        }
        Result<GardenPlot> r = garden.plant(s.getId(), slot);
        statusLabel.setText(r.isSuccess() ? "已种下「" + s.getName() + "」" : r.getMessage());
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

    /** 页面被再次显示时刷新数据（实现 PageRefreshable） */
    @Override
    public void refresh() {
        refreshAll();
    }

    /** 刷新整页 */
    private void refreshAll() {
        refreshCollection();
        refreshPlots();
        refreshTotalEnergy();
    }

    /** 刷新地块网格（按 0..PLOT_COUNT-1 顺序渲染，空地显示占位） */
    private void refreshPlots() {
        plotPane.getChildren().clear();
        Map<Integer, GardenPlot> bySlot = new HashMap<>();
        for (GardenPlot p : garden.allPlots()) {
            bySlot.put(p.getSlotIndex(), p);
        }
        for (int i = 0; i < GameConstants.PLOT_COUNT; i++) {
            GardenPlot p = bySlot.get(i);
            boolean grew = p != null && prevStage.containsKey(i) && p.getStage() > prevStage.get(i);
            plotPane.getChildren().add(buildTile(i, p, grew));
        }
        // 更新本轮阶段快照，供下次检测升级
        prevStage.clear();
        for (GardenPlot p : garden.allPlots()) {
            prevStage.put(p.getSlotIndex(), p.getStage());
        }
    }

    /**
     * 构建单个地块卡片。
     * 空地为「土壤 + 空地」占位；已种地显示按阶段缩放的 Emoji + 阶段 + 进度条 + 能量。
     */
    private VBox buildTile(int slot, GardenPlot plot, boolean grew) {
        VBox tile = new VBox(4);
        tile.setPrefSize(150, 215);
        tile.setAlignment(Pos.CENTER);

        Label slotLabel = new Label("地块 " + (slot + 1));
        slotLabel.getStyleClass().add("plot-slot");

        if (plot == null) {
            tile.getStyleClass().addAll("plot-tile", "plot-empty");
            Label empty = new Label("空地");
            empty.getStyleClass().add("empty-hint");
            tile.getChildren().addAll(slotLabel, empty);
            makeDroppable(tile, slot);
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

        // 阶段升级时播放「生长」缩放动画（从 0.6 弹跳到 1.0）
        if (grew) {
            plantNode.setScaleX(0.6);
            plantNode.setScaleY(0.6);
            ScaleTransition grow = new ScaleTransition(Duration.millis(260), plantNode);
            grow.setFromX(0.6);
            grow.setFromY(0.6);
            grow.setToX(1.0);
            grow.setToY(1.0);
            grow.setInterpolator(Interpolator.EASE_OUT);
            Platform.runLater(grow::play);
        }

        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("plot-name");

        String stageText = GameConstants.STAGE_NAMES[plot.getStage()] + (harvested ? " · 已收获" : "");
        Label stageLabel = new Label(stageText);
        stageLabel.getStyleClass().add("plot-stage");

        // 生长进度条：当前阶段内的成长比例，成熟满格
        ProgressBar bar = new ProgressBar(garden.stageProgress(plot));
        bar.setPrefWidth(120);
        bar.setPrefHeight(8);
        bar.getStyleClass().add("plot-progress");

        Label energyLabel = new Label(plot.getEnergy() + " EP");
        energyLabel.getStyleClass().add("plot-energy");

        tile.getChildren().addAll(slotLabel, plantNode, nameLabel, stageLabel, bar, energyLabel);
        return tile;
    }

    /** 给空地地块绑定拖拽放置：拖拽图鉴作物到该地块种植 */
    private void makeDroppable(VBox tile, int slot) {
        tile.setOnDragOver(e -> {
            if (e.getDragboard().hasString()) {
                e.acceptTransferModes(TransferMode.COPY);
            }
            e.consume();
        });
        tile.setOnDragEntered(e -> {
            if (e.getDragboard().hasString()) {
                tile.getStyleClass().add("plot-drop-target");
            }
            e.consume();
        });
        tile.setOnDragExited(e -> {
            tile.getStyleClass().remove("plot-drop-target");
            e.consume();
        });
        tile.setOnDragDropped(e -> {
            boolean ok = false;
            if (e.getDragboard().hasString()) {
                String speciesId = e.getDragboard().getString();
                Result<GardenPlot> r = garden.plant(speciesId, slot);
                statusLabel.setText(r.isSuccess() ? "已种下" : r.getMessage());
                if (r.isSuccess()) {
                    refreshPlots();
                    refreshTotalEnergy();
                    ok = true;
                }
            }
            e.setDropCompleted(ok);
            e.consume();
        });
    }

    /** 刷新累计能量显示 */
    private void refreshTotalEnergy() {
        totalEnergyLabel.setText("累计能量：" + garden.totalEnergy());
    }
}
