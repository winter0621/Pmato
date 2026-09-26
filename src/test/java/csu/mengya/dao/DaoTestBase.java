package csu.mengya.dao;

import csu.mengya.db.DBManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * DAO 测试基类。
 *
 * <p>核心职责：把 {@link DBManager} 的连接指向临时数据库文件，
 * 避免测试污染开发用的 mengya.db（依赖 DBManager 的 pmato.db.url 开关）。</p>
 *
 * <p><b>三步顺序不能变</b>：先设置系统属性 → 再关闭可能已打开的开发库连接
 * → 最后触发 getConnection() 让新库建表。顺序错了测试会写到开发库上。</p>
 *
 * <p>建库是 {@code @BeforeAll}（每类一次），清表是 {@code @BeforeEach}（每方法一次）。
 * 两者不能合并成 {@code @BeforeAll}：那样同一个测试类里前一个用例插入的数据会残留，
 * 用例中 {@code findAll().get(0)} 拿到的就是上一个用例的行，出现「断言的是别的对象」
 * 这类与被测逻辑无关的失败。</p>
 *
 * @author 侯卓轩
 * @since V1.0
 */
abstract class DaoTestBase {

    /** 本次测试使用的临时数据库路径，@AfterAll 时删除 */
    private static Path tempDb;

    @BeforeAll
    static void initTempDatabase() throws Exception {
        // SQLite 会自动创建文件，这里先建后删，确保拿到一个不存在的干净路径
        tempDb = Files.createTempFile("pmato-test-", ".db");
        Files.deleteIfExists(tempDb);

        System.setProperty("pmato.db.url", "jdbc:sqlite:" + tempDb.toAbsolutePath());
        DBManager.getInstance().close();         // 丢弃开发库连接
        DBManager.getInstance().getConnection(); // 按临时路径建库 + 建表 + 插入默认档案
    }

    /**
     * 每个测试方法前清空业务表，保证用例之间互不干扰。
     *
     * <p>只清业务数据，{@code plant_species} 与 {@code user_profile} 必须保留：
     * 前者是 {@code PlantSpeciesDao} 在表空时才写入的内置图鉴，后者是
     * {@code DBManager.open()} 里 {@code INSERT OR IGNORE} 建的默认档案，
     * 清掉之后本次运行内不会再补，统计类用例会读不到 id=1。</p>
     *
     * <p>删除顺序：先子表后父表。{@code focus_session} 与 {@code schedule_event}
     * 都有指向 {@code todo_item} 的外键，先删待办会触发 SET NULL 的空转。</p>
     */
    @BeforeEach
    void resetBusinessTables() throws SQLException {
        try (Statement st = DBManager.getInstance().getConnection().createStatement()) {
            st.executeUpdate("DELETE FROM garden_plot");
            st.executeUpdate("DELETE FROM focus_session");
            st.executeUpdate("DELETE FROM schedule_event");
            st.executeUpdate("DELETE FROM todo_item");
        }
    }

    @AfterAll
    static void dropTempDatabase() throws Exception {
        DBManager.getInstance().close();
        System.clearProperty("pmato.db.url");
        if (tempDb != null) {
            Files.deleteIfExists(tempDb);
        }
    }
}
