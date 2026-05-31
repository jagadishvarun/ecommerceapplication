package com.example.ecommerce.order.dto;

import com.example.ecommerce.order.entity.ShippingAddress;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PlaceOrderRequest {

    @NotNull
    @Valid
    private ShippingAddress shippingAddress;
}