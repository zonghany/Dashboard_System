-- ============================================================
-- AI Task Kanban Dashboard - 数据库初始化脚本
-- 数据库: MySQL
-- 描述: 创建看板系统所需的全部 8 张表及相关索引
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- -----------------------------------------------------------
-- 1. 用户表 - 存储系统用户的基本信息与登录凭证
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id          BIGINT          AUTO_INCREMENT PRIMARY KEY,
    email       VARCHAR(255)    UNIQUE NOT NULL,
    username    VARCHAR(100)    NOT NULL,
    password    VARCHAR(255)    NOT NULL,
    created_at  DATETIME       DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------
-- 2. 项目表 - 存储项目的基本信息，每个项目可拥有独立看板
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS projects (
    id          BIGINT          AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100)    NOT NULL,
    description TEXT,
    created_at  DATETIME       DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------
-- 3. 项目成员表 - 用户与项目的多对多关系，记录成员角色
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS project_members (
    id          BIGINT          AUTO_INCREMENT PRIMARY KEY,
    project_id  BIGINT          NOT NULL,
    user_id     BIGINT          NOT NULL,
    role        VARCHAR(20)     NOT NULL DEFAULT '普通成员',
    joined_at   DATETIME       DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_project_user (project_id, user_id),
    CONSTRAINT fk_pm_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_pm_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------
-- 4. 看板列表 - 每个项目下的任务列（如：待办、进行中、已完成）
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS board_columns (
    id          BIGINT          AUTO_INCREMENT PRIMARY KEY,
    project_id  BIGINT          NOT NULL,
    name        VARCHAR(100)    NOT NULL,
    sort_order  INT             NOT NULL DEFAULT 0,
    created_at  DATETIME       DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_project_name (project_id, name),
    CONSTRAINT fk_bc_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------
-- 5. 任务表 - 看板上的核心任务卡片，支持优先级、到期日、指派人、预估工时与 JSON 元数据
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS tasks (
    id               BIGINT          AUTO_INCREMENT PRIMARY KEY,
    column_id        BIGINT          NOT NULL,
    title            VARCHAR(255)    DEFAULT '新任务',
    description      TEXT,
    priority         VARCHAR(10)     DEFAULT '中',
    due_date         DATETIME,
    assignee_id      BIGINT,
    estimated_hours  DOUBLE,
    sort_order       INT             NOT NULL DEFAULT 0,
    metadata         JSON            DEFAULT (JSON_OBJECT()),
    created_at       DATETIME       DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_task_column FOREIGN KEY (column_id) REFERENCES board_columns(id) ON DELETE CASCADE,
    CONSTRAINT fk_task_assignee FOREIGN KEY (assignee_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------
-- 6. 任务附件表 - 记录任务关联的附件文件信息
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS task_attachments (
    id          BIGINT          AUTO_INCREMENT PRIMARY KEY,
    task_id     BIGINT          NOT NULL,
    file_name   VARCHAR(255)    NOT NULL,
    file_path   VARCHAR(500)    NOT NULL,
    file_type   VARCHAR(50)     NOT NULL,
    file_size   BIGINT          NOT NULL,
    uploaded_at DATETIME       DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ta_task FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------
-- 7. 通知表 - 用户通知消息，支持已读/未读状态与关联任务
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS notifications (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT          NOT NULL,
    message         TEXT            NOT NULL,
    is_read         TINYINT(1)      DEFAULT 0,
    related_task_id BIGINT,
    type            VARCHAR(50)     DEFAULT 'REMINDER',
    created_at      DATETIME       DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notif_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------
-- 8. 邀请表 - 通过邮件邀请用户加入项目，使用 token 验证
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS invitations (
    id          BIGINT          AUTO_INCREMENT PRIMARY KEY,
    project_id  BIGINT          NOT NULL,
    email       VARCHAR(255)    NOT NULL,
    token       VARCHAR(255)    UNIQUE NOT NULL,
    status      VARCHAR(20)     DEFAULT 'PENDING',
    created_at  DATETIME       DEFAULT CURRENT_TIMESTAMP,
    expires_at  DATETIME       NOT NULL,
    CONSTRAINT fk_inv_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 索引 - 优化常见查询性能
-- ============================================================

CREATE INDEX idx_project_members_user ON project_members(user_id);
CREATE INDEX idx_tasks_column ON tasks(column_id);
CREATE INDEX idx_tasks_assignee ON tasks(assignee_id);
CREATE INDEX idx_tasks_due_date ON tasks(due_date);
CREATE INDEX idx_notifications_user ON notifications(user_id, is_read);
CREATE INDEX idx_projects_updated ON projects(updated_at);

SET FOREIGN_KEY_CHECKS = 1;
