package csu.mengya;

import javafx.application.Application;

/**
 * 启动入口类（F0 工程骨架）。
 *
 * <p>作用：提供一个「不继承 {@link Application}」的 main 入口，规避
 * JavaFX 在非模块化（classpath）运行时的启动报错
 * {@code Error: JavaFX runtime components are missing}。</p>
 *
 * <p>原因说明：若 main 方法所在的类直接继承 {@link Application}，启动时
 * 会先触发 JavaFX 对 module-path 的检查；在未配置 module-path 的 classpath
 * 场景下会被误判为「缺组件」。用一个普通类做入口、再显式调用
 * {@link Application#launch(Class, String...)} 即可绕开该检查。</p>
 *
 * <p>运行方式：{@code mvn javafx:run}（由 javafx-maven-plugin 调用本类）。</p>
 *
 * @author 郭艾迪
 * @since V1.0
 */
public class Launcher {

    /**
     * 程序主入口。
     *
     * @param args 命令行参数，本程序当前不使用
     */
    public static void main(String[] args) {
        // 委托给 JavaFX 启动真正的 Application 主类 App
        Application.launch(App.class, args);
    }
}
