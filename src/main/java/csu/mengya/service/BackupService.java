package csu.mengya.service;

import csu.mengya.common.DataRestoredEvent;
import csu.mengya.common.EventBus;
import csu.mengya.common.Result;
import csu.mengya.db.DBManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 数据备份与恢复服务（F6 系统与设置）。
 *
 * <p>对应验收用例 TC-6.5（导出数据）与 TC-6.6（导入备份）。</p>
 *
 * <p><b>导出为什么用 {@code VACUUM INTO} 而不是直接复制文件：</b>
 * 程序运行时 SQLite 连接是打开的，此时直接复制 .db 文件可能拷到
 * 「数据页已改、日志尚未合并」的中间状态，导出的备份可能是坏的。
 * {@code VACUUM INTO} 会在一个只读事务里生成一份一致的副本，
 * 且不需要关闭连接，导出时计时和提醒都能继续跑。</p>
 *
 * <p><b>导入为什么必须先关闭连接：</b>Windows 下文件被占用时无法覆盖，
 * 而且连接里可能还缓存着旧库的页。因此导入流程是
 * 「关闭连接 → 校验并替换文件 → 重新打开」。</p>
 *
 * @author 侯卓轩
 * @since V1.0
 */
public final class BackupService {

    /** SQLite 数据库文件头，用于识别用户是否选错了文件 */
    private static final String SQLITE_MAGIC = "SQLite format 3";

    /** 备份文件默认名用的时间戳格式 */
    private static final DateTimeFormatter FILE_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private BackupService() {
    }

    /**
     * 生成默认的备份文件名，形如 {@code pmato-backup-20261008-143000.db}。
     *
     * @return 建议的文件名
     */
    public static String defaultFileName() {
        return "pmato-backup-" + LocalDateTime.now().format(FILE_STAMP) + ".db";
    }

    /**
     * 导出数据库到指定文件。
     *
     * @param target 目标文件（由文件选择器选定）
     * @return 成功时 {@code data} 为提示文案；失败时携带原因
     */
    public static Result<String> exportTo(File target) {
        if (target == null) {
            return Result.fail("未选择保存位置");
        }

        try {
            Path destination = target.toPath().toAbsolutePath();
            // VACUUM INTO 要求目标文件不存在，覆盖导出前先删掉旧的
            Files.deleteIfExists(destination);

            try (Statement statement = DBManager.getInstance().getConnection().createStatement()) {
                // 路径里的单引号需要转义，否则 SQL 会被截断
                String escaped = destination.toString().replace("'", "''");
                statement.execute("VACUUM INTO '" + escaped + "'");
            }
            return Result.ok("已导出到 " + target.getName());
        } catch (SQLException | IOException e) {
            return Result.fail("导出失败：" + e.getMessage());
        }
    }

    /**
     * 从备份文件恢复数据库。
     *
     * <p>恢复流程刻意分成「先写临时文件、再替换」两步：
     * 如果直接往正式库文件上拷贝而中途失败，用户会得到一个半坏的数据库；
     * 先写临时文件则失败时原库仍是完整的。</p>
     *
     * @param source 备份文件
     * @return 成功时 {@code data} 为提示文案；失败时携带原因
     */
    public static Result<String> importFrom(File source) {
        if (source == null) {
            return Result.fail("未选择备份文件");
        }

        Path backup = source.toPath();
        if (!Files.isRegularFile(backup)) {
            return Result.fail("备份文件不存在");
        }
        // 用户可能误选了图片、文档等无关文件，先认一下文件头再动手
        if (!looksLikeSqlite(backup)) {
            return Result.fail("这不是有效的备份文件（不是 SQLite 数据库）");
        }

        DBManager db = DBManager.getInstance();
        Path database = db.databaseFile();
        Path staging = null;

        try {
            // 1. 先把备份写进同目录的临时文件
            staging = Files.createTempFile(database.getParent(), "pmato-restore-", ".db");
            Files.copy(backup, staging, StandardCopyOption.REPLACE_EXISTING);

            // 2. 关闭连接后才能覆盖正式库文件（Windows 下文件被占用会失败）
            db.close();
            Files.move(staging, database, StandardCopyOption.REPLACE_EXISTING);
            staging = null;

            // 3. 重新打开；open() 内会执行建表与迁移，老版本备份也能自动补齐表结构
            db.getConnection();
        } catch (IOException e) {
            db.getConnection();     // 尽量把连接恢复回来，别让程序卡在无库状态
            return Result.fail("导入失败：" + e.getMessage());
        } finally {
            if (staging != null) {
                try {
                    Files.deleteIfExists(staging);
                } catch (IOException ignored) {
                    // 临时文件清理失败不影响主流程
                }
            }
        }

        // 通知主框架清掉页面缓存并重新加载，否则界面还停留在旧数据上
        EventBus.publish(new DataRestoredEvent(source.getName()));
        return Result.ok("已从 " + source.getName() + " 恢复");
    }

    /**
     * 判断文件是否为 SQLite 数据库。
     *
     * <p>SQLite 文件固定以 16 字节的 {@code "SQLite format 3\0"} 开头，
     * 用它做一次廉价校验，避免用户选错文件后把正式库覆盖成垃圾。</p>
     *
     * @param file 待检查的文件
     * @return 是 SQLite 数据库返回 true
     */
    private static boolean looksLikeSqlite(Path file) {
        try (InputStream in = Files.newInputStream(file)) {
            byte[] header = new byte[16];
            if (in.read(header) != 16) {
                return false;
            }
            return new String(header, StandardCharsets.US_ASCII).startsWith(SQLITE_MAGIC);
        } catch (IOException e) {
            return false;
        }
    }
}
