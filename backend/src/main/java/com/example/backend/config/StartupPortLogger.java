package com.example.backend.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * 启动时在控制台明确打印服务端口，便于在 IDE 运行/调试时查看。
 */
@Component
public class StartupPortLogger implements ApplicationRunner {

    private final Environment environment;

    public StartupPortLogger(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        String port = environment.getProperty("server.port", "8080");
        String contextPath = environment.getProperty("server.servlet.context-path", "");
        String base = "http://localhost:" + port + contextPath;
        System.out.println("========================================");
        System.out.println("  后端服务已启动");
        System.out.println("  端口: " + port);
        System.out.println("  本地访问: " + base);
        System.out.println("========================================");
    }
}
