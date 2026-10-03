package csu.mengya.common;

/** 数据恢复完成事件（F6）：导入备份成功后发布，主框架据此清空缓存并重载页面。 */
public record DataRestoredEvent(String backupFileName) {
}
