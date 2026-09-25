package csu.mengya.db;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;

/**
 * 数据库管理器（F0 共享地基，【临时实现，待组长 A 定稿对齐】）。
 *
 * <p>职责：持有全局唯一的 SQLite 连接，首次启动时执行建表脚本。</p>
 *
 * <p>依据计划书 2.2 节第④条：本地单用户程序不做连接池，由一个单例持有连接；
 * 写操作串行执行。当前版本 DAO 层用 synchronized 保证串行，后续 D4 起可改为
 * 单线程执行器 + 后台线程，本类接口保持不变。</p>
 *
 * @author B（临时搭建，组长 A 定稿后接管）
 * @since V1.0
 */
public final class DBManager {

    /** SQLite 连接串，数据库文件生成在程序运行目录下（mengya.db） */
    private static final String DB_URL = "jdbc:sqlite:mengya.db";

    /** 单例实例 */
    private static final DBManager INSTANCE = new DBManager();

    /** 全局唯一连接 */
    private Connection connection;

    /** 私有构造，保证单例 */
    private DBManager() {
    }

    /**
     * 获取全局唯一实例。
     *
     * @return DBManager 单例
     */
    public static DBManager getInstance() {
        return INSTANCE;
    }

    /**
     * 获取数据库连接；首次调用时自动打开并建表。
     *
     * @return 全局唯一连接
     */
    public synchronized Connection getConnection() {
        if (connection == null) {
            open();
        }
        return connection;
    }

    /** 打开连接并执行建表脚本 */
    private void open() {
        try {
            connection = DriverManager.getConnection(DB_URL);
            executeSchema(connection);
        } catch (SQLException e) {
            // 数据库初始化失败属于致命错误，直接抛出，由 Bootstrap 让程序提示后退出
            throw new RuntimeException("数据库初始化失败", e);
        }
    }

    /** 读取 schema.sql 并逐条执行建表语句 */
    private void executeSchema(Connection conn) {
        String sql = readSchema();
        for (String stmt : sql.split(";")) {
            if (stmt.trim().isEmpty()) {
                continue;
            }
            try (Statement s = conn.createStatement()) {
                s.execute(stmt);
            } catch (SQLException e) {
                throw new RuntimeException("执行建表脚本失败", e);
            }
        }
    }

    /** 从 classpath 读取 /db/schema.sql 全文（去除注释与空行） */
    private String readSchema() {
        try (InputStream in = DBManager.class.getResourceAsStream("/db/schema.sql")) {
            if (in == null) {
                throw new IllegalStateException("找不到建表脚本 /db/schema.sql");
            }
            StringBuilder sb = new StringBuilder();
            try (Scanner sc = new Scanner(in, StandardCharsets.UTF_8.name())) {
                while (sc.hasNextLine()) {
                    String line = sc.nextLine().trim();
                    if (line.isEmpty() || line.startsWith("--")) {
                        continue;
                    }
                    sb.append(line).append(' ');
                }
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("读取建表脚本失败", e);
        }
    }
}
