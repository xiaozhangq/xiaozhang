package com.example.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.backend.domain.DeliveryAddress;

public interface DeliveryAddressRepository extends JpaRepository<DeliveryAddress, Long> {

    List<DeliveryAddress> findByCustomerUser_IdOrderByDefaultAddressDescIdAsc(Long customerUserId);

    void deleteByCustomerUser_IdAndId(Long customerUserId, Long id);
}
