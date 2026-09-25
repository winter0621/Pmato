package csu.mengya.service;

import csu.mengya.dao.FocusSessionDao;
import csu.mengya.dao.UserProfileDao;
import csu.mengya.model.FocusSession;

import java.util.Map;
import java.util.TreeMap;

/**
 * 数据统计服务（F5 数据统计）。
 *
 * <p>从专注会话表聚合出：番茄数、学习时长、按天趋势、作物收集进度、连续打卡。
 * 图表数据为空时由界面层展示空状态，本层返回空集合而非 null。</p>
 *
 * @author B
 * @since V1.0
 */
public final class StatsService {

    private final FocusSessionDao sessionDao = new FocusSessionDao();

    /** 累计完成番茄数 */
    public int totalPomodoro() {
        return sessionDao.findCompletedFocus().size();
    }

    /** 累计学习时长（分钟） */
    public int totalMinutes() {
        int sum = 0;
        for (FocusSession s : sessionDao.findCompletedFocus()) {
            sum += s.getActualMin();
        }
        return sum;
    }

    /** 按天聚合的学习时长（分钟），日期升序 */
    public Map<String, Integer> minutesByDay() {
        Map<String, Integer> map = new TreeMap<>();
        for (FocusSession s : sessionDao.findCompletedFocus()) {
            map.merge(dayOf(s.getStartAt()), s.getActualMin(), Integer::sum);
        }
        return map;
    }

    /** 按天聚合的番茄数，日期升序 */
    public Map<String, Integer> pomodoroByDay() {
        Map<String, Integer> map = new TreeMap<>();
        for (FocusSession s : sessionDao.findCompletedFocus()) {
            map.merge(dayOf(s.getStartAt()), 1, Integer::sum);
        }
        return map;
    }

    /** 已收集物种数 */
    public int collectedSpeciesCount() {
        return GardenService.getInstance().collectedSpeciesIds().size();
    }

    /** 图鉴物种总数 */
    public int totalSpeciesCount() {
        return GardenService.getInstance().allSpecies().size();
    }

    /** 连续打卡天数（当前直接读 user_profile，后续 D9 再实现完整打卡判定） */
    public int streakDays() {
        return new UserProfileDao().getOrCreate().getStreakDays();
    }

    /** 从 ISO8601 时间串提取日期部分（yyyy-MM-dd） */
    private String dayOf(String iso) {
        if (iso == null || iso.length() < 10) {
            return "未知";
        }
        return iso.substring(0, 10);
    }
}
