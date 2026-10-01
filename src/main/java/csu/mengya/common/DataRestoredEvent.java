package csu.mengya.common;

/**
 * 数据恢复完成事件（F6）。
 *
 * <p>由 {@code BackupService} 在导入备份成功后发布，主框架收到后清空页面缓存
 * 并重新加载当前页面 —— 否则界面上仍是导入前的旧数据。</p>
 *
 * @param backupFileName 被导入的备份文件名，用于提示文案
 * @author 侯卓轩
 * @since V1.0
 */
public record DataRestoredEvent(String backupFileName) {
}
