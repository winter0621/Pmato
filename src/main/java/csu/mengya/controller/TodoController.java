package csu.mengya.controller;

import csu.mengya.dao.TodoDao;
import csu.mengya.model.TodoItem;
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

import java.net.URL;
import java.util.ResourceBundle;

/**
 * 待办清单控制器（F3，对应计划书 5.2 的 todo_item 表）。
 *
 * <p>提供任务的增删改查、优先级与预计番茄数。UI 复用 A 的 todo.fxml 布局，
 * 数据访问改为 {@link TodoDao}（5.2 表结构）。</p>
 *
 * @author A（界面）/ B（适配 5.2 数据访问）
 * @since V1.0
 */
public class TodoController implements Initializable {

    @FXML private TextField titleField;
    @FXML private ComboBox<String> priorityBox;
    @FXML private Spinner<Integer> tomatoSpinner;
    @FXML private TableView<Todo> todoTable;
    @FXML private TableColumn<Todo, String> doneColumn, titleColumn, priorityColumn, statusColumn;
    @FXML private TableColumn<Todo, Number> tomatoColumn;
    @FXML private Label todoStatus;

    private final TodoDao todoDao = new TodoDao();
    private final ObservableList<Todo> items = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        priorityBox.setItems(FXCollections.observableArrayList("高", "中", "低"));
        priorityBox.setValue("中");
        tomatoSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 20, 1));
        titleColumn.setCellValueFactory(x -> x.getValue().title);
        priorityColumn.setCellValueFactory(x -> x.getValue().priority);
        tomatoColumn.setCellValueFactory(x -> x.getValue().tomatoes);
        doneColumn.setCellValueFactory(x -> x.getValue().done.asString());
        statusColumn.setCellValueFactory(x -> new SimpleStringProperty(x.getValue().done.get() ? "已完成" : "待完成"));
        todoTable.setItems(items);
        load();
        todoTable.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> {
            if (b != null) {
                titleField.setText(b.title.get());
                priorityBox.setValue(b.priority.get());
                tomatoSpinner.getValueFactory().setValue(b.tomatoes.get());
            }
        });
    }

    /** 从数据库加载全部待办 */
    private void load() {
        items.clear();
        for (TodoItem t : todoDao.findAll()) {
            items.add(new Todo(t.getId(), t.getTitle(), priorityName(t.getPriority()),
                    t.getEstPomodoro(), "done".equals(t.getStatus())));
        }
    }

    @FXML
    private void addTodo() {
        String t = titleField.getText().trim();
        if (t.isEmpty()) {
            todoStatus.setText("请输入任务名称");
            return;
        }
        todoDao.insert(t, priorityValue(priorityBox.getValue()), tomatoSpinner.getValue());
        load();
        titleField.clear();
        todoStatus.setText("任务已添加");
    }

    @FXML
    private void completeSelected() {
        Todo t = todoTable.getSelectionModel().getSelectedItem();
        if (t != null) {
            todoDao.setStatus(t.id, "done");
            load();
            todoStatus.setText("任务已完成");
        }
    }

    @FXML
    private void deleteSelected() {
        Todo t = todoTable.getSelectionModel().getSelectedItem();
        if (t != null) {
            todoDao.delete(t.id);
            load();
            todoStatus.setText("任务已删除");
        }
    }

    @FXML
    private void updateSelected() {
        Todo t = todoTable.getSelectionModel().getSelectedItem();
        if (t != null) {
            todoDao.update(t.id, titleField.getText().trim(),
                    priorityValue(priorityBox.getValue()), tomatoSpinner.getValue(), t.done.get());
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
        final StringProperty title = new SimpleStringProperty(), priority = new SimpleStringProperty();
        final IntegerProperty tomatoes = new SimpleIntegerProperty();
        final BooleanProperty done = new SimpleBooleanProperty();

        Todo(long i, String t, String p, int n, boolean d) {
            id = i;
            title.set(t);
            priority.set(p);
            tomatoes.set(n);
            done.set(d);
        }
    }
}
