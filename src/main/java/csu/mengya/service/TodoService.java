package csu.mengya.service;

import csu.mengya.common.EventBus;
import csu.mengya.common.FocusFinishedEvent;
import csu.mengya.common.TodoCompletedEvent;
import csu.mengya.dao.TodoDao;
import csu.mengya.model.TodoItem;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** F3 待办与专注会话联动。应用启动时只订阅一次，避免切换页面后重复计数。 */
public final class TodoService {
    private static final TodoService INSTANCE = new TodoService();
    private final TodoDao dao = new TodoDao();
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "todo-event-worker");
        thread.setDaemon(true);
        return thread;
    });

    private TodoService() {
        EventBus.subscribe(FocusFinishedEvent.class, event -> {
            if (event.getTodoId() != null) executor.execute(() -> updateProgress(event.getTodoId()));
        });
    }

    public static TodoService getInstance() { return INSTANCE; }

    private void updateProgress(Integer todoId) {
        TodoItem before = dao.findAll().stream().filter(t -> t.getId() == todoId).findFirst().orElse(null);
        if (before == null || "done".equals(before.getStatus())) return;
        dao.incrementDonePomodoro(todoId);
        if (before.getDonePomodoro() + 1 >= before.getEstPomodoro())
            EventBus.publish(new TodoCompletedEvent(before.getId(), before.getTitle()));
    }
}
