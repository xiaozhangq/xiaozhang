# SpringBoot + Vue3 点餐系统

一个前后端分离的简易点餐项目，包含：

- **前台点餐端**：菜单分类、菜品展示、购物车、提交订单
- **后台管理端**：分类管理、菜单维护、订单接收与状态更新

---

## 技术栈

- 后端：`Spring Boot 3.5` + `Spring Data JPA` + `MySQL`
- 前端：`Vue 3` + `Vite` + `Vue Router` + `Axios`

---

## 目录结构

```text
.
├── backend   # Spring Boot API 服务
└── frontend  # Vue3 页面
```

---

## 本地启动

### 1) 启动后端

先确保本地 MySQL 已启动，并创建数据库：

```sql
CREATE DATABASE IF NOT EXISTS ordering_system
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

默认连接参数（已在 `backend/src/main/resources/application.properties` 中配置）：

- Host: `127.0.0.1`
- Port: `3306`
- User: `root`
- Password: `jianjing@123`

也支持通过环境变量覆盖：

- `MYSQL_HOST`
- `MYSQL_PORT`
- `MYSQL_DB`
- `MYSQL_USER`
- `MYSQL_PASSWORD`

```bash
cd backend
./mvnw spring-boot:run
```

后端默认地址：`http://localhost:8080`

### 2) 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端默认地址：`http://localhost:5173`

> 前端默认调用 `http://localhost:8080`。  
> 如需改后端地址，可设置环境变量：`VITE_API_BASE_URL`

---

## 页面说明

- 前台点餐：`/`
  - 左侧分类切换
  - 中间菜品列表
  - 右侧购物车与下单表单
- 后台管理：`/admin`（未登录会自动跳转 `/admin/login`）
  - 分类管理（增删改）
  - 菜品管理（增删改、上架状态）
  - 订单管理（查看与更新状态）

---

## 主要接口

### 前台

- `GET /api/public/categories` 获取启用分类
- `GET /api/public/menu-items?categoryId=1` 获取菜品
- `POST /api/public/orders` 提交订单

### 后台

- `POST /api/admin/auth/login` 管理员登录，返回 JWT
- `GET /api/admin/auth/me` 获取当前管理员信息
- `GET/POST/PUT/DELETE /api/admin/categories`
- `GET/POST/PUT/DELETE /api/admin/menu-items`
- `GET /api/admin/orders`
- `PUT /api/admin/orders/{id}/status`

---

## 后台登录鉴权（JWT）

后端已启用 JWT 鉴权，`/api/admin/**` 需要管理员权限访问。  
默认管理员账号：

- 用户名：`admin`
- 密码：`admin123456`

可通过环境变量覆盖：

- `ADMIN_USERNAME`
- `ADMIN_PASSWORD`
- `JWT_SECRET`（长度至少 32）
- `JWT_EXPIRE_MINUTES`

---

## 已完成验证

- 后端：`./mvnw test` ✅
- 前端：`npm run build` ✅