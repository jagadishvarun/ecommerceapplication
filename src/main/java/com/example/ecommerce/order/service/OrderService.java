package com.example.ecommerce.order.service;

import com.example.ecommerce.cart.entity.Cart;
import com.example.ecommerce.cart.entity.CartItem;
import com.example.ecommerce.cart.repository.CartRepository;
import com.example.ecommerce.common.exception.ApiException;
import com.example.ecommerce.order.dto.OrderResponse;
import com.example.ecommerce.order.dto.PlaceOrderRequest;
import com.example.ecommerce.order.dto.UpdateOrderStatusRequest;
import com.example.ecommerce.order.entity.Order;
import com.example.ecommerce.order.entity.OrderItem;
import com.example.ecommerce.order.entity.OrderStatus;
import com.example.ecommerce.order.repository.OrderRepository;
import com.example.ecommerce.product.repository.ProductRepository;
import com.example.ecommerce.user.entity.User;
import com.example.ecommerce.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional
    public OrderResponse placeOrder(String email, PlaceOrderRequest request) {
        User user = findUser(email);
        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new ApiException("Cart is empty", HttpStatus.BAD_REQUEST));

        if (cart.getItems().isEmpty()) {
            throw new ApiException("Cart is empty", HttpStatus.BAD_REQUEST);
        }

        // Decrement stock atomically for each item; fail fast if any item is under-stocked
        for (CartItem cartItem : cart.getItems()) {
            int updated = productRepository.decrementStock(
                    cartItem.getProduct().getId(), cartItem.getQuantity());
            if (updated == 0) {
                throw new ApiException(
                        "Insufficient stock for: " + cartItem.getProduct().getName(),
                        HttpStatus.CONFLICT);
            }
        }

        BigDecimal total = cart.getItems().stream()
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = Order.builder()
                .user(user)
                .totalAmount(total)
                .shippingAddress(request.getShippingAddress())
                .build();

        List<OrderItem> orderItems = cart.getItems().stream()
                .map(cartItem -> OrderItem.builder()
                        .order(order)
                        .productId(cartItem.getProduct().getId())
                        .productName(cartItem.getProduct().getName())
                        .productImageUrl(cartItem.getProduct().getImageUrl())
                        .unitPrice(cartItem.getUnitPrice())
                        .quantity(cartItem.getQuantity())
                        .build())
                .toList();

        order.getItems().addAll(orderItems);
        cart.getItems().clear();
        cartRepository.save(cart);

        return OrderResponse.from(orderRepository.save(order));
    }

    public Page<OrderResponse> getMyOrders(String email, Pageable pageable) {
        User user = findUser(email);
        return orderRepository.findByUser(user, pageable).map(OrderResponse::from);
    }

    public OrderResponse getMyOrder(String email, Long orderId) {
        User user = findUser(email);
        Order order = orderRepository.findByIdAndUser(orderId, user)
                .orElseThrow(() -> new ApiException("Order not found", HttpStatus.NOT_FOUND));
        return OrderResponse.from(order);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Page<OrderResponse> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable).map(OrderResponse::from);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public OrderResponse updateStatus(Long orderId, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ApiException("Order not found", HttpStatus.NOT_FOUND));

        if (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new ApiException(
                    "Cannot update a " + order.getStatus().name().toLowerCase() + " order",
                    HttpStatus.BAD_REQUEST);
        }

        order.setStatus(request.getStatus());
        return OrderResponse.from(orderRepository.save(order));
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
    }
}