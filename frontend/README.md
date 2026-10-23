# 校园二手书前端

Vue 3 + Vite + Element Plus，对接本项目的 Spring Boot 后端。

## 启动

```bash
cd frontend
npm install
npm run dev
```

访问 http://localhost:5173 。

开发服务器把 `/api` 代理到 `http://localhost:8080`（见 `vite.config.js`），
所以后端要先启动，并且使用默认的 8080 端口。

## 页面

| 路由 | 说明 |
|---|---|
| `/login` | 登录页，学号 + 密码，带表单校验 |
| `/books` | 书籍列表，支持按书名搜索、分类筛选、分页、购买 |

未登录访问 `/books` 会被路由守卫重定向到 `/login`，登录成功后回到原页面。

## 请求封装

`src/utils/axios.js`：

- 请求拦截器自动加 `Authorization: Bearer <token>`；
- 响应拦截器把后端 `{ code, message, data }` 拆成 `data`；
- `code === 401` 时清空 localStorage 并跳回登录页；
- 业务错误用 Element Plus 的 `ElMessage` 统一提示。

## 首次使用

项目没有预置测试账号。启动后在登录页选择“立即注册”，创建账号后即可登录。
