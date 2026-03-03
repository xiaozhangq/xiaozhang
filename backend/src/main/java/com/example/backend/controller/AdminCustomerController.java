package com.example.backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.backend.domain.CustomerStatus;
import com.example.backend.domain.CustomerUser;
import com.example.backend.dto.CustomerDto;
import com.example.backend.dto.UpdateCustomerStatusRequest;
import com.example.backend.repository.CustomerUserRepository;

import org.springframework.http.HttpStatus;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/customers")
public class AdminCustomerController {

    private final CustomerUserRepository customerUserRepository;

    public AdminCustomerController(CustomerUserRepository customerUserRepository) {
        this.customerUserRepository = customerUserRepository;
    }

    @GetMapping
    public List<CustomerDto> listCustomers(@RequestParam(required = false) CustomerStatus status) {
        if (status != null) {
            return customerUserRepository.findByStatusOrderByIdDesc(status).stream()
                    .map(CustomerDto::from)
                    .toList();
        }
        return customerUserRepository.findAllByOrderByIdDesc().stream()
                .map(CustomerDto::from)
                .toList();
    }

    @PatchMapping("/{id}/status")
    public CustomerDto updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateCustomerStatusRequest request) {
        CustomerUser user = customerUserRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));
        if (request.status() != CustomerStatus.APPROVED && request.status() != CustomerStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "仅支持审核通过或拒绝");
        }
        user.setStatus(request.status());
        return CustomerDto.from(customerUserRepository.save(user));
    }
}
