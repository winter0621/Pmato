package csu.mengya.service;

import csu.mengya.common.EventBus;
import csu.mengya.common.FocusFinishedEvent;
import csu.mengya.common.TodoCompletedEvent;
import csu.mengya.dao.TodoDao;
import csu.mengya.model.TodoItem;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 待办与专注会话的联动服务（F3）。
 *
 * <p>职责：订阅 F1 计时模块发布的 {@link FocusFinishedEvent}，
 * 把「完成了一次专注」反映到关联待办的番茄进度上；当进度达到预计番茄数时，
 * 该待办自动完成，并发布 {@link TodoCompletedEvent} 通知统计模块。</p>
 *
 * <p>订阅在构造时完成且全局只做一次 —— 这样切换页面不会重复注册，
 * 否则一次专注会被记成多个番茄。</p>
 *
 * <p>数据库操作放在单线程执行器上，避免阻塞发布事件的界面线程。</p>
 *
 * @author 侯卓轩
 * @since V1.0
 */
public final class TodoService {

    /** 全局唯一实例 */
    private static final TodoService INSTANCE = new TodoService();

    /** 待办数据访问 */
    private final TodoDao dao = new TodoDao();

    /** 事件处理线程；单线程保证进度递增不会并发写同一条记录 */
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "todo-event-worker");
        thread.setDaemon(true);
        return thread;
    });

    private TodoService() {
        EventBus.subscribe(FocusFinishedEvent.class, event -> {
            // 只有绑定了待办的专注才需要更新进度
            if (event.getTodoId() != null) {
                executor.execute(() -> updateProgress(event.getTodoId()));
            }
        });
    }

    /**
     * 获取全局唯一实例（首次调用时完成事件订阅）。
     *
     * @return TodoService 单例
     */
    public static TodoService getInstance() {
        return INSTANCE;
    }

    /**
     * 递增指定待办的番茄进度，达到预计数时自动完成。
     *
     * <p>先查出递增前的状态做判断，再执行更新 —— 因为
     * {@code incrementDonePomodoro} 的 SQL 里带有 {@code status != 'done'} 条件，
     * 已经完成的任务不会被重复递增。</p>
     *
     * @param todoId 关联的待办 id
     */
    private void updateProgress(Integer todoId) {
        TodoItem before = dao.findAll().stream()
                .filter(t -> t.getId() == todoId)
                .findFirst()
                .orElse(null);

        // 待办已被删除，或已经是完成状态，都不需要再递增
        if (before == null || "done".equals(before.getStatus())) {
            return;
        }

        dao.incrementDonePomodoro(todoId);

        // 递增后刚好达到预计番茄数，说明这次专注让任务完成了
        if (before.getDonePomodoro() + 1 >= before.getEstPomodoro()) {
            EventBus.publish(new TodoCompletedEvent(before.getId(), before.getTitle()));
        }
    }
}
