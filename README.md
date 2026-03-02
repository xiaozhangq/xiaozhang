# SpringBoot + Vue3 点餐系统

一个前后端分离的简易点餐项目，包含：

- **前台点餐端**：菜单分类、菜品展示、购物车、提交订单
- **后台管理端**：分类管理、菜单维护、订单接收与状态更新

---

## 技术栈

- 后端：`Spring Boot 3.5` + `Spring Data JPA` + `H2`
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

```bash
cd backend
./mvnw spring-boot:run
```

后端默认地址：`http://localhost:8080`

可选：H2 控制台（调试用）  
`http://localhost:8080/h2-console`  
JDBC URL: `jdbc:h2:mem:ordering`，用户名 `sa`，密码为空

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
- 后台管理：`/admin`
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

- `GET/POST/PUT/DELETE /api/admin/categories`
- `GET/POST/PUT/DELETE /api/admin/menu-items`
- `GET /api/admin/orders`
- `PUT /api/admin/orders/{id}/status`

---

## 已完成验证

- 后端：`./mvnw test` ✅
- 前端：`npm run build` ✅