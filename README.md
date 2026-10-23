# 校园二手书交易平台

Spring Boot + MyBatis-Plus + MySQL 的校园二手书交易后端脚手架，包含用户注册登录（JWT）、书籍浏览与发布、下单（含防超卖）。

## 技术栈

| 组件 | 版本 | 说明 |
|---|---|---|
| JDK | 17+ | `pom.xml` 中 `java.version=17` |
| Spring Boot | 3.5.16 | Web / Validation / Test |
| MyBatis-Plus | 3.5.17 | 用户模块用 BaseMapper / ServiceImpl；书籍、订单保留 XML |
| MySQL | 8.0+ | 驱动 `mysql-connector-j`，版本由 Boot BOM 管理 |
| JJWT | 0.13.0 | `jjwt-api` + `jjwt-impl` + `jjwt-jackson` |
| Lombok | Boot BOM 管理 | 编译期生成 getter/setter |

## 目录结构

```
二手书交易平台/
├── pom.xml
├── sql/
│   └── schema.sql                     # 建库建表（用户/书籍/订单/举报/评价）
├── src/main/java/com/campusbook/
│   ├── CampusBookApplication.java     # 启动类，@MapperScan
│   ├── common/                        # Result / ResultCode / 业务异常 / 全局异常 / 分页
│   ├── config/                        # JWT 配置、拦截器注册、CORS、BCrypt
│   ├── context/UserContext.java       # ThreadLocal 保存当前登录用户
│   ├── interceptor/LoginInterceptor.java
│   ├── util/JwtUtil.java              # token 生成与解析
│   ├── entity/                        # User / Book / Order
│   ├── dto/                           # 入参对象，带校验注解
│   ├── vo/                            # 出参对象（LoginVO）
│   ├── mapper/                        # Mapper 接口
│   ├── service/ + service/impl/       # 业务接口与实现
│   └── controller/                    # Auth / User / Book / Order
├── src/main/resources/
│   ├── application.yml
│   └── mapper/                        # MyBatis XML
└── src/test/java/.../JwtUtilTest.java # 不依赖数据库的单元测试
```

## 快速开始

1. 初始化数据库：

   ```bash
   mysql -uroot -p < sql/schema.sql
   ```

   脚本可重复运行，不会删除已有表或数据；它只创建缺失的表，不会迁移已有表结构。首次启动后通过注册接口创建账号，项目没有预置测试账号。

2. 配置数据库账号，二选一：

   - 直接改 `src/main/resources/application.yml` 的 `spring.datasource.username/password`；
   - 或使用环境变量 `DB_USERNAME`、`DB_PASSWORD`（默认 root/root）。

3. 启动：

   ```bash
   mvn spring-boot:run
   ```

4. 验证：

   ```bash
   curl http://localhost:8080/api/books
   ```

   预期返回 `{"code":200,"message":"操作成功","data":{"total":0,...}}`。

## 接口清单

| 方法 | 路径 | 需要登录 | 说明 |
|---|---|---|---|
| POST | `/api/auth/register` | 否 | 注册并直接返回 token |
| POST | `/api/auth/login` | 否 | 学号 + 密码登录 |
| GET | `/api/books?pageNum=1&pageSize=10&keyword=&category=` | 否 | 在售书籍分页（创建时间倒序） |
| GET | `/api/books/{id}` | 否 | 书籍详情（浏览量 +1） |
| GET | `/api/users/me` | 是 | 当前登录用户（从 ThreadLocal 取 userId） |
| POST | `/api/sell/books` | 是 | 发布二手书 |
| PUT | `/api/sell/books/{id}/status?status=0` | 是 | 上下架：0-下架，1-重新上架 |
| POST | `/api/orders` | 是 | 下单 |
| GET | `/api/orders?role=buyer` | 是 | 我买到的 / `role=seller` 我卖出的 |
| GET | `/api/orders/{id}` | 是 | 订单详情（买卖双方可看） |

需要登录的接口统一携带请求头：

```
Authorization: Bearer <token>
```

### 调用示例

```bash
# 注册
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"studentNo":"20230001","nickname":"小明","password":"123456","campus":"东校区"}'

# 发布书籍（把 $TOKEN 换成上一步返回的 token）
curl -X POST http://localhost:8080/api/sell/books \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"title":"高等数学（第七版）上册","author":"同济大学数学系","category":"教材","price":15.00,"originalPrice":49.80,"conditionLevel":8}'

# 分页查询在售书籍
curl "http://localhost:8080/api/books?pageNum=1&pageSize=10&keyword=高等数学&category=教材"

# 下架 / 重新上架（卖家本人）
curl -X PUT "http://localhost:8080/api/sell/books/1/status?status=0" \
  -H "Authorization: Bearer $TOKEN"

# 下单
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"bookId":1,"quantity":1,"deliveryType":1,"deliveryAddr":"图书馆门口"}'
```

## JWT 说明

- 密钥来自 `app.jwt.secret`，生产环境用环境变量 `JWT_SECRET` 覆盖；HS256 要求至少 32 字节，`JwtUtil` 构造时会校验。
- 有效期 `app.jwt.expire-minutes`，默认 120 分钟。
- 拦截器注册在 `/api/**`，排除 `/api/auth/**`、`/api/books/**` 和 `/error`；解析出的 userId 放进 `UserContext`（ThreadLocal），请求结束在 `afterCompletion` 里清理。
- 项目只引入 `spring-security-crypto` 使用 BCrypt，没有引入 `spring-boot-starter-security`，因此不会触发 Spring Security 默认的登录页与过滤器链。

## 几个实现要点

- **统一下单防超卖**：`BookMapper.decreaseStock` 用一条带 `status = 1 AND stock >= quantity` 的 UPDATE 原子占位，受影响行数为 0 就抛出"已被抢走"，而不是先查后改。
- **订单快照**：`orders.book_title`、`orders.unit_price` 记录下单时的书名与价格，卖家后续改价不影响历史订单。
- **金额类型**：数据库 `DECIMAL(10,2)`，Java `BigDecimal`，不使用 double。
- **分页**：MyBatis-Plus 的 `Page` + `PaginationInnerInterceptor`，SQL 由插件生成；分页插件在 `MybatisPlusConfig` 注册，且需要额外引入 `mybatis-plus-jsqlparser`（3.5.9 起拆出的独立模块）。
- **密码**：BCrypt 加密存储，登录失败不区分"用户不存在/密码错误"。
- **MyBatis-Plus 版本差异**：3.5.17 的 `IService` / `ServiceImpl` 在 `com.baomidou.mybatisplus.spring.service` 包下，不是老教程里的 `extension.service`，直接抄旧代码会编译失败。
- **ThreadLocal**：`LoginInterceptor` 在 `preHandle` 写入、`afterCompletion` 清理；异步任务（@Async、线程池）不会自动传递上下文。
- **JDK 23+ 编译**：`pom.xml` 已配置 `maven-compiler-plugin` 的 `annotationProcessorPaths`，否则新版 javac 不会自动运行 Lombok。

## 工程化

- **接口文档**：启动后访问 `/swagger-ui/index.html`，登录接口拿 token 后在 Authorize 里填 `Bearer <token>`；`/v3/api-docs` 可导入 Postman / Apifox。
- **测试**：`mvn test` 跑 6 个用例，`OrderFlowTest` 覆盖下单、取消后库存回滚、重复购买、买自己的书，用 `@Transactional` 自动回滚不污染开发库。
- **代码规范**：前端 `npm run lint`（ESLint 10 + eslint-plugin-vue）和 `npm run format`（Prettier）。
- **日志**：`TraceIdFilter` 给每个请求生成 traceId，写入 MDC 和响应头 `X-Trace-Id`，日志格式是 `时间 级别 [traceId] 类名 - 消息`。
- **Docker**：`docker compose up -d --build` 一键起 MySQL + 后端 + Nginx，配置见 [docker-compose.yml](docker-compose.yml)，说明见 [DEPLOY.md](DEPLOY.md)。
- **前端 404**：未匹配的路由进自定义 404 页面。
- **许可证**：本项目使用 MIT License，详见 [LICENSE](LICENSE)。

## 后续可以补的部分

- 收藏、站内信、书籍留言；
- 管理员用户管理（封禁违规账号）；
- 用对象存储替代本地图片上传；
- CI（GitHub Actions）与自动化部署。
