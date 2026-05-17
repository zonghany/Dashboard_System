## Context

本项目是一个全新的 AI 驱动的智能任务协同看板系统。当前状态为零起点（Greenfield），无遗留代码或数据库需要迁移。目标用户是需要高效任务管理与可视化追踪的现代团队。

**技术约束**（来自项目技术宪法）：
- 前端：Vue 3 + Vue Draggable（拖拽）+ Vue Router（路由守卫）
- 后端：Spring Boot 3 + Spring AI（大模型集成）+ Spring Security + JWT
- 数据库：PostgreSQL（需充分利用 JSONB 扩展能力）
- 通信：REST API + WebSocket（STOMP 协议）实时同步
- 认证：JWT 无状态认证，BCrypt 密码加密

**业务约束**：
- 看板拖拽响应 < 100ms，WebSocket 同步延迟 < 500ms
- 支持 Chrome、Firefox、Edge 最新两个版本
- 所有 API 必须通过 JWT 认证
- 数据备份与恢复能力

## Goals / Non-Goals

**Goals:**
- 构建完整的 JWT 认证体系（注册、登录、令牌刷新、登出）
- 实现项目管理（CRUD、成员邀请、角色权限）
- 实现可拖拽的自定义看板，通过 WebSocket 实时同步
- 实现任务详情管理（字段编辑、附件上传、实时同步）
- 实现基于定时扫描的截止日期邮件+站内信双通道提醒
- 集成 Spring AI 实现大模型驱动的标签/工时建议与自然语言批量操作
- 数据库使用 JSONB 字段为未来扩展预留灵活性

**Non-Goals:**
- 不实现 OAuth2.0 第三方登录（如 Google、GitHub 登录）—— 留待后续迭代
- 不实现移动端原生 App —— 仅面向桌面浏览器
- 不实现离线编辑与冲突合并 —— 采用 last-write-wins 策略
- 不实现文件版本管理或在线预览 —— 附件仅做存储和下载
- 不实现实时音视频或即时通讯功能
- 不实现多语言国际化 —— 首个版本仅支持中文

## Decisions

### D1: WebSocket 采用 STOMP over SockJS 协议
- **决策**：使用 Spring WebSocket + STOMP 协议 + SockJS 回退方案
- **理由**：STOMP 提供发布-订阅模型，天然适合看板广播场景（按 projectId 隔离频道）；SockJS 在不支持 WebSocket 的旧浏览器上可降级为 HTTP 长轮询
- **替代方案**：原生 WebSocket（无 STOMP）—— 需要自行实现消息路由和序列化，工作量更大；Server-Sent Events —— 仅支持单向推送，无法处理客户端发起的状态更新

### D2: JWT 令牌存储策略
- **决策**：前端将 JWT 存储在 localStorage，每次请求通过 Axios 拦截器自动附加 Authorization Header
- **理由**：实现简单，配合路由守卫可快速完成认证拦截；刷新令牌机制通过临近过期时自动续期实现无缝体验
- **替代方案**：HttpOnly Cookie —— 更安全但需要 CSRF 防护，且在 WebSocket 握手时传递 Cookie 增加复杂度；内存存储 —— 页面刷新后丢失，用户体验差

### D3: 拖拽实现方案
- **决策**：使用 Vue Draggable (vuedraggable) 实现看板列内和跨列拖拽
- **理由**：基于 SortableJS，支持嵌套列表拖拽；与 Vue 3 响应式系统无缝集成；社区活跃，文档完善
- **替代方案**：原生 HTML5 Drag & Drop API —— 需要大量自定义逻辑处理列间移动、动画和触摸支持；react-beautiful-dnd —— 仅为 React 设计

### D4: AI 集成架构
- **决策**：通过 Spring AI 抽象层对接大模型 API，支持多种模型供应商配置切换
- **理由**：Spring AI 提供统一的 ChatClient 接口，降低与特定大模型供应商的耦合度；支持流式响应，为未来扩展流式输出预留能力
- **替代方案**：直接调用 OpenAI/Claude SDK —— 供应商锁定，切换成本高

### D5: 数据库 JSONB 扩展策略
- **决策**：`tasks` 表使用 `metadata JSONB` 字段存储 AI 生成的标签、自定义字段等非结构化数据
- **理由**：PostgreSQL JSONB 支持索引和查询，兼顾灵活性与性能；避免频繁 DDL 变更；JSONB 的 GIN 索引可支持对 AI 生成标签的高效查询
- **替代方案**：EAV (Entity-Attribute-Value) 模式 —— 查询复杂、性能差；单独自定义字段表 —— 增加 JOIN 复杂度

### D6: 通知系统架构
- **决策**：采用定时任务（Spring @Scheduled，每 30 分钟）+ 事件驱动（WebSocket 推送）的混合模式
- **理由**：定时扫描保证截止提醒不遗漏；WebSocket 推送保证站内信实时到达；解耦邮件发送（异步）避免阻塞主流程
- **替代方案**：纯事件驱动 —— 依赖任务修改事件触发提醒，可能遗漏未被编辑的临近截止任务；消息队列 —— 过度设计，当前规模不需要引入 RabbitMQ/Kafka

### D7: 前端状态管理方案
- **决策**：使用 Vue 3 Composition API + provide/inject 进行状态管理，不使用 Vuex/Pinia
- **理由**：当前应用状态以项目+看板为作用域，provide/inject 天然支持作用域隔离；Composition API 的 reactive/ref 足以管理 WebSocket 连接和令牌状态
- **替代方案**：Pinia —— 对当前规模而言过度设计，增加学习成本和打包体积

## Risks / Trade-offs

- **[风险] WebSocket 广播风暴**：当看板任务数量很大（>1000），频繁拖拽可能导致大量广播消息
  → **缓解措施**：对拖拽操作实施防抖（debounce 300ms），合并短时间内同一任务的位置更新

- **[风险] LLM API 调用延迟和成本**：每次任务描述变更触发 AI 请求可能产生大量 API 调用
  → **缓解措施**：实施 3 秒输入防抖 + 最少 10 字符阈值；前端显示 "AI 建议" 为可选功能，不阻塞用户正常操作

- **[风险] JWT 令牌泄露**：localStorage 存储 JWT 易受 XSS 攻击
  → **缓解措施**：前端对所有用户输入进行严格转义（Vue 默认转义）；实施 Content Security Policy (CSP) 头

- **[风险] 并发拖拽数据不一致**：last-write-wins 策略可能导致用户的拖拽操作被覆盖
  → **缓解措施**：通过 WebSocket 实时广播状态，所有客户端在收到远程更新时立即刷新视图，保持最终一致性

- **[取舍] 邮件验证简化**：首版不实现严格的邮箱验证码流程（即不阻止未验证邮箱登录）
  → **理由**：加快开发进度，降低用户注册摩擦；后续迭代可增加邮箱验证强制执行

- **[取舍] 附件存储本地文件系统**：不引入对象存储（如 S3/MinIO）
  → **理由**：初期用户规模小，本地存储够用；系统设计预留存储层抽象接口，便于后续切换