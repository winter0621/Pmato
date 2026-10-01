package csu.mengya.controller;

import csu.mengya.common.ThemeManager;
import csu.mengya.service.BackupService;
import csu.mengya.service.SettingsService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * F6 设置页面：番茄节奏、提醒方式、外观主题与数据备份。
 *
 * <p>参数保存后对下一次计时与后续提醒生效；主题切换则是立即生效，
 * 不需要用户再点「保存设置」。</p>
 *
 * @author 郭艾迪
 * @since V1.0
 */
public class SettingsController implements Initializable {

    /** 主题下拉框的显示文案；下标与 {@link #THEME_KEYS} 一一对应 */
    private static final String[] THEME_LABELS = {"浅色", "深色"};

    /** 主题下拉框文案对应的内部标识 */
    private static final String[] THEME_KEYS = {ThemeManager.LIGHT, ThemeManager.DARK};

    @FXML private Spinner<Integer> focusMinutes, shortBreakMinutes, longBreakMinutes;
    @FXML private CheckBox doNotDisturb;
    @FXML private ComboBox<String> themeBox;
    @FXML private Label settingsStatus;
    private final SettingsService settings = SettingsService.getInstance();

    @Override public void initialize(URL url, ResourceBundle resources) {
        focusMinutes.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 180, settings.focusMinutes()));
        shortBreakMinutes.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 60, settings.shortBreakMinutes()));
        longBreakMinutes.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 120, settings.longBreakMinutes()));
        doNotDisturb.setSelected(settings.doNotDisturb());

        // 外观主题：选中即生效，无需再点保存
        themeBox.getItems().setAll(THEME_LABELS);
        themeBox.setValue(ThemeManager.DARK.equals(settings.theme()) ? THEME_LABELS[1] : THEME_LABELS[0]);
        themeBox.valueProperty().addListener((observable, oldValue, newValue) -> applyTheme(newValue));
    }

    /**
     * 切换主题：立即应用并持久化。
     *
     * @param label 下拉框选中的文案
     */
    private void applyTheme(String label) {
        String key = THEME_LABELS[1].equals(label) ? ThemeManager.DARK : ThemeManager.LIGHT;
        ThemeManager.apply(key);
        settings.saveTheme(key);
        settingsStatus.setText("已切换到" + label + "主题");
    }

    @FXML private void saveSettings() {
        settings.save(focusMinutes.getValue(), shortBreakMinutes.getValue(),
                longBreakMinutes.getValue(), doNotDisturb.isSelected());
        settingsStatus.setText("设置已保存；时长对下一次计时生效");
    }

    /**
     * 导出备份：把当前数据库导出为独立的 .db 文件。
     *
     * <p>导出过程中计时、提醒和界面都能继续使用，不需要退出程序。</p>
     */
    @FXML private void exportBackup() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("导出备份");
        chooser.setInitialFileName(BackupService.defaultFileName());
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQLite 数据库", "*.db"));

        File target = chooser.showSaveDialog(window());
        if (target == null) {
            return;         // 用户取消，不做任何提示
        }
        settingsStatus.setText(BackupService.exportTo(target).getMessage());
    }

    /**
     * 导入备份：用备份文件覆盖当前数据库。
     *
     * <p>因为会覆盖全部学习记录，所以先弹确认框；确认后由
     * {@link BackupService} 负责校验文件头并安全替换。</p>
     */
    @FXML private void importBackup() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("选择备份文件");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQLite 数据库", "*.db"));

        File source = chooser.showOpenDialog(window());
        if (source == null) {
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "导入会用备份中的数据覆盖当前全部学习记录、待办与日程，且无法撤销。\n确定继续吗？",
                ButtonType.OK, ButtonType.CANCEL);
        confirm.setHeaderText("导入备份");
        confirm.initOwner(window());
        // 对话框使用独立 Scene，不挂样式表会在深色主题下变成刺眼的白块
        ThemeManager.applyToDialog(confirm.getDialogPane(), settings.theme());
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        settingsStatus.setText(BackupService.importFrom(source).getMessage());
    }

    /**
     * 取当前窗口，用作文件选择器与对话框的父窗口。
     *
     * @return 当前窗口；界面尚未挂载时返回 null（对话框将不受父窗口约束）
     */
    private Window window() {
        return settingsStatus.getScene() == null ? null : settingsStatus.getScene().getWindow();
    }
}
