package csu.mengya.dao;

import csu.mengya.model.FocusSession;
import csu.mengya.model.TodoItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 待办数据访问测试。
 *
 * <p>覆盖验收用例 TC-3.1（排序）、TC-3.2（完成状态）、
 * TC-3.3（删除待办不连带删除专注会话）、TC-3.4（番茄进度）。</p>
 *
 * @author 侯卓轩
 * @since V1.0
 */
class TodoDaoTest extends DaoTestBase {

    private final TodoDao dao = new TodoDao();
    private final FocusSessionDao sessionDao = new FocusSessionDao();

    @Test
    @DisplayName("TC-3.1 未完成任务按优先级降序排列")
    void findAllSortsByPriorityDesc() {
        dao.insert("低优先级任务", 0, 1);
        dao.insert("高优先级任务", 2, 1);
        dao.insert("中优先级任务", 1, 1);

        List<TodoItem> list = dao.findAll();

        assertEquals("高优先级任务", list.get(0).getTitle());
        assertEquals("中优先级任务", list.get(1).getTitle());
        assertEquals("低优先级任务", list.get(2).getTitle());
    }

    @Test
    @DisplayName("TC-3.1 已完成的任务排到未完成任务之后")
    void completedTodoGoesLast() {
        dao.insert("未完成任务", 0, 1);          // 优先级最低
        dao.insert("已完成任务", 2, 1);          // 优先级最高，但已完成

        long doneId = dao.findAll().stream()
                .filter(t -> t.getTitle().equals("已完成任务"))
                .findFirst().orElseThrow().getId();
        dao.setStatus(doneId, "done");

        List<TodoItem> list = dao.findAll();
        assertEquals("未完成任务", list.get(0).getTitle());
        assertEquals("已完成任务", list.get(1).getTitle());
    }

    @Test
    @DisplayName("TC-3.2 标记完成时写入 completed_at")
    void setStatusRecordsCompletedAt() {
        dao.insert("待完成的任务", 1, 1);
        long id = dao.findAll().get(0).getId();

        dao.setStatus(id, "done");

        TodoItem reloaded = dao.findAll().get(0);
        assertEquals("done", reloaded.getStatus());
        assertNotNull(reloaded.getCompletedAt(), "完成时间不应为空");
    }

    @Test
    @DisplayName("TC-3.3 删除待办后专注会话保留，todo_id 被置空")
    void deleteKeepsFocusSessionAndNullsTodoId() {
        // 1. 建一条待办
        dao.insert("会被删除的任务", 1, 1);
        long todoId = dao.findAll().get(0).getId();

        // 2. 建一条绑定该待办的专注会话
        FocusSession session = new FocusSession();
        session.setStartAt(LocalDateTime.now().toString());
        session.setPlannedMin(25);
        session.setActualMin(25);
        session.setType("focus");
        session.setStatus("completed");
        session.setTodoId((int) todoId);
        session.setEnergyGained(25);
        sessionDao.insert(session);

        // 3. 删除待办
        dao.delete(todoId);

        // 4. 会话必须还在，且 todo_id 被外键设为 NULL
        List<FocusSession> sessions = sessionDao.findAll();
        assertEquals(1, sessions.size(), "删除待办不应连带删除专注会话");
        assertNull(sessions.get(0).getTodoId(), "todo_id 应被 ON DELETE SET NULL 置空");
        assertEquals(25, sessions.get(0).getEnergyGained(), "能量不应被清零");
    }

    @Test
    @DisplayName("TC-3.4 番茄进度递增，达到预计数时自动完成")
    void incrementDonePomodoroCompletesWhenReachingEstimate() {
        dao.insert("预计 2 个番茄", 1, 2);
        long id = dao.findAll().get(0).getId();

        dao.incrementDonePomodoro(id);
        assertEquals(1, dao.findAll().get(0).getDonePomodoro());
        assertEquals("doing", dao.findAll().get(0).getStatus());

        dao.incrementDonePomodoro(id);
        assertEquals(2, dao.findAll().get(0).getDonePomodoro());
        assertEquals("done", dao.findAll().get(0).getStatus(), "达到预计番茄数应自动完成");
    }

    @Test
    @DisplayName("番茄进度不会超过预计数")
    void incrementDoesNotExceedEstimate() {
        dao.insert("预计 1 个番茄", 1, 1);
        long id = dao.findAll().get(0).getId();

        dao.incrementDonePomodoro(id);
        dao.incrementDonePomodoro(id);   // status 已是 done，SQL 的 WHERE 条件会挡住

        assertTrue(dao.findAll().get(0).getDonePomodoro() <= 1);
    }
}
