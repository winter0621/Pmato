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
 * 系统托盘服务。
 *
 * <p>JavaFX 没有托盘 API，这里用 AWT 的 SystemTray 实现。两者可同进程共存，
 * 但 AWT 的回调跑在 AWT 线程上，所以都要经 Platform.runLater 切回 JavaFX 线程。
 *
 * @author 侯卓轩
 * @since V1.0
 */
public final class TrayService {

    private static TrayIcon trayIcon;

    private static Stage mainStage;

    /** 未装托盘的环境，关闭窗口应保持「直接退出」 */
    private static boolean installed;

    private TrayService() {
    }

    /**
     * 安装系统托盘。
     *
     * @param stage 主窗口
     * @return 环境不支持托盘时返回 false，调用方应保持默认关闭行为
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

            trayIcon = new TrayIcon(createTrayImage(), "Pmato植物园激励式学习软件", menu);
            trayIcon.setImageAutoSize(true);
            trayIcon.addActionListener(e -> Platform.runLater(TrayService::showWindow));

            tray.add(trayIcon);
            installed = true;
            return true;
        } catch (AWTException | RuntimeException e) {
            // 托盘不可用属于环境限制，静默退回默认行为
            return false;
        }
    }

    public static boolean isInstalled() {
        return installed;
    }

    public static void showWindow() {
        if (mainStage == null) {
            return;
        }
        mainStage.show();
        mainStage.setIconified(false);
        mainStage.toFront();
        mainStage.requestFocus();
    }

    public static void hideWindow() {
        if (mainStage != null) {
            mainStage.hide();
        }
    }

    /**
     * 弹托盘气泡通知；免打扰开启时不弹。
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

    /** 退出程序；先摘掉托盘图标，否则图标会残留在托盘区 */
    public static void exit() {
        if (trayIcon != null) {
            SystemTray.getSystemTray().remove(trayIcon);
            trayIcon = null;
        }
        installed = false;
        Platform.exit();
        System.exit(0);
    }

    /** 切到专注页；发事件而不是持有控制器引用，托盘不该依赖界面层 */
    private static void openFocusPage() {
        showWindow();
        EventBus.publish(new FocusRequestedEvent(null));
    }

    /** 程序化绘制托盘图标，避免额外依赖图片资源 */
    private static Image createTrayImage() {
        int size = 32;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        g.setColor(new Color(0x16, 0xA3, 0x4A));
        g.fill(new Ellipse2D.Float(1f, 1f, size - 2f, size - 2f));

        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Float(size / 2f, size * 0.74f, size / 2f, size * 0.46f));

        g.fill(new Ellipse2D.Float(size * 0.18f, size * 0.30f, size * 0.30f, size * 0.20f));
        g.fill(new Ellipse2D.Float(size * 0.52f, size * 0.30f, size * 0.30f, size * 0.20f));

        g.dispose();
        return image;
    }
}
