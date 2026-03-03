package com.example.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.backend.domain.CustomerStatus;
import com.example.backend.domain.CustomerUser;

public interface CustomerUserRepository extends JpaRepository<CustomerUser, Long> {

    Optional<CustomerUser> findByUsername(String username);

    boolean existsByUsername(String username);

    List<CustomerUser> findAllByOrderByIdDesc();

    List<CustomerUser> findByStatusOrderByIdDesc(CustomerStatus status);
}
