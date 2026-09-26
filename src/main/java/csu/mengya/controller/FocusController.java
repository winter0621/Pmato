package csu.mengya.controller;

import csu.mengya.dao.TodoDao;
import csu.mengya.model.TodoItem;
import csu.mengya.service.FocusService;
import csu.mengya.service.SettingsService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * F1 计时页面：只负责展示后台计时状态，并把用户操作交给 FocusService。
 *
 * @author 郭艾迪
 * @since V1.0
 */
public class FocusController implements Initializable, PageRefreshable {
    @FXML private Label timerLabel, modeLabel, focusStatus, cycleLabel;
    @FXML private ComboBox<TodoChoice> todoBox;
    @FXML private Spinner<Integer> minutesSpinner;
    @FXML private Button startButton, pauseButton, resumeButton, abandonButton, skipButton;
    private final FocusService focus = FocusService.getInstance();
    private String lastNotice = "";

    @Override public void initialize(URL url, ResourceBundle resources) {
        minutesSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 180, SettingsService.getInstance().focusMinutes()));
        focus.setListener(snapshot -> Platform.runLater(() -> show(snapshot)));
        refresh();
    }

    @Override public void refresh() {
        Integer selected = todoBox.getValue() == null ? null : todoBox.getValue().id();
        todoBox.setItems(FXCollections.observableArrayList());
        todoBox.getItems().add(new TodoChoice(null, "不绑定待办"));
        for (TodoItem todo : new TodoDao().findAll()) {
            if (!"done".equals(todo.getStatus()))
                todoBox.getItems().add(new TodoChoice((int) todo.getId(), todo.getTitle()));
        }
        selectTodo(selected);
        if (!focus.snapshot().running()) minutesSpinner.getValueFactory().setValue(SettingsService.getInstance().focusMinutes());
        show(focus.snapshot());
    }

    /** 从待办或日程跳转时带入任务，用户可直接点击开始。 */
    public void selectTodo(Integer todoId) {
        for (TodoChoice choice : todoBox.getItems()) {
            if (java.util.Objects.equals(choice.id(), todoId)) {
                todoBox.setValue(choice);
                return;
            }
        }
        todoBox.getSelectionModel().selectFirst();
    }

    @FXML private void startFocus() {
        TodoChoice selected = todoBox.getValue();
        focus.start(minutesSpinner.getValue(), selected == null ? null : selected.id())
                .thenAccept(result -> Platform.runLater(() -> {
                    if (!result.isSuccess()) focusStatus.setText(result.getMessage());
                }));
    }
    @FXML private void pauseFocus() { report(focus.pause()); }
    @FXML private void resumeFocus() { report(focus.resume()); }
    @FXML private void abandonFocus() { report(focus.abandon()); }
    @FXML private void skipBreak() { report(focus.skipBreak()); }

    private void report(java.util.concurrent.CompletableFuture<csu.mengya.common.Result<String>> future) {
        future.thenAccept(result -> Platform.runLater(() -> {
            if (!result.isSuccess()) focusStatus.setText(result.getMessage());
        }));
    }

    private void show(FocusService.Snapshot state) {
        boolean work = "focus".equals(state.mode());
        modeLabel.setText(work ? "专注学习" : "long_break".equals(state.mode()) ? "长休息" : "短休息");
        long seconds = state.running() ? state.remainingSeconds() : minutesSpinner.getValue() * 60L;
        timerLabel.setText(String.format("%02d:%02d", seconds / 60, seconds % 60));
        cycleLabel.setText("已完成 " + state.completedInCycle() + " 个番茄");
        focusStatus.setText(state.message());
        startButton.setDisable(state.running());
        pauseButton.setDisable(!state.running() || state.paused());
        resumeButton.setDisable(!state.running() || !state.paused());
        abandonButton.setDisable(!state.running());
        skipButton.setDisable(!state.running() || work);
        todoBox.setDisable(state.running());
        minutesSpinner.setDisable(state.running());
        if (state.message().startsWith("专注完成") && !state.message().equals(lastNotice)
                && !SettingsService.getInstance().doNotDisturb()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("专注完成");
            alert.setHeaderText("做得好，休息一下");
            alert.setContentText(state.message());
            alert.show();
        }
        lastNotice = state.message();
    }

    private record TodoChoice(Integer id, String title) {
        @Override public String toString() { return title; }
    }
}