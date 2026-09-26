package csu.mengya.controller;

import csu.mengya.service.SettingsService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * F6 设置页面：参数保存后对下一次计时与后续提醒生效。
 *
 * @author 郭艾迪
 * @since V1.0
 */
public class SettingsController implements Initializable {
    @FXML private Spinner<Integer> focusMinutes, shortBreakMinutes, longBreakMinutes;
    @FXML private CheckBox doNotDisturb;
    @FXML private Label settingsStatus;
    private final SettingsService settings = SettingsService.getInstance();

    @Override public void initialize(URL url, ResourceBundle resources) {
        focusMinutes.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 180, settings.focusMinutes()));
        shortBreakMinutes.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 60, settings.shortBreakMinutes()));
        longBreakMinutes.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 120, settings.longBreakMinutes()));
        doNotDisturb.setSelected(settings.doNotDisturb());
    }

    @FXML private void saveSettings() {
        settings.save(focusMinutes.getValue(), shortBreakMinutes.getValue(),
                longBreakMinutes.getValue(), doNotDisturb.isSelected());
        settingsStatus.setText("设置已保存；时长对下一次计时生效");
    }
}