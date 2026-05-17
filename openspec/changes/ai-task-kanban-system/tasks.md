## 1. 项目基础设施搭建

- [ ] 1.1 初始化 Spring Boot 3 后端项目，配置 PostgreSQL 数据源、Spring Security、Spring AI、WebSocket 依赖，创建多模块包结构（auth/project/board/task/notification/ai）
- [ ] 1.2 初始化 Vue 3 前端项目，配置 Vue Router、Axios、Vue Draggable，创建页面路由结构（login/register/projects/project-detail），搭建基础布局组件（导航栏、侧边栏、主内容区）

## 2. 数据库建模与实体设计

- [ ] 2.1 编写数据库初始化 SQL 脚本，创建 `users`、`projects`、`project_members`、`board_columns`、`tasks`、`task_attachments`、`notifications`、`invitations` 八张表，包含所有主键、外键约束、唯一索引及 `tasks.metadata JSONB` 字段
- [ ] 2.2 创建对应的 Spring Data JPA Entity 类与 Repository 接口，确保 JSONB 字段映射正确（使用 `@Type(JsonType.class)`），编写基础 CRUD 集成测试验证所有 Repository 可用

## 3. 用户认证模块

- [ ] 3.1 实现后端 `/api/auth/register` 和 `/api/auth/login` 端点，包含邮箱格式校验、密码强度校验（至少8位+大小写+数字）、BCrypt 加密存储、JWT 令牌生成（7天有效期），注册成功返回 HTTP 201 并发送验证邮件
- [ ] 3.2 实现 JWT 认证过滤器（OncePerRequestFilter）和全局异常处理，对所有 `/api/**` 路径进行令牌校验；未认证返回 HTTP 401，令牌格式错误返回 "无效的认证令牌"
- [ ] 3.3 实现前端登录/注册页面，配置 Axios 拦截器自动附加 Authorization Header 和处理 401 响应（清除令牌并跳转登录页），配置 Vue Router 全局前置守卫拦截未认证访问

## 4. 项目管理模块

- [ ] 4.1 实现后端 `/api/projects` CRUD 端点：创建项目时自动将当前用户设为管理员并生成默认三列（待办/进行中/已完成）；项目列表按 `updated_at` 降序排列、分页（20条/页）
- [ ] 4.2 实现成员邀请功能：管理员通过 `/api/projects/{id}/invitations` 发送邀请邮件（含48小时有效链接），受邀者点击链接接受后自动加入项目；实现角色变更与成员移除 API，保护「至少一名管理员」约束
- [ ] 4.3 实现前端项目列表页（显示项目名、成员数、最后更新时间、空状态引导）和项目详情页（显示项目信息、成员管理面板、看板主视图）

## 5. 看板与 WebSocket 实时同步

- [ ] 5.1 实现后端看板列 CRUD 端点（`/api/boards/{projectId}/columns`）：创建/重命名/排序/删除列，删除列时将任务迁移到「待办」默认列并弹出确认提示，保护「至少保留一列」约束
- [ ] 5.2 配置 Spring WebSocket + STOMP 端点 `/ws/board/{projectId}`，实现 JWT 握手拦截器认证；后端在看板列变更、任务拖拽/创建/删除时通过 `SimpMessagingTemplate` 向对应 projectId 频道广播消息
- [ ] 5.3 实现前端看板主视图：使用 Vue Draggable 渲染可拖拽列和任务卡片，通过 SockJS + STOMP.js 连接 WebSocket 频道，收到广播后实时更新本地看板状态；新增「添加任务」按钮在列底部创建空白任务卡片

## 6. 任务详情与附件管理

- [ ] 6.1 实现后端任务 CRUD 端点（`/api/tasks`）：字段包含标题、描述、优先级、截止日期、负责人、预估工时；支持 debounce 自动保存（2秒防抖），删除任务时级联删除附件并 WebSocket 广播
- [ ] 6.2 实现后端附件上传端点（`/api/tasks/{id}/attachments`）：校验文件类型（PNG/JPG/GIF/WebP/PDF/DOC/DOCX/XLS/XLSX/TXT）和大小（≤10MB），返回 HTTP 400/413 校验失败，保存到本地文件系统并记录附件元数据
- [ ] 6.3 实现前端任务详情右侧滑出面板组件：显示/编辑所有任务字段（标题失焦/回车保存、优先级下拉、日期选择器、负责人选择器），附件上传拖拽区域+文件列表，删除确认弹窗，所有操作实时同步

## 7. 通知提醒系统

- [ ] 7.1 实现后端定时任务（`@Scheduled` 每30分钟）扫描截止时间 <24h 的任务，对每个有负责人的任务发送邮件提醒并创建站内信通知记录；实现去重逻辑（24h内同一任务不重复提醒）和邮件发送失败重试
- [ ] 7.2 实现后端通知 API（`/api/notifications`）：获取未读列表、标为已读、未读计数；新通知通过 WebSocket 实时推送到对应用户客户端
- [ ] 7.3 实现前端通知中心组件：导航栏铃铛图标+未读红点徽章，下拉列表显示通知（未读高亮、按时间倒序），点击通知跳转到对应任务并自动标为已读

## 8. AI 智能功能

- [ ] 8.1 实现后端 AI 建议端点（`/api/ai/suggest`）：接收任务描述文本，通过 Spring AI ChatClient 调用大模型生成标签（最多5个）和预估工时（小时数），返回结构化 JSON；实现 15s 超时熔断和异常降级
- [ ] 8.2 实现后端 AI 指令端点（`/api/ai/command`）：接收自然语言指令，通过大模型解析为结构化操作（目标过滤条件 + 执行动作），执行批量任务更新；处理解析失败、空结果、破坏性操作确认等边界场景
- [ ] 8.3 实现前端 AI 面板组件：任务描述输入时3秒防抖触发 AI 建议（最少10字符阈值），展示建议标签和工时及"采纳"/"拒绝"按钮；AI 指令输入框支持自然语言批量操作提交并展示操作结果摘要

## 9. 集成验证与收尾

- [ ] 9.1 编写端到端集成测试：覆盖用户注册→登录→创建项目→邀请成员→创建列→创建任务→拖拽流转→上传附件→删除任务→收到通知→登出的完整用户旅程，验证 WebSocket 多客户端同步和 AI 建议/指令可用性
- [ ] 9.2 实施安全加固：确认所有 `/api/**` 端点均有 JWT 认证保护，BCrypt 工作因子 ≥10，API 响应不含密码哈希；配置 CORS 白名单、CSP 头、文件上传大小限制