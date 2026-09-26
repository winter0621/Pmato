package csu.mengya.service;

import java.util.prefs.Preferences;

/** F6 本地设置。Java Preferences 持久化时长与免打扰选项。 */
public final class SettingsService {
    private static final SettingsService INSTANCE = new SettingsService();
    private final Preferences prefs = Preferences.userNodeForPackage(SettingsService.class);

    private SettingsService() {}
    public static SettingsService getInstance() { return INSTANCE; }

    public int focusMinutes() { return bounded("focusMinutes", 25, 1, 180); }
    public int shortBreakMinutes() { return bounded("shortBreakMinutes", 5, 1, 60); }
    public int longBreakMinutes() { return bounded("longBreakMinutes", 15, 1, 120); }
    public boolean doNotDisturb() { return prefs.getBoolean("doNotDisturb", false); }

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