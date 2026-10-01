package csu.mengya.service;

import csu.mengya.common.ThemeManager;

import java.util.prefs.Preferences;

/**
 * F6 本地设置。Java Preferences 持久化时长、免打扰与主题选项。
 *
 * <p>存的是用户偏好而非业务数据，所以放在 Preferences（Windows 下即注册表
 * 用户配置区），不占 SQLite 库；这样即使用户删库重建，设置仍然保留。</p>
 *
 * @author 郭艾迪
 * @since V1.0
 */
public final class SettingsService {
    private static final SettingsService INSTANCE = new SettingsService();
    private final Preferences prefs = Preferences.userNodeForPackage(SettingsService.class);

    private SettingsService() {}
    public static SettingsService getInstance() { return INSTANCE; }

    public int focusMinutes() { return bounded("focusMinutes", 25, 1, 180); }
    public int shortBreakMinutes() { return bounded("shortBreakMinutes", 5, 1, 60); }
    public int longBreakMinutes() { return bounded("longBreakMinutes", 15, 1, 120); }
    public boolean doNotDisturb() { return prefs.getBoolean("doNotDisturb", false); }

    /**
     * 当前主题。
     *
     * @return {@link ThemeManager#LIGHT} 或 {@link ThemeManager#DARK}，默认浅色
     */
    public String theme() {
        return ThemeManager.DARK.equals(prefs.get("theme", ThemeManager.LIGHT))
                ? ThemeManager.DARK : ThemeManager.LIGHT;
    }

    /**
     * 保存主题选择。主题由 {@link ThemeManager} 立即生效，这里只负责持久化。
     *
     * @param theme 主题标识；非法值按浅色保存
     */
    public void saveTheme(String theme) {
        prefs.put("theme", ThemeManager.DARK.equals(theme) ? ThemeManager.DARK : ThemeManager.LIGHT);
    }

    public void save(int focus, int shortBreak, int longBreak, boolean dnd) {
        prefs.putInt("focusMinutes", Math.max(1, Math.min(180, focus)));
        prefs.putInt("shortBreakMinutes", Math.max(1, Math.min(60, shortBreak)));
        prefs.putInt("longBreakMinutes", Math.max(1, Math.min(120, longBreak)));
        prefs.putBoolean("doNotDisturb", dnd);
    }

    private int bounded(String key, int fallback, int min, int max) {
        return Math.max(min, Math.min(max, prefs.getInt(key, fallback)));
    }
}