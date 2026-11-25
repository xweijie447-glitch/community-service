##额...更改管理员用户信息可以直接在8080端口上增加/resident/profile或复制http://localhost:8080/resident/profile.进行修改=-=
##提示：OvO存储数据均保存在json里，无三方数据库，本品仅做学习使用暂无企业用途，若有问题可发送至邮箱xweijie447@gmail.com

# 社区便民服务预约系统

面向社区居民与物业的便民服务线上预约与派单管理系统。居民在网上下单选服务、挑时间、填地址，物业统一派单，服务人员上门执行，完成后居民评价——替代传统电话登记模式。

**技术栈**：Spring Boot 3.2.5 + Thymeleaf + Spring Security + Spring Data Redis + Spring AMQP（RabbitMQ）+ 本地 JSON 文件存储
**前端**：Thymeleaf 服务端渲染，原生 CSS + 原生 Canvas 图表（不依赖任何前端框架与图表库，离线可用）

---

## 一、快速启动

### 1. 环境要求

| 组件 | 版本 | 说明 |
| --- | --- | --- |
| JDK | 17+ | 项目使用 Java 17 编译 |
| Maven | 3.6+ | **可选**，项目自带 Maven Wrapper（`mvnw`），不装 Maven 也能构建；IDEA 内置的同样可用 |
| Redis | 5.0+ | 服务分类 / 热门服务缓存 + 接口限流 |
| RabbitMQ | 3.8+ | 订单超时取消、异步通知、评价异步统计 |

> 本项目**不使用任何数据库**（无 MySQL、无 SQL 脚本）。业务数据以 JSON 文件形式保存在项目根目录的 `data/` 下。

### 2. 启动中间件（Windows 本机）

```bat
:: Redis：进入你自己的 Redis 安装目录后执行（默认端口 6379）
redis-server.exe redis.windows.conf

:: RabbitMQ 一般已注册为服务，确认 5672 端口在监听即可
net start RabbitMQ
```

Linux / macOS 下对应 `redis-server /usr/local/etc/redis.conf` 与 `rabbitmq-server -detached`。

Redis 可用 `redis-cli ping` 验证，返回 `PONG` 即正常；RabbitMQ 可访问 `http://localhost:15672`（guest/guest）查看队列。

### 3. 启动应用

IDEA：打开项目根目录（`pom.xml` 所在目录）→ 等待依赖下载 → 运行 `CommunityServiceApplication` → 访问 <http://localhost:8080>

命令行：

```bash
# 已安装 Maven
mvn clean package -DskipTests

# 没装 Maven：用项目自带的 Wrapper，首次运行会自动下载 Maven 3.9.16
# Windows 用 mvnw.cmd，Linux / macOS 用 ./mvnw
./mvnw clean package -DskipTests

java -jar target/community-service.jar
```

> 国内网络建议在 `~/.m2/settings.xml` 里配一个阿里云镜像，否则首次拉依赖较慢：
> `<mirror><id>aliyunmaven</id><mirrorOf>*</mirrorOf><url>https://maven.aliyun.com/repository/public</url></mirror>
> 另外项目可直接用 `./mvnw spring-boot:run` 一条命令启动，免去打包步骤。

首次启动会自动初始化演示数据（账号、6 个服务分类、18 个服务项目、28 笔历史订单与评价），日志里会打印 `演示数据初始化完成`。

### 4. 演示账号

| 角色 | 账号 | 密码 | 入口 |
| --- | --- | --- | --- |
| 居民 | `resident01` | `resident123` | 首页 / 服务大厅 |
| 居民 | `resident02`、`resident03` | `resident123` | 同上 |
| 服务人员 | `staff01` | `staff123` | `/staff/workbench` |
| 服务人员 | `staff02`、`staff03`、`staff04` | `staff123` | 同上 |
| 系统管理员 | `admin` | `admin123` | `/admin/dashboard` |

居民也可自助注册（`/register`），服务人员账号只能由管理员创建。

---

## 二、功能对照

### 服务管理
- 服务分类：新增 / 编辑 / 删除 / 启停 / 排序，分类下有服务时禁止删除
- 服务项目：价格、计价单位、服务时长、服务说明、热门标记、上下架
- 居民端服务大厅：分类筛选、关键字搜索、分页；详情页展示价格与服务说明、同类推荐

### 预约下单
- 服务详情页直接下单：上门时间（`datetime-local`，至少提前 30 分钟）、地址、联系人、电话、数量、备注
- 前端 + 后端双重校验（`@Valid` + 字段级错误回显）
- 我的预约：按状态筛选、分页、取消、查看进度
- 幂等：表单携带 `requestToken`，重复提交/刷新只会生成一张单

### 人员管理
- 服务人员档案：技能标签、服务时段、每日最大接单量、从业年限、简介
- 接单状态：可接单 / 忙碌中 / 休息中（休息中不会被派单）
- 服务人员自助维护技能与接单量，管理员可启停账号

### 派单管理
- 订单列表：状态筛选 + 关键字（订单号 / 联系人 / 手机号 / 地址）
- 智能派单：按「技能匹配度 50% + 评分 30% + 负载 20%」打分排序，展示每项得分与不可接原因，一键指派
- 人工改派：候选列表任意指派人，派单记录留痕（AUTO / MANUAL + 得分 + 原因）
- 订单状态调整：受状态机约束，非法流转直接拒绝

### 用户中心
- 居民：资料维护、修改密码、预约历史、我的评价
- 服务人员：工作台（在手工单、开始/完成服务）、接单记录、收到评价、技能设置

### 运营看板
- 订单总量、今日单量、待接单/已派单/服务中/已完成/已取消、今日营收
- 近 7 天订单趋势（折线）、服务类型占比（环形）、订单状态分布（柱状）、服务人员绩效表
- 全部图表由自研 `charts.js`（Canvas）绘制，无第三方依赖

---

## 三、关键技术实现

### 1. 多角色权限（三级）
- `SecurityConfig` 做 URL 级拦截：`/admin/**` → ADMIN，`/staff/**` → STAFF，`/resident/**` → RESIDENT/ADMIN
- 控制器上 `@PreAuthorize` 做方法级兜底，接口与页面双保险
- 页面级用 `sec:authorize` 控制元素显隐
- 登录成功按角色分流：管理员 → 看板，服务人员 → 工作台，居民 → 首页
- 密码 BCrypt 加密；会话并发控制（同一账号只允许一处在线）；CSRF 开启（表单与 AJAX 全局适配）

### 2. 缓存优化
- `@Cacheable` 把服务分类（2h）、热门服务（15min）、服务详情（30min）写入 Redis
- 后台增删改服务时 `@CacheEvict` 主动清理
- `CacheConfig` 注册缓存异常处理器：Redis 故障时降级查文件，不影响主流程

### 3. RabbitMQ 三类场景

| 交换机 / 队列 | 用途 | 实现要点 |
| --- | --- | --- |
| `order.delay.queue` | 订单超时自动取消 | 队列级 `x-message-ttl`，到期死信到 `order.timeout.queue` |
| `order.notify.queue` | 派单 / 完成 / 取消异步通知 | 消费端用 Redis `setIfAbsent` 做消息去重 |
| `review.stats.queue` | 评价异步统计 | 异步汇总服务人员评分与服务项目评分 |

> 队列 TTL 取 `app.order.timeout-minutes`。修改该值后需在 RabbitMQ 管理台删除 `order.delay.queue` 让其按新 TTL 重建（队列参数变更无法原地修改）。
> MQ 不可用时下单不会失败，超时取消由定时补偿扫描兜底。

### 4. 订单状态流转与幂等
```
待接单 PENDING ──派单──> 已派单 ASSIGNED ──开始服务──> 服务中 IN_SERVICE ──完成──> 已完成 COMPLETED
     └────────── 取消 ──────────┴──────── 取消 ────────┴──> 已取消 CANCELLED
```
- 状态机集中在 `OrderStatus` 枚举，非法流转抛业务异常
- 幂等三层：下单唯一令牌、状态流转「已是目标状态直接返回」、MQ 消费 Redis 去重
- 写操作 `synchronized` + 存储层加锁 + `version` 字段递增，避免并发覆盖
- 每次流转写入 `order_status_logs.json`，全程可追溯

### 5. 接口限流
- `@RateLimit` 注解 + AOP + Redis Lua 固定窗口计数，默认同一用户 60 秒内最多 5 次预约提交
- 触发后返回友好错误页；Redis 故障时降级放行，避免限流组件成为单点故障

### 6. 邮件与短信模拟
- `MessageService` 双通道：`app.message.simulated=true` 时只落库 + 打日志，不真正投递；生产可切 SMTP
- 触发点：下单成功、派单（同时通知居民与服务人员）、服务完成、超时取消、次日工单提醒
- 所有消息可在「管理后台 → 消息记录」查看

### 7. 定时任务
- 每日 02:30：已完成超过 7 天的订单标记归档（`archived=true`，列表不再展示，数据保留可审计）
- 每日 08:00：汇总次日待服务工单，推送给服务人员与管理员
- 每 5 分钟：补偿扫描，兜底取消超时未派单订单

### 8. 本地文件存储
- `JsonStore<T>` 启动时把 `data/*.json` 全量读入内存，读走内存、写立即落盘（先写临时文件再原子替换）
- 写操作加锁，单机多线程安全；实体主键自增，重启后从已有数据继续
- 换电脑只需拷贝整个项目目录（含 `data/`），数据不丢
- 想重置演示数据：停止应用 → 删除 `data/` 目录 → 重启，自动重新初始化

### 9. 工程化规范
- 统一响应：`ApiResponse{code, message, data, timestamp}`
- 全局异常处理：接口异常返回 JSON，页面异常渲染友好错误页（业务异常 / 403 / 404 / 500）
- 参数校验：`jakarta.validation` 注解 + 字段级错误回显
- 日志：控制台 + `logs/community-service.log`，按天与大小滚动归档

---

## 四、目录结构

```
community-service
├── src/main/java/com/community
│   ├── common/          # 统一响应、异常处理、限流注解与切面、模板格式化
│   ├── config/          # 安全、Redis、RabbitMQ、线程池、数据初始化
│   ├── controller
│   │   ├── page/        # Thymeleaf 页面控制器（居民端 / 服务人员端 / 管理端）
│   │   └── api/         # JSON 接口（统计看板）
│   ├── domain/          # 实体与枚举（订单状态机、角色、接单状态）
│   ├── dto/             # 表单对象（含校验注解）
│   ├── mq/              # 消息生产者、消费者、消息体
│   ├── repository/      # 基于 JsonStore 的数据访问
│   ├── security/         # 登录主体、UserDetailsService、登录分流
│   ├── service/         # 业务服务（订单、派单、评价、统计、定时任务、通知）
│   ├── storage/         # JSON 文件存储引擎
│   └── vo/              # 页面展示对象
├── src/main/resources
│   ├── templates/       # Thymeleaf 页面
│   ├── static/          # css / js（含自研 Canvas 图表）
│   ├── application.yml  # 通用配置（存储目录、超时、限流、派单权重）
│   ├── application-dev.yml
│   └── logback-spring.xml
├── data/                # 运行时数据（自动生成，已在 .gitignore 中忽略）
└── logs/                # 运行日志（自动生成，已忽略）
```

---

## 五、配置说明（`application.yml`）

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `app.storage.dir` | `./data` | 数据目录，相对应用启动目录 |
| `app.order.timeout-minutes` | `30` | 未派单超时自动取消（分钟） |
| `app.order.keep-days` | `7` | 已完成订单归档天数 |
| `app.rate-limit.booking-limit` | `5` | 窗口内允许的预约提交次数 |
| `app.rate-limit.window-seconds` | `60` | 限流窗口秒数 |
| `app.message.simulated` | `true` | true=模拟发送（落库+日志），false=真实 SMTP |
| `app.schedule.enabled` | `true` | 定时任务总开关 |
| `app.dispatch.skill-weight` / `rating-weight` / `load-weight` | 50/30/20 | 智能派单权重 |

---

## 六、常见问题

**Q：没启动 Redis 能跑吗？**
能。缓存读取失败会自动降级到文件，接口限流降级放行，只是失去缓存与限流保护。

**Q：没启动 RabbitMQ 会怎样？**
下单、派单、完成都不受影响（消息投递失败只记错误日志），超时取消由 5 分钟一次的补偿扫描兜底；异步通知与评价统计会缺失。

**Q：改了订单超时时长，为什么超时取消没生效？**
`order.delay.queue` 的 TTL 建在队列上，改配置后需先删除该队列（RabbitMQ 管理台 Queues → 删除）再重启应用。

**Q：数据文件能直接改吗？**
可以，是格式化好的 JSON。修改前请停止应用，避免被内存快照覆盖。

**Q：这是单机方案，多人同时访问会冲突吗？**
写操作在存储层加锁并原子落盘，本机多线程安全；不适合多实例部署（会各自持有内存副本）。

---

