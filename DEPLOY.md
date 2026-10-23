# 部署说明

前后端分离部署：Nginx 托管前端静态文件，并把 `/api` 反向代理到 Spring Boot。
本文以 Windows 本机为例，Linux 只是路径和启动命令的差别。

## 1. 前置条件

| 组件 | 版本 | 说明 |
|---|---|---|
| JDK | 17+ | 后端运行环境 |
| MySQL | 8.0+ | 需要先启动服务 |
| Nginx | 任意稳定版 | 托管 `frontend/dist` 并反代 `/api`；没有也可以先用 `vite preview` 验证 |

## 2. 初始化数据库

```bash
mysql -uroot -p < sql/schema.sql
```

脚本会创建 `campus_book` 库和 `user`、`book`、`orders`、`report`、`review` 五张表。脚本可重复运行且不会删除已有数据；它只创建缺失的表，不会迁移已有表结构。没有预置测试账号，启动后请先通过注册页面或 `POST /api/auth/register` 创建账号。

## 3. 后端打包与启动

```bash
# 打包，产物在 target/campus-book-platform-1.0.0.jar
mvn clean package -DskipTests
```

启动：

```bash
java -jar target/campus-book-platform-1.0.0.jar --server.port=8080
```

生产环境建议用环境变量覆盖敏感配置：

```bash
# Linux / macOS
export DB_USERNAME=root
export DB_PASSWORD=你的数据库密码
export JWT_SECRET=至少32字节的随机字符串
java -jar target/campus-book-platform-1.0.0.jar
```

```powershell
# Windows PowerShell
$env:DB_USERNAME = 'root'
$env:DB_PASSWORD = '你的数据库密码'
$env:JWT_SECRET = '至少32字节的随机字符串'
java -jar target\campus-book-platform-1.0.0.jar
```

验证：

```bash
curl http://localhost:8080/actuator/health
# {"status":"UP", ...}
```

## 4. 前端构建

```bash
cd frontend
npm install
npm run build
```

产物在 `frontend/dist/`，包含 `index.html` 和 `assets/`。

本地可以先验证生产构建：

```bash
npm run preview
# http://localhost:4173
```

`vite.config.js` 的 `preview.proxy` 已把 `/api` 代理到 8080，和 Nginx 行为一致。

## 5. Nginx 配置

```nginx
server {
    listen 80;
    server_name localhost;

    # 指向 frontend/dist 的绝对路径
    root D:/campus-book-platform/frontend/dist;
    index index.html;

    # 前端是 history 路由，刷新 /orders 这类地址要回落到 index.html
    location / {
        try_files $uri $uri/ /index.html;
    }

    # API 反向代理到 Spring Boot
    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    # 上传的图片也由 Spring Boot 的 /uploads/** 静态映射提供，必须一起代理
    location /uploads/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
    }
}
```

修改后 reload：

```bash
nginx -t
nginx -s reload
```

访问 `http://localhost/`，先注册账号，再使用注册时填写的学号和密码登录。

## 6. 部署验证清单

| 检查项 | 预期 |
|---|---|
| `curl http://localhost:8080/actuator/health` | `status: UP`，db 组件也是 UP |
| `curl http://localhost/api/books?pageNum=1&pageSize=5` | 返回 `{"code":200,...}` |
| 浏览器打开首页 | 登录页正常，静态资源无 404 |
| 登录后刷新 `/orders` | 不出现 404（`try_files` 回落生效） |
| 发布、下单、取消 | 状态和库存正确变化 |

## 7. 常见问题

**刷新页面 404**：Nginx 少了 `try_files $uri $uri/ /index.html`。

**接口 502**：后端没启动，或 `proxy_pass` 的端口不是 8080。

**登录 401**：`JWT_SECRET` 变了导致旧 token 失效，重新登录即可。

**数据库连接失败**：确认 MySQL 服务已启动、`campus_book` 已初始化、`DB_PASSWORD` 正确。

**生产不要用默认配置**：`application.yml` 里的 JWT 默认密钥、`root/root` 数据库账号只适合本地开发。

## 8. Docker Compose 一键部署

仓库里已经提供了 `Dockerfile`、`frontend/Dockerfile`、`frontend/nginx.conf` 和 `docker-compose.yml`。

前提：安装 Docker Desktop（Windows / macOS）或 Docker Engine + Compose 插件（Linux）。

```bash
# 1. 准备环境变量，至少改掉 JWT_SECRET
cp .env.example .env

# 2. 构建并启动 MySQL、后端、前端
docker compose up -d --build

# 3. 查看状态
docker compose ps
docker compose logs -f backend
```

启动后：

| 服务 | 地址 |
|---|---|
| 前端 | http://localhost |
| 后端 API | http://localhost:8080 |
| Swagger | http://localhost:8080/swagger-ui/index.html |
| MySQL | localhost:3306（root / .env 里的密码） |

说明：

- MySQL 首次启动会自动执行 `sql/schema.sql` 建库建表；已有数据卷不会重复执行，想重置就 `docker compose down -v`（会删数据）。
- `uploads/` 目录挂载到宿主机，重建容器图片不丢。
- 前端容器里的 Nginx 会把 `/api`、`/uploads` 反代到 `backend:8080`。
- 这套配置没有在本机实际运行验证过（当前机器没装 Docker），首次启动如果有问题优先看 `docker compose logs backend`。

## 9. 接口文档与代码规范

- **Swagger UI**：`/swagger-ui/index.html`，登录接口拿 token 后，右上角 Authorize 填 `Bearer <token>` 就能调需要登录的接口。
- **OpenAPI JSON**：`/v3/api-docs`，可以导入 Postman / Apifox。
- **前端代码规范**：

  ```bash
  cd frontend
  npm run lint      # ESLint 检查
  npm run format    # Prettier 格式化
  ```

- **后端测试**：`mvn test`，包含 `OrderFlowTest`（下单 → 取消库存回滚、重复购买、买自己的书）和 `JwtUtilTest`。
- **许可证**：本项目使用 MIT License，详见 [LICENSE](LICENSE)。
