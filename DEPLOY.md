# 部署说明（服务器 + 移动端访问）

## 一、前后端一体部署（推荐）

后端与前端打包到同一个服务，同一域名、同一端口，方便移动端直接访问。

### 1. 环境要求

- JDK 17+
- Node.js 18+（仅构建时需要）
- MySQL 8（或兼容版本）

### 2. 构建步骤

```bash
# 1. 构建前端并复制到后端 static 目录
cd frontend
npm ci
npm run build:deploy

# 2. 打包后端（含前端静态资源）
cd ../backend
mvn -q package -DskipTests
```

生成的 jar 位于 `backend/target/backend-0.0.1-SNAPSHOT.jar`。

### 3. 服务器运行

```bash
java -jar backend/target/backend-0.0.1-SNAPSHOT.jar
```

或使用环境变量覆盖配置（生产务必修改）：

```bash
export MYSQL_HOST=你的数据库地址
export MYSQL_PORT=3306
export MYSQL_DB=ordering_system
export MYSQL_USER=root
export MYSQL_PASSWORD=你的密码
export JWT_SECRET=生产环境请使用至少32位随机字符串
export ADMIN_USERNAME=admin
export ADMIN_PASSWORD=强密码
export UPLOAD_DIR=/data/ordering/uploads   # 可选，持久化上传目录

java -jar backend/target/backend-0.0.1-SNAPSHOT.jar
```

### 4. 访问地址

- **前台点餐（含移动端）**：`http://服务器IP或域名:8079/`
- **后台管理**：`http://服务器IP或域名:8079/admin`

同一地址在手机浏览器中打开即可使用；已按移动端分辨率做了适配。

### 5. 端口与反向代理（可选）

- 修改端口：`java -jar ... --server.port=80` 或环境变量 `SERVER_PORT=80`
- 使用 Nginx 反代并配 HTTPS 示例：

```nginx
server {
    listen 80;
    server_name your-domain.com;
    
    # 静态图片由 Nginx 直接提供，不经过后端
    location /uploads/ {
        alias /data/ordering/uploads/;
    }
    
    # API 和前端页面走后端
    location / {
        proxy_pass http://127.0.0.1:8079;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

配置 HTTPS 后，移动端访问 `https://your-domain.com` 即可。

---

## 二、前后端分离部署

若前端与后端不在同一域名（例如前端单独用 CDN），需：

1. **构建前端时指定后端地址**：

```bash
cd frontend
VITE_API_BASE_URL=https://api.your-domain.com npm run build
```

2. **后端 CORS**：当前已允许任意来源（`/api/**`），如需限制可改 `CorsConfig` 的 `allowedOrigins`。

3. 将 `frontend/dist` 部署到任意静态服务器或 CDN，移动端访问该前端地址即可。

---

## 三、图片上传与 Nginx 配置

### 3.1 指定上传路径

上传目录通过 `UPLOAD_DIR` 配置，生产环境建议使用绝对路径：

```bash
export UPLOAD_DIR=/data/ordering/uploads
mkdir -p /data/ordering/uploads
chmod 755 /data/ordering/uploads
```

### 3.2 使用 Nginx 提供图片访问

当使用 Nginx 反向代理时，建议由 Nginx 直接提供 `/uploads/` 静态文件，减轻后端压力：

1. **Nginx 配置**（见上文 5 节）：`location /uploads/ { alias /data/ordering/uploads/; }`
2. **关闭 Spring 静态服务**：`export UPLOAD_SERVE_WITH_SPRING=false`
3. **上传路径与 Nginx alias 一致**：`UPLOAD_DIR=/data/ordering/uploads`

### 3.3 图片访问地址（可选）

- 同域访问：不配置 `UPLOAD_BASE_URL`，返回相对路径 `/uploads/xxx.png`
- 独立域名/CDN：`export UPLOAD_BASE_URL=https://static.your-domain.com`，返回完整 URL
