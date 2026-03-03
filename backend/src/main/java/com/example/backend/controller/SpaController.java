package com.example.backend.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 前后端一体部署时，将前台的 SPA 路由指向 index.html，由前端路由接管。
 */
@Controller
public class SpaController {

    @GetMapping(value = { "/admin", "/admin/login", "/admin/categories", "/admin/orders", "/admin/menu", "/admin/customers", "/login" })
    public String admin() {
        return "forward:/index.html";
    }
}
