-- ============================================================
-- 萌芽专注 数据库建表脚本（V1.0）
-- 依据《项目分工计划书》5.2 节，共 6 张表。
-- 说明：单文件 SQLite，随程序目录走，由 DBManager 在首次启动时自动执行。
--      加 IF NOT EXISTS 保证重复启动不报错（对应验收用例 TC-6.7）。
-- ============================================================

-- 用户档案与设置
CREATE TABLE IF NOT EXISTS user_profile (
  id            INTEGER PRIMARY KEY,
  nickname      TEXT    NOT NULL DEFAULT '学习者',
  total_energy  INTEGER NOT NULL DEFAULT 0,
  streak_days   INTEGER NOT NULL DEFAULT 0,
  created_at    TEXT    NOT NULL
);

-- 专注会话（统计模块的数据源）
CREATE TABLE IF NOT EXISTS focus_session (
  id            INTEGER PRIMARY KEY AUTOINCREMENT,
  start_at      TEXT    NOT NULL,
  end_at        TEXT,
  planned_min   INTEGER NOT NULL,
  actual_min    INTEGER NOT NULL DEFAULT 0,
  type          TEXT    NOT NULL,
  status        TEXT    NOT NULL,
  todo_id       INTEGER,
  energy_gained INTEGER NOT NULL DEFAULT 0,
  FOREIGN KEY (todo_id) REFERENCES todo_item(id) ON DELETE SET NULL
);

-- 作物图鉴（静态配置，随软件内置）
CREATE TABLE IF NOT EXISTS plant_species (
  id            TEXT    PRIMARY KEY,
  name          TEXT    NOT NULL,
  rarity        TEXT    NOT NULL,
  unlock_energy INTEGER NOT NULL,
  stage_scale   REAL    NOT NULL DEFAULT 1.0
);

-- 植物园地块
CREATE TABLE IF NOT EXISTS garden_plot (
  id            INTEGER PRIMARY KEY AUTOINCREMENT,
  species_id    TEXT    NOT NULL,
  slot_index    INTEGER NOT NULL,
  planted_at    TEXT    NOT NULL,
  energy        INTEGER NOT NULL DEFAULT 0,
  stage         INTEGER NOT NULL DEFAULT 0,
  status        TEXT    NOT NULL,
  FOREIGN KEY (species_id) REFERENCES plant_species(id)
);

-- 待办事项
CREATE TABLE IF NOT EXISTS todo_item (
  id             INTEGER PRIMARY KEY AUTOINCREMENT,
  title          TEXT    NOT NULL,
  note           TEXT,
  priority       INTEGER NOT NULL DEFAULT 1,
  due_date       TEXT,
  est_pomodoro   INTEGER NOT NULL DEFAULT 1,
  done_pomodoro  INTEGER NOT NULL DEFAULT 0,
  status         TEXT    NOT NULL DEFAULT 'todo',
  created_at     TEXT    NOT NULL,
  completed_at   TEXT
);

-- 日程事件
CREATE TABLE IF NOT EXISTS schedule_event (
  id            INTEGER PRIMARY KEY AUTOINCREMENT,
  title         TEXT    NOT NULL,
  start_at      TEXT    NOT NULL,
  end_at        TEXT    NOT NULL,
  repeat_rule   TEXT    NOT NULL DEFAULT 'none',
  remind_before INTEGER NOT NULL DEFAULT 5,
  todo_id       INTEGER,
  color         TEXT,
  FOREIGN KEY (todo_id) REFERENCES todo_item(id) ON DELETE SET NULL
);
