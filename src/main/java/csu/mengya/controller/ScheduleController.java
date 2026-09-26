package csu.mengya.controller;

import csu.mengya.dao.ScheduleDao;
import csu.mengya.model.ScheduleEvent;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
public class ScheduleController implements Initializable {

    @FXML private DatePicker datePicker;
    @FXML private ComboBox<String> timeBox, typeBox;
    @FXML private TextField eventField;
    @FXML private GridPane weekGrid;
    @FXML private Label scheduleStatus;

    private final ScheduleDao scheduleDao = new ScheduleDao();
    private final ObservableList<Event> events = FXCollections.observableArrayList();
    private Event selected;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        datePicker.setValue(LocalDate.now());
        timeBox.setItems(FXCollections.observableArrayList("08:00", "09:00", "10:00", "11:00",
                "14:00", "15:00", "16:00", "19:00", "20:00"));
        timeBox.setValue("09:00");
        typeBox.setItems(FXCollections.observableArrayList("学习", "待办", "专注"));
        typeBox.setValue("学习");
        datePicker.valueProperty().addListener((o, a, b) -> render());
        load();
    }

    /** 从数据库加载日程并渲染周视图 */
    private void load() {
        events.clear();
        for (ScheduleEvent e : scheduleDao.findAll()) {
            LocalDate date = LocalDate.parse(e.getStartAt().substring(0, 10));
            String time = e.getStartAt().substring(11);
            events.add(new Event(e.getId(), date, time, e.getTitle()));
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
        String date = datePicker.getValue().toString();
        String startAt = date + " " + timeBox.getValue();
        // 起止时间暂存为同一时间，时长计算留待后续
        scheduleDao.insert(name, startAt, startAt, typeColor(typeBox.getValue()));
        eventField.clear();
        load();
        scheduleStatus.setText("日程已添加");
    }

    @FXML
    private void deleteSelected() {
        if (selected != null) {
            scheduleDao.delete(selected.id);
            selected = null;
            load();
            scheduleStatus.setText("日程已删除");
        }
    }

    /** 渲染当前周的 7 列日程 */
    private void render() {
        weekGrid.getChildren().clear();
        LocalDate base = datePicker.getValue().with(DayOfWeek.MONDAY);
        for (int i = 0; i < 7; i++) {
            LocalDate d = base.plusDays(i);
            Label h = new Label(d.format(DateTimeFormatter.ofPattern("E MM/dd")));
            h.getStyleClass().add("calendar-header");
            weekGrid.add(h, i, 0);

            VBox cell = new VBox(6);
            for (Event e : events) {
                if (e.date.equals(d)) {
                    Button b = new Button(e.time + "  " + e.name);
                    b.setMaxWidth(Double.MAX_VALUE);
                    b.setOnAction(x -> {
                        selected = e;
                        scheduleStatus.setText("已选择：" + e.name);
                    });
                    b.getStyleClass().add("event-button");
                    cell.getChildren().add(b);
                }
            }
            weekGrid.add(cell, i, 1);
            GridPane.setHgrow(cell, Priority.ALWAYS);
        }
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
        final LocalDate date;
        final String time, name;

        Event(long i, LocalDate d, String t, String n) {
            id = i;
            date = d;
            time = t;
            name = n;
        }
    }
}
