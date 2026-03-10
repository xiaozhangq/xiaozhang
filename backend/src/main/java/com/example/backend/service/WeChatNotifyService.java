package com.example.backend.service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.backend.config.WeChatProperties;
import com.example.backend.dto.OrderDto;

/**
 * 微信公众号模板消息：前台下单后通知管理员。
 * 需在公众平台申请模板（建议包含：first、keyword1 订单号、keyword2 金额、keyword3 时间、remark）。
 * 仅当配置了 app.wechat.app-id 时生效。
 */
@Service
@ConditionalOnProperty(prefix = "app.wechat", name = "app-id", matchIfMissing = false)
public class WeChatNotifyService {

    private static final Logger log = LoggerFactory.getLogger(WeChatNotifyService.class);
    private static final String TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=%s&secret=%s";
    private static final String SEND_URL = "https://api.weixin.qq.com/cgi-bin/message/template/send?access_token=%s";
    private static final Duration TOKEN_EXPIRE_BUFFER = Duration.ofMinutes(5);

    private final WeChatProperties weChat;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String cachedAccessToken;
    private Instant tokenExpireAt;

    public WeChatNotifyService(WeChatProperties weChat) {
        this.weChat = weChat;
        SimpleClientHttpRequestFactory simple = new SimpleClientHttpRequestFactory();
        this.restTemplate = new RestTemplate(new BufferingClientHttpRequestFactory(simple));
    }

    /**
     * 新订单时向配置的 openid 列表发送模板消息。
     */
    public void sendOrderNotify(OrderDto order) {
        if (!weChat.isEnabled()) {
            return;
        }
        List<String> openids = weChat.getNotifyOpenidList();
        if (openids == null || openids.isEmpty()) {
            return;
        }
        String token = getAccessToken();
        if (token == null) {
            log.warn("微信 access_token 获取失败，跳过模板消息");
            return;
        }
        for (String openid : openids) {
            try {
                sendTemplateMessage(token, openid, order);
            } catch (Exception e) {
                log.warn("微信模板消息发送失败 openid={}: {}", openid, e.getMessage());
            }
        }
    }

    /**
     * 发送模板消息。模板示例：收到用户{{userName.DATA}}-{{phone.DATA}}的订单，请及时处理！
     */
    private void sendTemplateMessage(String token, String openid, OrderDto order) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("touser", openid);
        body.put("template_id", weChat.getTemplateId());

        Map<String, Map<String, String>> data = new LinkedHashMap<>();
        data.put("userName", mapValue(truncate(order.customerName(), 20)));
        data.put("phone", mapValue(truncate(order.customerPhone(), 20)));
        body.put("data", data);

        String json = objectMapper.writeValueAsString(body);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setContentLength(bytes.length);
        HttpEntity<byte[]> entity = new HttpEntity<>(bytes, headers);

        String url = String.format(SEND_URL, token);
        @SuppressWarnings("unchecked")
        ResponseEntity<Map> resp = restTemplate.postForEntity(url, entity, Map.class);
        if (resp.getBody() != null) {
            Object errcode = resp.getBody().get("errcode");
            if (errcode != null && Integer.valueOf(0).equals(errcode)) {
                log.debug("微信模板消息已发送 orderId={} openid={}", order.id(), openid);
            } else {
                log.warn("微信模板消息返回异常: {}", resp.getBody());
            }
        }
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return "";
        if (s.length() <= maxLen) return s;
        return s.substring(0, maxLen);
    }

    private static Map<String, String> mapValue(String value) {
        return Map.of("value", value != null ? value : "");
    }

    private synchronized String getAccessToken() {
        if (cachedAccessToken != null && Instant.now().plus(TOKEN_EXPIRE_BUFFER).isBefore(tokenExpireAt)) {
            return cachedAccessToken;
        }
        String url = String.format(TOKEN_URL, weChat.getAppId(), weChat.getAppSecret());
        try {
            @SuppressWarnings("unchecked")
            ResponseEntity<Map> resp = restTemplate.getForEntity(url, Map.class);
            Map<String, Object> body = resp.getBody();
            if (body != null && body.get("access_token") != null) {
                cachedAccessToken = (String) body.get("access_token");
                Object expiresIn = body.get("expires_in");
                int seconds = expiresIn instanceof Number ? ((Number) expiresIn).intValue() : 7200;
                tokenExpireAt = Instant.now().plusSeconds(seconds);
                return cachedAccessToken;
            }
            log.warn("微信 token 接口返回异常: {}", body);
        } catch (Exception e) {
            log.warn("获取微信 access_token 失败: {}", e.getMessage());
        }
        return null;
    }
}
