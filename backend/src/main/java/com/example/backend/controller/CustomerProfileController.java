package com.example.backend.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.backend.domain.CustomerUser;
import com.example.backend.dto.CustomerProfileResponse;
import com.example.backend.repository.CustomerUserRepository;

import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/customer")
public class CustomerProfileController {

    private final CustomerUserRepository customerUserRepository;

    public CustomerProfileController(CustomerUserRepository customerUserRepository) {
        this.customerUserRepository = customerUserRepository;
    }

    @GetMapping("/me")
    public CustomerProfileResponse me(Authentication auth) {
        String username = auth != null ? auth.getName() : null;
        if (username == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        CustomerUser user = customerUserRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户不存在"));
        return new CustomerProfileResponse(user.getId(), user.getUsername(), user.getPhone());
    }
}
