package com.example.backend.controller;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.backend.domain.CustomerOrder;
import com.example.backend.domain.CustomerStatus;
import com.example.backend.domain.CustomerUser;
import com.example.backend.domain.MenuItem;
import com.example.backend.dto.CustomerLoginRequest;
import com.example.backend.dto.CustomerLoginResponse;
import com.example.backend.dto.CustomerRegisterRequest;
import com.example.backend.dto.MenuCategoryDto;
import com.example.backend.dto.MenuItemDto;
import com.example.backend.dto.OrderDto;
import com.example.backend.repository.CustomerOrderRepository;
import com.example.backend.repository.CustomerUserRepository;
import com.example.backend.repository.MenuCategoryRepository;
import com.example.backend.repository.MenuItemRepository;
import com.example.backend.security.AppSecurityProperties;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/public")
public class PublicController {

    private final MenuCategoryRepository menuCategoryRepository;
    private final MenuItemRepository menuItemRepository;
    private final CustomerOrderRepository customerOrderRepository;
    private final CustomerUserRepository customerUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtEncoder jwtEncoder;
    private final AppSecurityProperties securityProperties;

    public PublicController(
            MenuCategoryRepository menuCategoryRepository,
            MenuItemRepository menuItemRepository,
            CustomerOrderRepository customerOrderRepository,
            CustomerUserRepository customerUserRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtEncoder jwtEncoder,
            AppSecurityProperties securityProperties) {
        this.menuCategoryRepository = menuCategoryRepository;
        this.menuItemRepository = menuItemRepository;
        this.customerOrderRepository = customerOrderRepository;
        this.customerUserRepository = customerUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtEncoder = jwtEncoder;
        this.securityProperties = securityProperties;
    }

    @PostMapping("/register")
    @Transactional
    public void register(@Valid @RequestBody CustomerRegisterRequest request) {
        if (customerUserRepository.existsByUsername(request.username().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "用户名已存在");
        }
        CustomerUser user = new CustomerUser();
        user.setUsername(request.username().trim());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setPhone(request.phone() != null ? request.phone().trim() : null);
        user.setStatus(CustomerStatus.PENDING_APPROVAL);
        customerUserRepository.save(user);
    }

    @PostMapping("/auth/login")
    public CustomerLoginResponse customerLogin(@Valid @RequestBody CustomerLoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            request.username().trim(),
                            request.password()));
        } catch (DisabledException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "账号待审核，请等待管理员通过");
        } catch (AuthenticationException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }
        List<String> roles = authentication.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .toList();
        if (!roles.contains("CUSTOMER")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "仅点餐用户可在此登录");
        }
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(securityProperties.getJwtExpireMinutes() * 60);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("ordering-system")
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(authentication.getName())
                .claim("roles", roles)
                .build();
        String token = jwtEncoder.encode(
                JwtEncoderParameters.from(
                        JwsHeader.with(MacAlgorithm.HS256).build(),
                        claims))
                .getTokenValue();
        return new CustomerLoginResponse(token, "Bearer", expiresAt.toEpochMilli(), authentication.getName());
    }

    @GetMapping("/categories")
    public List<MenuCategoryDto> listActiveCategories() {
        return menuCategoryRepository.findByActiveTrueOrderBySortOrderAscIdAsc().stream()
                .map(MenuCategoryDto::from)
                .toList();
    }

    @GetMapping("/menu-items")
    public List<MenuItemDto> listAvailableMenuItems(@RequestParam(required = false) Long categoryId) {
        List<MenuItem> menuItems = categoryId == null
                ? menuItemRepository.findAllAvailableItems()
                : menuItemRepository.findAvailableItemsByCategoryId(categoryId);
        return menuItems.stream().map(MenuItemDto::from).toList();
    }

    /** 前台根据订单号 + 手机号查询订单（手机号用于校验身份） */
    @GetMapping("/orders/{id}")
    @Transactional(readOnly = true)
    public OrderDto getOrder(
            @PathVariable Long id,
            @RequestParam String phone) {
        CustomerOrder order = customerOrderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "订单不存在"));
        if (phone == null || !order.getCustomerPhone().trim().equals(phone.trim())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "订单不存在或手机号不匹配");
        }
        return OrderDto.from(order);
    }
}
