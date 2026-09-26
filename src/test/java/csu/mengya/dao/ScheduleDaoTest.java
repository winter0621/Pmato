package csu.mengya.dao;

import csu.mengya.model.ScheduleEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 日程数据访问测试。
 *
 * <p>覆盖验收用例 TC-4.3（重复规则存储）与「删除关联待办后 todo_id 置空」。</p>
 *
 * <p>注意时间格式统一为 {@code yyyy-MM-dd HH:mm}，与
 * {@code ScheduleController.DB_TIME} 保持一致。</p>
 *
 * @author 侯卓轩
 * @since V1.0
 */
class ScheduleDaoTest extends DaoTestBase {

    private final ScheduleDao dao = new ScheduleDao();
    private final TodoDao todoDao = new TodoDao();

    @Test
    @DisplayName("新增日程后能查出来，字段完整")
    void insertAndFindAll() {
        dao.insert("数学复习", "2026-10-01 14:00", "2026-10-01 15:30", "daily", 5, null, "#4CAF7D");

        List<ScheduleEvent> list = dao.findAll();
        assertEquals(1, list.size());

        ScheduleEvent e = list.get(0);
        assertEquals("数学复习", e.getTitle());
        assertEquals("daily", e.getRepeatRule());
        assertEquals(5, e.getRemindBefore());
        assertEquals("#4CAF7D", e.getColor());
    }

    @Test
    @DisplayName("TC-4.3 每周重复规则能正确存储与读回")
    void weeklyRepeatRuleRoundTrip() {
        dao.insert("周会", "2026-10-02 09:00", "2026-10-02 10:00", "weekly", 10, null, "#F2B84B");

        assertEquals("weekly", dao.findAll().get(0).getRepeatRule());
    }

    @Test
    @DisplayName("不重复的日程 repeat_rule 为 none")
    void defaultRepeatRuleIsNone() {
        dao.insert("一次性会议", "2026-10-03 09:00", "2026-10-03 10:00", "none", 5, null, null);

        assertEquals("none", dao.findAll().get(0).getRepeatRule());
    }

    @Test
    @DisplayName("删除关联待办后，日程保留且 todo_id 被置空")
    void deletingTodoNullsScheduleTodoId() {
        todoDao.insert("被引用的任务", 1, 1);
        long todoId = todoDao.findAll().get(0).getId();

        dao.insert("关联该任务的日程", "2026-10-04 09:00", "2026-10-04 10:00", "none", 5, (int) todoId, null);
        assertNotNull(dao.findAll().get(0).getTodoId());

        todoDao.delete(todoId);

        List<ScheduleEvent> list = dao.findAll();
        assertEquals(1, list.size(), "删除待办不应连带删除日程");
        assertNull(list.get(0).getTodoId(), "todo_id 应被置空");
    }

    @Test
    @DisplayName("TC-4.7 跨天日程的起止时间原样存取，不被截断")
    void overnightEventRoundTrip() {
        dao.insert("跨天学习", "2026-10-05 23:00", "2026-10-06 01:00", "none", 5, null, null);

        ScheduleEvent e = dao.findAll().get(0);
        assertEquals("2026-10-05 23:00", e.getStartAt());
        assertEquals("2026-10-06 01:00", e.getEndAt());
    }
}