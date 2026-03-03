package com.example.backend.dto;

import com.example.backend.domain.DeliveryAddress;

public record AddressDto(
        Long id,
        String receiverName,
        String phone,
        String address,
        boolean defaultAddress
) {
    public static AddressDto from(DeliveryAddress a) {
        return new AddressDto(
                a.getId(),
                a.getReceiverName(),
                a.getPhone(),
                a.getAddress(),
                a.isDefaultAddress()
        );
    }
}
