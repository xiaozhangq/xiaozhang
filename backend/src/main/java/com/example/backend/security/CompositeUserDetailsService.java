package com.example.backend.security;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.backend.domain.CustomerStatus;
import com.example.backend.domain.CustomerUser;
import com.example.backend.repository.CustomerUserRepository;

public class CompositeUserDetailsService implements UserDetailsService {

    private final AppSecurityProperties properties;
    private final PasswordEncoder passwordEncoder;
    private final CustomerUserRepository customerUserRepository;

    public CompositeUserDetailsService(
            AppSecurityProperties properties,
            PasswordEncoder passwordEncoder,
            CustomerUserRepository customerUserRepository) {
        this.properties = properties;
        this.passwordEncoder = passwordEncoder;
        this.customerUserRepository = customerUserRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (username != null && username.trim().equals(properties.getAdminUsername())) {
            return User.builder()
                    .username(properties.getAdminUsername())
                    .password(passwordEncoder.encode(properties.getAdminPassword()))
                    .roles("ADMIN")
                    .build();
        }
        CustomerUser customer = customerUserRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));
        boolean enabled = customer.getStatus() == CustomerStatus.APPROVED;
        return User.builder()
                .username(customer.getUsername())
                .password(customer.getPassword())
                .roles("CUSTOMER")
                .disabled(!enabled)
                .build();
    }
}
