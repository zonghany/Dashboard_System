## Why

现代团队协作缺乏一个集任务看板、实时同步、智能辅助于一体的协同工具。当前市场中看板工具缺乏原生 AI 能力（如自然语言指令批量操作、智能标签建议），且团队内多人在线协作时数据同步滞后。本项目旨在填补这一空白，提供 AI 驱动的智能任务协同看板系统，提升团队任务管理效率与决策质量。

## What Changes

- **新增** JWT 无状态用户认证体系，支持邮箱注册、登录、令牌自动续期与登出
- **新增** 多项目管理能力，支持项目创建、成员邀请、角色权限管理（管理员/普通成员）
- **新增** 可自定义看板列的拖拽式任务看板，基于 WebSocket 实现多客户端实时状态同步
- **新增** 任务详情侧边栏，支持标题、描述、优先级、截止日期、负责人、附件等完整字段管理
- **新增** 通知提醒系统，支持任务截止前 24 小时的邮件与站内信双通道提醒
- **新增** AI 智能功能模块，集成大模型 API 实现任务标签推荐、工时预估、自然语言批量操作指令解析
- **新增** BCrypt 密码加密存储，所有 API 接口强制 JWT 认证
- **新增** 数据库 JSONB 扩展字段，为未来功能迭代预留灵活数据空间

## Capabilities

### New Capabilities

- `user-auth`: JWT 无状态认证，包含注册（邮箱验证、密码强度校验）、登录（7天有效期令牌）、令牌过期自动跳转与登出清除
- `project-management`: 项目 CRUD 操作，管理员邀请成员（邮件邀请链接），成员角色管理（管理员/普通成员），按更新时间倒序排列项目列表
- `task-kanban`: 自定义看板列的增删改查与排序，拖拽任务卡片跨列流转，通过 WebSocket 实时广播状态变更到所有在线客户端
- `task-detail`: 任务详情右侧滑出侧边栏，支持标题/描述/优先级/截止日期/负责人编辑、附件上传（类型与大小校验）、删除确认，编辑实时同步
- `notification-system`: 定时扫描截止前 24 小时任务，发送邮件与站内信；铃铛图标展示未读列表，点击标记已读
- `ai-smart-features`: 输入任务描述时调用大模型生成标签与预估工时建议，支持"采纳/拒绝"交互；自然语言指令（如"标记所有逾期任务为紧急"）解析与批量执行

### Modified Capabilities

<!-- 此为全新项目，无现有能力需修改 -->

## Impact

### 前端 (Vue 3 + Vue Draggable)
- **新增页面/视图**：登录页、注册页、项目列表页、项目详情页（含看板主视图）、任务详情侧边栏组件、通知中心组件、AI 建议面板组件
- **新增状态管理**：JWT 令牌管理、WebSocket 连接管理、看板列/任务状态实时同步
- **新增路由守卫**：未认证自动跳转登录页

### 后端 (Spring Boot 3 + Spring AI)
- **新增 API 模块**：认证 API (`/api/auth/*`)、项目 API (`/api/projects/*`)、看板 API (`/api/boards/*`)、任务 API (`/api/tasks/*`)、通知 API (`/api/notifications/*`)、AI API (`/api/ai/*`)
- **新增 WebSocket 端点**：`/ws/board/{projectId}` 用于看板状态实时广播
- **新增中间件**：JWT 认证过滤器、WebSocket 认证拦截器
- **新增定时任务**：截止日期提醒扫描（每 30 分钟）
- **新增 AI 集成**：Spring AI 对接大模型 API，处理自然语言指令解析与标签/工时建议生成

### 数据库 (PostgreSQL)
- **新增表**：`users`、`projects`、`project_members`、`board_columns`、`tasks`、`task_attachments`、`notifications`
- **关键约束**：email UNIQUE、JWT token 索引、project_id + column_order 联合索引
- **扩展字段**：`tasks.metadata JSONB` 支持未来自定义字段

### 外部依赖
- Spring AI（大模型集成）、Spring WebSocket、Spring Security + JWT、Spring Mail
- Vue Draggable（拖拽）、Vue Router（路由守卫）
- PostgreSQL JSONB 特性