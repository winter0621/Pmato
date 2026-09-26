package csu.mengya.db;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
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

    /** 默认 SQLite 连接串，数据库文件生成在程序运行目录下（mengya.db） */
    private static final String DEFAULT_DB_URL = "jdbc:sqlite:mengya.db";

    /** 覆盖连接串的系统属性名；仅供测试使用，正式运行不设置 */
    private static final String DB_URL_PROPERTY = "pmato.db.url";

    /**
     * 解析当前应使用的数据库连接串。
     *
     * <p>优先读取系统属性 {@code pmato.db.url}，没有时回退到默认值。
     * 这个开关是为单元测试准备的：测试在 {@code @BeforeAll} 里把连接串指向
     * 临时文件，就能在不碰开发数据库的前提下跑 DAO 测试。</p>
     *
     * @return 完整的 JDBC 连接串
     */
    private String resolveDbUrl() {
        String override = System.getProperty(DB_URL_PROPERTY);
        if (override != null && !override.isBlank()) {
            return override;
        }
        return DEFAULT_DB_URL;
    }

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

    /**
     * 关闭当前连接并置空。
     *
     * <p>调用后下一次 {@link #getConnection()} 会按当时的配置重新打开数据库。
     * 单元测试用它从开发库切到临时库；程序退出时也可用于清理。</p>
     */
    public synchronized void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                // 关闭失败不影响后续流程，忽略
            }
            connection = null;
        }
    }

    /** 打开连接并执行建表脚本 */
    private void open() {
        try {
            connection = DriverManager.getConnection(resolveDbUrl());
            try (Statement pragma = connection.createStatement()) {
                pragma.execute("PRAGMA foreign_keys = ON");
            }
            executeSchema(connection);
            migrate(connection);
            ensureDefaultProfile(connection);
        } catch (SQLException e) {
            // 数据库初始化失败属于致命错误，直接抛出，由 Bootstrap 让程序提示后退出
            throw new RuntimeException("数据库初始化失败", e);
        }
    }

    /**
     * 轻量迁移：老库缺列时补列，避免「直接删库重建」丢用户数据（计划书 5.2 迁移策略）。
     */
    private void migrate(Connection conn) {
        // V1.1：plant_species 增加 collected 列，标记物种是否已收集
        if (!hasColumn(conn, "plant_species", "collected")) {
            try (Statement s = conn.createStatement()) {
                s.execute("ALTER TABLE plant_species ADD COLUMN collected INTEGER NOT NULL DEFAULT 0");
            } catch (SQLException e) {
                throw new RuntimeException("迁移 plant_species 失败", e);
            }
        }
        // V1.2：plant_species 增加 unlocked 列（解锁状态）；普通作物（门槛 0）默认已解锁
        if (!hasColumn(conn, "plant_species", "unlocked")) {
            try (Statement s = conn.createStatement()) {
                s.execute("ALTER TABLE plant_species ADD COLUMN unlocked INTEGER NOT NULL DEFAULT 0");
                s.execute("UPDATE plant_species SET unlocked = 1 WHERE unlock_energy = 0");
            } catch (SQLException e) {
                throw new RuntimeException("迁移 plant_species 失败", e);
            }
        }
    }

    /** 判断某表是否存在指定列 */
    private boolean hasColumn(Connection conn, String table, String column) {
        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rs.next()) {
                if (column.equals(rs.getString("name"))) {
                    return true;
                }
            }
        } catch (SQLException e) {
            // 查询失败按「列不存在」处理
        }
        return false;
    }

    /** 首次启动创建默认用户档案，避免统计页读取不到 id=1。 */
    private void ensureDefaultProfile(Connection conn) {
        String sql = "INSERT OR IGNORE INTO user_profile (id, nickname, total_energy, streak_days, created_at) "
                + "VALUES (1, '学习者', 0, 0, datetime('now', 'localtime'))";
        try (Statement statement = conn.createStatement()) {
            statement.executeUpdate(sql);
        } catch (SQLException e) {
            throw new RuntimeException("初始化默认用户失败", e);
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
