package csu.mengya;

import javafx.application.Application;

/**
 * 启动入口。
 *
 * <p>故意不继承 Application：主类若直接继承，JavaFX 启动时会先检查 module-path，
 * 在 classpath 方式下会误报「JavaFX runtime components are missing」。
 * 用一个普通类做入口再显式 launch，即可绕开这个检查。
 *
 * @author 唐天乐
 * @since V1.0
 */
public class Launcher {

    public static void main(String[] args) {
        Application.launch(App.class, args);
    }
}
