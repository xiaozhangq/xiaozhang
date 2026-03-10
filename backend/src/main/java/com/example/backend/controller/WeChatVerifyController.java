package com.example.backend.controller;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.config.WeChatProperties;

/**
 * 微信公众号 / 服务号测试号「接口配置信息」验证接口。
 * 在公众平台填写 URL 和 Token 后，微信会 GET 此地址并携带 signature、timestamp、nonce、echostr，
 * 校验通过后需原样返回 echostr。
 * <p>
 * 配置示例：URL 填 https://你的域名/wechat ，Token 填与 app.wechat.verify-token 一致的值。
 */
@RestController
@RequestMapping("/wechat")
public class WeChatVerifyController {

    private final WeChatProperties weChat;

    public WeChatVerifyController(WeChatProperties weChat) {
        this.weChat = weChat;
    }

    @GetMapping(produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> verify(
            @RequestParam(value = "signature", required = false) String signature,
            @RequestParam(value = "timestamp", required = false) String timestamp,
            @RequestParam(value = "nonce", required = false) String nonce,
            @RequestParam(value = "echostr", required = false) String echostr) {

        String token = weChat.getVerifyToken();
        if (token == null || token.isBlank()) {
            return ResponseEntity.status(503)
                    .body("请先配置 app.wechat.verify-token（与公众平台填写的 Token 一致）");
        }
        if (signature == null || timestamp == null || nonce == null || echostr == null) {
            return ResponseEntity.badRequest().body("缺少 signature/timestamp/nonce/echostr 参数");
        }

        String computed = sha1Hex(token, timestamp, nonce);
        if (computed.equalsIgnoreCase(signature)) {
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(echostr);
        }
        return ResponseEntity.badRequest().body("signature 校验失败");
    }

    private static String sha1Hex(String token, String timestamp, String nonce) {
        String combined = Arrays.asList(token, timestamp, nonce).stream()
                .sorted()
                .collect(Collectors.joining());
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(combined.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(40);
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-1 不可用", e);
        }
    }
}
