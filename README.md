# 萌芽专注 —— 植物园式学习计时软件 V1.0

> Day1 工程骨架：验证「环境就绪 + JavaFX 空窗口可运行」。
> 对应《项目分工计划书》里程碑 M1（D1 结束）。

## 环境要求

| 项 | 版本 |
| --- | --- |
| JDK | 17 或 21（LTS），本机已装 Temurin 17 |
| Maven | 3.8+ |
| IDE | IntelliJ IDEA 社区版 |

## 一键运行

```bash
mvn javafx:run
```

首次运行会从中央仓库下载 JavaFX 21 依赖，稍等即可。
跑出一个标题为「萌芽专注」的窗口即算 D1 验收通过。

## 目录结构

```
mengya-focus/
├── pom.xml                              # Maven 配置（JavaFX 21 + 启动插件）
├── src/main/java/csu/mengya/
│   ├── Launcher.java                    # 启动入口（不继承 Application）
│   ├── App.java                         # JavaFX Application 主类
│   └── controller/MainController.java   # 主界面控制器
└── src/main/resources/
    ├── fxml/main.fxml                   # 主界面布局
    └── css/theme.css                    # 全局主题样式（颜色集中定义）
```

## 注意事项

- **不要**去官网下载 JavaFX SDK 配 module-path，本项目走 Maven 依赖 + classpath 方式。
- 源码统一 UTF-8；Windows 平台若中文乱码，确认 `pom.xml` 里 `project.build.sourceEncoding=UTF-8`。
- 提交代码时遵循 `.gitignore`，不提交 `target/`、`*.db`、`.idea/`。
