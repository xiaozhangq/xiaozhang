# 微信公众号新订单通知

前台下单成功后，可通过微信公众号向您（管理员）发送模板消息提醒。

## 接口配置信息（URL 与 Token 验证）

申请公众号 / 服务号测试号时，需要填写「接口配置信息」：**URL** 和 **Token**。本系统已提供验证接口。

### 1. 配置 Token

在 `application.properties` 中设置与公众平台一致的 Token（可自定义字符串，如随机英文+数字）：

```properties
app.wechat.verify-token=你自定义的Token字符串
```

或环境变量：`WECHAT_VERIFY_TOKEN=你自定义的Token字符串`

### 2. 填写到微信公众平台

- **URL**：填你的服务器地址 + `/wechat`，且必须为 **HTTPS**（测试号可用 HTTP）。  
  例如：`https://你的域名/wechat`（若后端通过 Nginx 反代，需保证 Nginx 把 `/wechat` 转发到后端 8079）。
- **Token**：与上面 `app.wechat.verify-token` 填写的值 **完全一致**。
- 点击「提交」后，微信会向该 URL 发起 GET 请求并携带 `signature`、`timestamp`、`nonce`、`echostr`，本接口会校验签名并原样返回 `echostr`，通过即配置成功。

### 3. Nginx 转发（若后端被 Nginx 反代）

确保 `/wechat` 会转发到后端，例如在 `location /api/` 同级增加：

```nginx
location /wechat {
    proxy_pass http://127.0.0.1:8079/wechat;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}
```

---

## 前置条件

1. **公众号类型**：需为 **服务号**（订阅号无法主动发模板消息给用户）。
2. **已认证**：服务号需完成微信认证。

## 配置步骤

### 1. 获取 AppID 和 AppSecret

- 登录 [微信公众平台](https://mp.weixin.qq.com/)
- 开发 → 基本配置 → 开发者 ID(AppID)、开发者密码(AppSecret)

### 2. 申请模板消息

- 功能 → 模板消息（或 订阅消息 / 服务号在“功能”里找“模板消息”）
- 从模板库选一个「订单通知」类模板，或自定义模板（需含：订单号、金额、时间等）
- 记下 **模板 ID**（template_id）
- 本系统当前按「收到用户{{userName.DATA}}-{{phone.DATA}}的订单，请及时处理！」这类模板传参，即传 `userName`（下单人姓名）、`phone`（手机号）。若您的模板字段不同，需在 `WeChatNotifyService.sendTemplateMessage` 中调整 data 键名与模板一致。

### 3. 获取您的 openid

- 您需要先 **关注该服务号**
- 获取 openid 方式之一：在公众号后台 用户管理 → 用户列表 中查看（若支持）；或开发一个临时网页/接口，让用户点击授权后即可拿到 openid；或使用微信提供的“接口调试工具”等。
- 将您的 openid 配置到下方 `app.wechat.notify-openids`。

### 4. 配置 application.properties 或环境变量

```properties
# 微信公众号新订单通知
app.wechat.app-id=你的AppID
app.wechat.app-secret=你的AppSecret
app.wechat.template-id=你的模板ID
# 接收通知的 openid，多个用英文逗号分隔
app.wechat.notify-openids=你的openid
```

或使用环境变量：

```bash
export WECHAT_APP_ID=xxx
export WECHAT_APP_SECRET=xxx
export WECHAT_TEMPLATE_ID=xxx
export WECHAT_NOTIFY_OPENIDS=your_openid
```

（若用环境变量，需在 application.properties 中用 `${WECHAT_APP_ID}` 等形式引用。）

## 行为说明

- 仅当配置了 `app.wechat.app-id` 且非空时，才会发送微信模板消息。
- 每次前台下单成功，会向 `notify-openids` 中的每个 openid 发送一条模板消息。
- 发送失败不会影响下单结果，仅会打日志。
