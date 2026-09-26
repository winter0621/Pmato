package csu.mengya.service;

import csu.mengya.dao.FocusSessionDao;
import csu.mengya.model.FocusSession;
import csu.mengya.model.PlantSpecies;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * 数据统计服务（F5 数据统计）。
 *
 * <p>从专注会话表聚合出：番茄数、学习时长、按天趋势、作物收集进度、连续打卡。
 * 图表数据为空时由界面层展示空状态，本层返回空集合而非 null。</p>
 *
 * @author 唐天乐
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

    /** 收集册数据：全部物种（含收集状态），供统计页收集册网格展示 */
    public List<PlantSpecies> speciesAlbum() {
        return GardenService.getInstance().allSpecies();
    }

    /**
     * 连续打卡天数：从专注会话反推，统计「连续有专注记录的天数」。
     * 今天还没记录时从昨天起算（当天未结束不算断签），断一天即归零。
     */
    public int streakDays() {
        Set<LocalDate> days = new HashSet<>();
        for (FocusSession s : sessionDao.findCompletedFocus()) {
            String d = dayOf(s.getStartAt());
            if (d.length() == 10) {
                days.add(LocalDate.parse(d));
            }
        }
        if (days.isEmpty()) {
            return 0;
        }
        LocalDate today = LocalDate.now();
        LocalDate cursor = days.contains(today) ? today : today.minusDays(1);
        int streak = 0;
        while (days.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    /** 从 ISO8601 时间串提取日期部分（yyyy-MM-dd） */
    private String dayOf(String iso) {
        if (iso == null || iso.length() < 10) {
            return "未知";
        }
        return iso.substring(0, 10);
    }
}
