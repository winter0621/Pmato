# pmato —— 植物园式学习计时软件 V1.0

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
跑出一个标题为「pmato」的窗口即算 D1 验收通过。

## 学习助手

侧边栏的「学习助手」可拆解待办、规划一周学习安排，或回答学习问题。使用时在页面输入自己的 OpenAI API Key；也可以在启动应用前设置 `OPENAI_API_KEY` 环境变量。默认模型为 `gpt-4.1-mini`。

密钥不会写入数据库或配置文件。仅在点击生成时请求 OpenAI；勾选「附上待办和日程摘要」时，本次请求会附带最多 8 条未完成待办及最多 8 条日程摘要。生成内容是建议，不会自动更改待办、日程或计时记录。使用 API 可能产生账户费用。

## 目录结构

```
pmato/
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

## 模块联动

1. 在 F3 待办清单中创建任务，选中后点击「开始专注」；也可以在 F4 日程中关联待办，选择日程后进入专注页。
2. F1 专注页会带入关联任务。完成一次专注后，会话写入 SQLite，按连续番茄数和任务加成结算能量；关联待办的番茄进度自动增加。
3. F2 植物园接收专注能量并推动作物生长。切换到 F5 统计页可查看专注时长、番茄数、连续打卡和收集进度。
4. F4 到点提醒可直接跳到 F1。F6 可调整下一次专注与休息的时长，并设置免打扰。

计时使用单调时钟反算剩余时间，切换页面和最小化窗口后继续运行。暂停时间不计入专注；异常退出留下的运行中会话在下次启动时标记为中断，不结算能量。