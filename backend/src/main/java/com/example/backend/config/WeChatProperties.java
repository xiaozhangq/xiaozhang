package com.example.backend.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 微信公众号配置（用于下单后模板消息通知）。
 * 不配置 app-id 则不发送微信消息。
 */
@ConfigurationProperties(prefix = "app.wechat")
public class WeChatProperties {

    /** 公众号 AppID，为空则禁用微信通知 */
    private String appId = "";
    /** 公众号 AppSecret */
    private String appSecret = "";
    /** 模板消息 ID（在公众平台-模板消息中申请，需包含订单号、金额、时间等字段） */
    private String templateId = "";
    /** 接收新订单通知的 openid，多个用英文逗号分隔（需先关注公众号并获取 openid） */
    private String notifyOpenids = "";
    /** 接口配置信息中的 Token（与公众平台填写的 Token 一致，用于 URL 验证） */
    private String verifyToken = "";

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public void setAppSecret(String appSecret) {
        this.appSecret = appSecret;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public String getNotifyOpenids() {
        return notifyOpenids;
    }

    public void setNotifyOpenids(String notifyOpenids) {
        this.notifyOpenids = notifyOpenids != null ? notifyOpenids : "";
    }

    public String getVerifyToken() {
        return verifyToken;
    }

    public void setVerifyToken(String verifyToken) {
        this.verifyToken = verifyToken != null ? verifyToken : "";
    }

    /** 解析为 openid 列表（逗号分隔） */
    public List<String> getNotifyOpenidList() {
        if (notifyOpenids == null || notifyOpenids.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(notifyOpenids.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    public boolean isEnabled() {
        return appId != null && !appId.isBlank()
                && appSecret != null && !appSecret.isBlank()
                && templateId != null && !templateId.isBlank()
                && !getNotifyOpenidList().isEmpty();
    }
}
