package com.example.backend.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.backend.domain.CustomerUser;
import com.example.backend.domain.DeliveryAddress;
import com.example.backend.dto.AddressDto;
import com.example.backend.dto.AddressRequest;
import com.example.backend.repository.CustomerUserRepository;
import com.example.backend.repository.DeliveryAddressRepository;

import org.springframework.http.HttpStatus;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/customer")
public class CustomerAddressController {

    private final CustomerUserRepository customerUserRepository;
    private final DeliveryAddressRepository deliveryAddressRepository;

    public CustomerAddressController(
            CustomerUserRepository customerUserRepository,
            DeliveryAddressRepository deliveryAddressRepository) {
        this.customerUserRepository = customerUserRepository;
        this.deliveryAddressRepository = deliveryAddressRepository;
    }

    private CustomerUser currentUser(Authentication auth) {
        String username = auth != null ? auth.getName() : null;
        if (username == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        return customerUserRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户不存在"));
    }

    @GetMapping("/addresses")
    public List<AddressDto> listAddresses(Authentication auth) {
        CustomerUser user = currentUser(auth);
        return deliveryAddressRepository.findByCustomerUser_IdOrderByDefaultAddressDescIdAsc(user.getId()).stream()
                .map(AddressDto::from)
                .toList();
    }

    @PostMapping("/addresses")
    public AddressDto addAddress(Authentication auth, @Valid @RequestBody AddressRequest request) {
        CustomerUser user = currentUser(auth);
        if (request.defaultAddress()) {
            deliveryAddressRepository.findByCustomerUser_IdOrderByDefaultAddressDescIdAsc(user.getId()).stream()
                    .filter(DeliveryAddress::isDefaultAddress)
                    .forEach(a -> {
                        a.setDefaultAddress(false);
                        deliveryAddressRepository.save(a);
                    });
        }
        DeliveryAddress addr = new DeliveryAddress();
        addr.setCustomerUser(user);
        addr.setReceiverName(request.receiverName().trim());
        addr.setPhone(request.phone().trim());
        addr.setAddress(request.address().trim());
        addr.setDefaultAddress(request.defaultAddress());
        return AddressDto.from(deliveryAddressRepository.save(addr));
    }

    @PutMapping("/addresses/{id}")
    public AddressDto updateAddress(Authentication auth, @PathVariable Long id, @Valid @RequestBody AddressRequest request) {
        CustomerUser user = currentUser(auth);
        DeliveryAddress addr = deliveryAddressRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "地址不存在"));
        if (!addr.getCustomerUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权修改该地址");
        }
        if (request.defaultAddress() && !addr.isDefaultAddress()) {
            deliveryAddressRepository.findByCustomerUser_IdOrderByDefaultAddressDescIdAsc(user.getId()).stream()
                    .filter(DeliveryAddress::isDefaultAddress)
                    .forEach(a -> {
                        a.setDefaultAddress(false);
                        deliveryAddressRepository.save(a);
                    });
        }
        addr.setReceiverName(request.receiverName().trim());
        addr.setPhone(request.phone().trim());
        addr.setAddress(request.address().trim());
        addr.setDefaultAddress(request.defaultAddress());
        return AddressDto.from(deliveryAddressRepository.save(addr));
    }

    @PatchMapping("/addresses/{id}/default")
    public AddressDto setDefaultAddress(Authentication auth, @PathVariable Long id) {
        CustomerUser user = currentUser(auth);
        DeliveryAddress addr = deliveryAddressRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "地址不存在"));
        if (!addr.getCustomerUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权操作该地址");
        }
        deliveryAddressRepository.findByCustomerUser_IdOrderByDefaultAddressDescIdAsc(user.getId()).stream()
                .filter(DeliveryAddress::isDefaultAddress)
                .forEach(a -> {
                    a.setDefaultAddress(false);
                    deliveryAddressRepository.save(a);
                });
        addr.setDefaultAddress(true);
        return AddressDto.from(deliveryAddressRepository.save(addr));
    }

    @DeleteMapping("/addresses/{id}")
    public void deleteAddress(Authentication auth, @PathVariable Long id) {
        CustomerUser user = currentUser(auth);
        DeliveryAddress addr = deliveryAddressRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "地址不存在"));
        if (!addr.getCustomerUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权删除该地址");
        }
        deliveryAddressRepository.delete(addr);
    }
}
