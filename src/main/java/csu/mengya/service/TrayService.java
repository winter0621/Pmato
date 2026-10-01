package csu.mengya.service;

import csu.mengya.common.EventBus;
import csu.mengya.common.FocusRequestedEvent;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.awt.AWTException;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.RenderingHints;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;

/**
 * 系统托盘服务（F6 系统与设置）。
 *
 * <p>提供三件事（对应验收用例 TC-6.1、TC-6.2）：</p>
 * <ol>
 *   <li>程序常驻系统托盘，关闭主窗口时隐藏而不退出；</li>
 *   <li>托盘图标右键菜单：显示主窗口 / 开始专注 / 退出；</li>
 *   <li>托盘气泡通知 {@link #notifyMessage}，供计时结束等场景调用。</li>
 * </ol>
 *
 * <p><b>为什么用 AWT 而不是 JavaFX：</b>JavaFX 至今没有提供系统托盘 API，
 * 官方推荐的方案就是调用 AWT 的 {@link SystemTray}，两者可以在同一进程共存。</p>
 *
 * <p><b>线程约定：</b>AWT 菜单项的点击回调运行在 AWT 事件线程上，
 * 而 JavaFX 控件只能在 JavaFX 应用线程上操作。因此本类所有回调都先经
 * {@link Platform#runLater} 切回 JavaFX 线程再执行，避免跨线程操作界面。</p>
 *
 * @author 侯卓轩
 * @since V1.0
 */
public final class TrayService {

    /** 托盘图标；安装成功后持有，退出时用于移除 */
    private static TrayIcon trayIcon;

    /** 主窗口引用，用于显示 / 隐藏 */
    private static Stage mainStage;

    /** 是否已成功安装托盘；未安装时，关闭窗口应保持「直接退出」的默认行为 */
    private static boolean installed;

    private TrayService() {
    }

    /**
     * 安装系统托盘。
     *
     * <p>若运行环境不支持托盘（部分 Linux 发行版、无桌面环境），返回 false，
     * 调用方应保持默认的「关闭窗口即退出」行为，不要吞掉关闭事件。</p>
     *
     * @param stage 主窗口
     * @return 安装成功返回 true
     */
    public static boolean install(Stage stage) {
        mainStage = stage;

        if (!SystemTray.isSupported()) {
            return false;
        }

        try {
            SystemTray tray = SystemTray.getSystemTray();
            PopupMenu menu = new PopupMenu();

            MenuItem showItem = new MenuItem("显示主窗口");
            showItem.addActionListener(e -> Platform.runLater(TrayService::showWindow));
            menu.add(showItem);

            MenuItem focusItem = new MenuItem("开始专注");
            focusItem.addActionListener(e -> Platform.runLater(TrayService::openFocusPage));
            menu.add(focusItem);

            menu.addSeparator();

            MenuItem exitItem = new MenuItem("退出");
            exitItem.addActionListener(e -> Platform.runLater(TrayService::exit));
            menu.add(exitItem);

            trayIcon = new TrayIcon(createTrayImage(), "萌芽专注", menu);
            trayIcon.setImageAutoSize(true);
            // 左键单击图标等同于「显示主窗口」
            trayIcon.addActionListener(e -> Platform.runLater(TrayService::showWindow));

            tray.add(trayIcon);
            installed = true;
            return true;
        } catch (AWTException | RuntimeException e) {
            // 托盘不可用属于环境限制，不应影响主流程，静默退回默认行为
            return false;
        }
    }

    /**
     * 是否已成功安装托盘。
     *
     * @return 安装成功返回 true
     */
    public static boolean isInstalled() {
        return installed;
    }

    /** 把主窗口显示到前台并取得焦点 */
    public static void showWindow() {
        if (mainStage == null) {
            return;
        }
        mainStage.show();
        mainStage.setIconified(false);
        mainStage.toFront();
        mainStage.requestFocus();
    }

    /** 隐藏主窗口到托盘 */
    public static void hideWindow() {
        if (mainStage != null) {
            mainStage.hide();
        }
    }

    /**
     * 弹出托盘气泡通知。
     *
     * <p>已开启免打扰时不弹（与验收用例 TC-6.4 的语义一致）。</p>
     *
     * @param title 通知标题
     * @param body  通知正文
     */
    public static void notifyMessage(String title, String body) {
        if (!installed || trayIcon == null) {
            return;
        }
        if (SettingsService.getInstance().doNotDisturb()) {
            return;
        }
        trayIcon.displayMessage(title, body, TrayIcon.MessageType.INFO);
    }

    /** 退出程序：先移除托盘图标，再结束 JavaFX 与进程 */
    public static void exit() {
        if (trayIcon != null) {
            SystemTray.getSystemTray().remove(trayIcon);
            trayIcon = null;
        }
        installed = false;
        Platform.exit();
        System.exit(0);
    }

    /**
     * 切换到 F1 专注页。
     *
     * <p>通过发布 {@link FocusRequestedEvent} 通知主框架切页，
     * 而不是直接持有控制器引用 —— 托盘属于系统层，不应依赖界面层。</p>
     */
    private static void openFocusPage() {
        showWindow();
        EventBus.publish(new FocusRequestedEvent(null));
    }

    /**
     * 程序化绘制托盘图标。
     *
     * <p>不读取外部图片文件，避免打包时必须附带图标资源；
     * 图案为品牌绿圆底 + 白色幼苗，在 16×16 的托盘尺寸下仍可辨认。</p>
     *
     * @return 32×32 的托盘图标
     */
    private static Image createTrayImage() {
        int size = 32;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        // 圆底：品牌主色「鼠尾草绿」
        g.setColor(new Color(0x7B, 0xAE, 0x8D));
        g.fill(new Ellipse2D.Float(1f, 1f, size - 2f, size - 2f));

        // 茎：白色竖线
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Float(size / 2f, size * 0.74f, size / 2f, size * 0.46f));

        // 左右两片子叶
        g.fill(new Ellipse2D.Float(size * 0.18f, size * 0.30f, size * 0.30f, size * 0.20f));
        g.fill(new Ellipse2D.Float(size * 0.52f, size * 0.30f, size * 0.30f, size * 0.20f));

        g.dispose();
        return image;
    }
}
