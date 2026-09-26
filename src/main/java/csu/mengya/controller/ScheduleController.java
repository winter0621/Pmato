package csu.mengya.controller;

import csu.mengya.common.EventBus;
import csu.mengya.common.FocusRequestedEvent;
import csu.mengya.dao.ScheduleDao;
import csu.mengya.dao.TodoDao;
import csu.mengya.model.ScheduleEvent;
import csu.mengya.model.TodoItem;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * 日程规划控制器（F4，对应计划书 5.2 的 schedule_event 表）。
 *
 * <p>周视图渲染日程，数据访问改为 {@link ScheduleDao}（5.2 表结构）。
 * 起止时间简化存为同一起止时间，后续再补时长与重复规则。</p>
 *
 * @author A（界面）/ B（适配 5.2 数据访问）
 * @since V1.0
 */
public class ScheduleController implements Initializable, PageRefreshable {

    @FXML private DatePicker datePicker;
    @FXML private ComboBox<String> timeBox, endTimeBox, typeBox, repeatBox;
    @FXML private ComboBox<TodoChoice> todoBox;
    @FXML private Spinner<Integer> remindSpinner;
    @FXML private TextField eventField;
    @FXML private GridPane weekGrid;
    @FXML private Label scheduleStatus;

    private final ScheduleDao scheduleDao = new ScheduleDao();
    private final TodoDao todoDao = new TodoDao();
    private final ObservableList<Event> events = FXCollections.observableArrayList();
    private Event selected;
    private static final DateTimeFormatter DB_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        datePicker.setValue(LocalDate.now());
        List<String> times = new ArrayList<>();
        for (int hour = 0; hour < 24; hour++) times.add(String.format("%02d:00", hour));
        timeBox.setItems(FXCollections.observableArrayList(times));
        endTimeBox.setItems(FXCollections.observableArrayList(times));
        timeBox.setValue("09:00");
        endTimeBox.setValue("10:00");
        eventField.setTextFormatter(new TextFormatter<String>(change ->
                change.getControlNewText().length() <= 200 ? change : null));
        typeBox.setItems(FXCollections.observableArrayList("学习", "待办", "专注"));
        typeBox.setValue("学习");
        repeatBox.setItems(FXCollections.observableArrayList("不重复", "每天", "每周"));
        repeatBox.setValue("不重复");
        remindSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 120, 5));
        todoBox.getItems().add(new TodoChoice(null, "无关联待办"));
        for (TodoItem todo : todoDao.findAll()) {
            if (!"done".equals(todo.getStatus())) todoBox.getItems().add(new TodoChoice((int) todo.getId(), todo.getTitle()));
        }
        todoBox.getSelectionModel().selectFirst();
        datePicker.valueProperty().addListener((o, a, b) -> render());
        load();
    }

    @Override public void refresh() { load(); }

    /** 从选中日程带入关联待办，切换到专注页。 */
    @FXML private void focusSelected() {
        if (selected == null) {
            scheduleStatus.setText("请先选择一个日程");
            return;
        }
        EventBus.publish(new FocusRequestedEvent(selected.todoId));
    }

    /** 从数据库加载日程并渲染周视图 */
    private void load() {
        events.clear();
        for (ScheduleEvent e : scheduleDao.findAll()) {
            events.add(new Event(e));
        }
        render();
    }

    @FXML
    private void addEvent() {
        String name = eventField.getText().trim();
        if (name.isEmpty()) {
            scheduleStatus.setText("请输入日程名称");
            return;
        }
        LocalDateTime start = datePicker.getValue().atTime(LocalTime.parse(timeBox.getValue()));
        LocalDateTime end = datePicker.getValue().atTime(LocalTime.parse(endTimeBox.getValue()));
        if (!end.isAfter(start)) end = end.plusDays(1);
        String rule = switch (repeatBox.getValue()) {
            case "每天" -> "daily";
            case "每周" -> "weekly";
            default -> "none";
        };
        TodoChoice todo = todoBox.getValue();
        scheduleDao.insert(name, start.format(DB_TIME), end.format(DB_TIME), rule,
                remindSpinner.getValue(), todo == null ? null : todo.id, typeColor(typeBox.getValue()));
        eventField.clear();
        load();
        scheduleStatus.setText("日程已添加");
    }

    @FXML private void previousWeek() { datePicker.setValue(datePicker.getValue().minusWeeks(1)); }
    @FXML private void nextWeek() { datePicker.setValue(datePicker.getValue().plusWeeks(1)); }

    @FXML
    private void deleteSelected() {
        if (selected != null) {
            scheduleDao.delete(selected.id);
            selected = null;
            load();
            scheduleStatus.setText("日程已删除");
        }
    }

    /** 渲染当前周的 24 行、7 列时间网格。 */
    private void render() {
        if (datePicker.getValue() == null) return;
        weekGrid.getChildren().clear();
        LocalDate base = datePicker.getValue().with(DayOfWeek.MONDAY);
        Map<Integer, String> todoTitles = new HashMap<>();
        for (TodoItem t : todoDao.findAll()) todoTitles.put((int) t.getId(), t.getTitle());
        weekGrid.add(new Label("时间"), 0, 0);
        for (int i = 0; i < 7; i++) {
            LocalDate d = base.plusDays(i);
            Label h = new Label(d.format(DateTimeFormatter.ofPattern("MM/dd E", Locale.CHINA)));
            h.getStyleClass().add("calendar-header");
            weekGrid.add(h, i + 1, 0);
        }
        boolean hasEvent = false;
        for (int hour = 0; hour < 24; hour++) {
            weekGrid.add(new Label(String.format("%02d:00", hour)), 0, hour + 1);
            for (int day = 0; day < 7; day++) {
                LocalDate date = base.plusDays(day);
                LocalDateTime slot = date.atTime(hour, 0);
                VBox cell = new VBox(2);
                cell.getStyleClass().add("calendar-cell");
                for (Event e : events) {
                    LocalDateTime occurrence = e.start;
                    long step = "daily".equals(e.rule) ? 1 : 7;
                    while (occurrence.toLocalDate().isBefore(date.minusDays(1)) && !"none".equals(e.rule))
                        occurrence = occurrence.plusDays(step);
                    while (!occurrence.toLocalDate().isAfter(date)) {
                        LocalDateTime finish = occurrence.plus(e.duration);
                        if (finish.isAfter(slot) && occurrence.isBefore(slot.plusHours(1))) {
                            hasEvent = true;
                            String related = e.todoId == null ? "" : " · " + todoTitles.getOrDefault(e.todoId, "关联任务");
                            Button b = new Button(e.name + related);
                            b.setMaxWidth(Double.MAX_VALUE);
                            b.setTooltip(new Tooltip(occurrence.format(DB_TIME) + " — " + finish.format(DB_TIME)));
                            b.setOnAction(x -> { selected = e; scheduleStatus.setText("已选择：" + e.name); });
                            b.getStyleClass().add("event-button");
                            cell.getChildren().add(b);
                            break;
                        }
                        if ("none".equals(e.rule)) break;
                        occurrence = occurrence.plusDays(step);
                    }
                }
                weekGrid.add(cell, day + 1, hour + 1);
                GridPane.setHgrow(cell, Priority.ALWAYS);
            }
        }
        if (!hasEvent) scheduleStatus.setText("这一周还没有日程，添加一个学习安排吧。");
    }

    /** 日程类型 -> 颜色（存到 schedule_event.color） */
    private String typeColor(String type) {
        if ("待办".equals(type)) {
            return "#F2B84B";
        }
        if ("专注".equals(type)) {
            return "#4A90D9";
        }
        return "#4CAF7D";
    }

    /** 日程行模型 */
    static class Event {
        final long id;
        final LocalDateTime start;
        final Duration duration;
        final String name, rule;
        final Integer todoId;

        Event(ScheduleEvent source) {
            id = source.getId();
            start = LocalDateTime.parse(source.getStartAt(), DB_TIME);
            Duration storedDuration = Duration.between(start, LocalDateTime.parse(source.getEndAt(), DB_TIME));
            duration = storedDuration.isZero() || storedDuration.isNegative() ? Duration.ofHours(1) : storedDuration;
            name = source.getTitle();
            rule = source.getRepeatRule();
            todoId = source.getTodoId();
        }
    }

    private record TodoChoice(Integer id, String title) {
        @Override public String toString() { return title; }
    }
}
