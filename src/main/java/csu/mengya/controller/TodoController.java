package csu.mengya.controller;

import csu.mengya.dao.TodoDao;
import csu.mengya.model.TodoItem;
import csu.mengya.common.EventBus;
import csu.mengya.common.TodoCompletedEvent;
import csu.mengya.common.FocusRequestedEvent;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextFormatter;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * 待办清单控制器（F3，对应计划书 5.2 的 todo_item 表）。
 *
 * <p>职责：接收界面操作、调用 {@link TodoDao} 读写数据、把结果渲染到表格。
 * 本类不含业务规则，也不直接写 SQL（计划书 5.3 分层约定）。</p>
 *
 * <p>与其他模块的协作：</p>
 * <ul>
 *   <li>点「开始专注」时发布 {@link FocusRequestedEvent}，由 F1 计时模块响应；</li>
 *   <li>标记完成时发布 {@link TodoCompletedEvent}，由 F5 统计模块响应。</li>
 * </ul>
 *
 * @author 侯卓轩
 * @since V1.0
 */


public class TodoController implements Initializable, PageRefreshable {

    @FXML private TextField titleField;
    @FXML private DatePicker dueDatePicker;
    @FXML private ComboBox<String> priorityBox;
    @FXML private Spinner<Integer> tomatoSpinner;
    @FXML private TableView<Todo> todoTable;
    @FXML private TableColumn<Todo, String> doneColumn, titleColumn, priorityColumn, dueColumn, progressColumn, statusColumn;
    @FXML private TableColumn<Todo, Number> tomatoColumn;
    @FXML private Label todoStatus;

    private final TodoDao todoDao = new TodoDao();
    private final ObservableList<Todo> items = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        priorityBox.setItems(FXCollections.observableArrayList("高", "中", "低"));
        priorityBox.setValue("中");
        tomatoSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 20, 1));
        titleField.setTextFormatter(new TextFormatter<String>(change ->
                change.getControlNewText().length() <= 200 ? change : null));
        titleColumn.setCellValueFactory(x -> x.getValue().title);
        priorityColumn.setCellValueFactory(x -> x.getValue().priority);
        tomatoColumn.setCellValueFactory(x -> x.getValue().tomatoes);
        doneColumn.setCellValueFactory(x -> new SimpleStringProperty(x.getValue().done.get() ? "✓" : "—"));
        dueColumn.setCellValueFactory(x -> x.getValue().dueDate);
        progressColumn.setCellValueFactory(x -> x.getValue().progress);
        statusColumn.setCellValueFactory(x -> new SimpleStringProperty(x.getValue().done.get() ? "已完成" : "待完成"));
        todoTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        todoTable.setItems(items);
        todoTable.setPlaceholder(new Label("还没有待办，先添加一个学习任务吧。"));
        load();
        todoTable.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> {
            if (b != null) {
                titleField.setText(b.title.get());
                priorityBox.setValue(b.priority.get());
                dueDatePicker.setValue(b.dueDate.get().isEmpty() ? null : java.time.LocalDate.parse(b.dueDate.get()));
                tomatoSpinner.getValueFactory().setValue(b.tomatoes.get());
            }
        });
    }

    @Override public void refresh() { load(); }

    /** 选中待办后切换到专注页。 */
    @FXML private void focusSelected() {
        Todo selected = todoTable.getSelectionModel().getSelectedItem();
        if (selected == null || selected.done.get()) {
            todoStatus.setText("请先选中未完成的任务");
            return;
        }
        EventBus.publish(new FocusRequestedEvent((int) selected.id));
    }

    /** 从数据库加载全部待办 */
    private void load() {
        items.clear();
        for (TodoItem t : todoDao.findAll()) {
            items.add(new Todo(t.getId(), t.getTitle(), priorityName(t.getPriority()), t.getDueDate(),
                    t.getDonePomodoro(), t.getEstPomodoro(), "done".equals(t.getStatus())));
        }
    }

    @FXML
    private void addTodo() {
        String t = titleField.getText().trim();
        if (t.isEmpty()) {
            todoStatus.setText("请输入任务名称");
            return;
        }
        todoDao.insert(t, priorityValue(priorityBox.getValue()),
                dueDatePicker.getValue() == null ? null : dueDatePicker.getValue().toString(), tomatoSpinner.getValue());
        load();
        titleField.clear();
        todoStatus.setText("任务已添加");
    }

    @FXML
    private void completeSelected() {
        Todo t = todoTable.getSelectionModel().getSelectedItem();
        if (t != null) {
            todoDao.setStatus(t.id, "done");
            EventBus.publish(new TodoCompletedEvent(t.id, t.title.get()));
            load();
            todoStatus.setText("任务已完成");
        }
    }

    @FXML
    private void deleteSelected() {
        Todo t = todoTable.getSelectionModel().getSelectedItem();
        if (t != null) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "确定删除任务“" + t.title.get() + "”？", ButtonType.OK, ButtonType.CANCEL);
            confirm.setHeaderText("删除待办");
            if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
            todoDao.delete(t.id);
            load();
            todoStatus.setText("任务已删除");
        }
    }

    @FXML
    private void updateSelected() {
        Todo t = todoTable.getSelectionModel().getSelectedItem();
        if (t != null) {
            String title = titleField.getText().trim();
            if (title.isEmpty()) { todoStatus.setText("请输入任务名称"); return; }
            todoDao.update(t.id, title, priorityValue(priorityBox.getValue()),
                    dueDatePicker.getValue() == null ? null : dueDatePicker.getValue().toString(),
                    tomatoSpinner.getValue(), t.done.get());
            load();
            todoStatus.setText("任务已更新");
        }
    }

    /** 优先级 高/中/低 -> 2/1/0 */
    private int priorityValue(String name) {
        if ("高".equals(name)) {
            return 2;
        }
        if ("低".equals(name)) {
            return 0;
        }
        return 1;
    }

    /** 优先级 2/1/0 -> 高/中/低 */
    private String priorityName(int value) {
        if (value >= 2) {
            return "高";
        }
        if (value <= 0) {
            return "低";
        }
        return "中";
    }

    /** 表格行模型（JavaFX 属性） */
    static class Todo {
        final long id;
        final StringProperty title = new SimpleStringProperty(), priority = new SimpleStringProperty(), dueDate = new SimpleStringProperty(), progress = new SimpleStringProperty();
        final IntegerProperty tomatoes = new SimpleIntegerProperty();
        final BooleanProperty done = new SimpleBooleanProperty();

        Todo(long i, String t, String p, String due, int completed, int n, boolean d) {
            id = i;
            title.set(t);
            priority.set(p);
            dueDate.set(due == null ? "" : due);
            progress.set(completed + "/" + n);
            tomatoes.set(n);
            done.set(d);
        }
    }
}
