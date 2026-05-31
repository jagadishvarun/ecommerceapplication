package com.example.ecommerce.payment.service;

import com.example.ecommerce.common.exception.ApiException;
import com.example.ecommerce.order.entity.Order;
import com.example.ecommerce.order.entity.OrderStatus;
import com.example.ecommerce.order.repository.OrderRepository;
import com.example.ecommerce.payment.dto.PaymentRequest;
import com.example.ecommerce.payment.dto.PaymentResponse;
import com.example.ecommerce.payment.entity.Payment;
import com.example.ecommerce.payment.entity.PaymentStatus;
import com.example.ecommerce.payment.repository.PaymentRepository;
import com.example.ecommerce.user.entity.User;
import com.example.ecommerce.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @Transactional
    public PaymentResponse pay(String email, PaymentRequest request) {
        User user = findUser(email);

        Order order = orderRepository.findByIdAndUser(request.getOrderId(), user)
                .orElseThrow(() -> new ApiException("Order not found", HttpStatus.NOT_FOUND));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new ApiException(
                    "Order cannot be paid — current status: " + order.getStatus().name().toLowerCase(),
                    HttpStatus.BAD_REQUEST);
        }

        paymentRepository.findByOrder(order).ifPresent(existing -> {
            if (existing.getStatus() != PaymentStatus.FAILED) {
                throw new ApiException("A payment already exists for this order", HttpStatus.CONFLICT);
            }
        });

        Payment payment = Payment.builder()
                .order(order)
                .amount(order.getTotalAmount())
                .paymentMethod(request.getPaymentMethod())
                .status(PaymentStatus.COMPLETED)
                .transactionId(UUID.randomUUID().toString())
                .build();

        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);

        return PaymentResponse.from(paymentRepository.save(payment));
    }

    public PaymentResponse getPaymentForOrder(String email, Long orderId) {
        User user = findUser(email);
        return paymentRepository.findByOrder_IdAndOrder_User(orderId, user)
                .map(PaymentResponse::from)
                .orElseThrow(() -> new ApiException("Payment not found", HttpStatus.NOT_FOUND));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Page<PaymentResponse> getAllPayments(Pageable pageable) {
        return paymentRepository.findAllByOrderByCreatedAtDesc(pageable).map(PaymentResponse::from);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public PaymentResponse refund(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ApiException("Payment not found", HttpStatus.NOT_FOUND));

        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new ApiException(
                    "Only completed payments can be refunded",
                    HttpStatus.BAD_REQUEST);
        }

        payment.setStatus(PaymentStatus.REFUNDED);
        payment.getOrder().setStatus(OrderStatus.CANCELLED);
        orderRepository.save(payment.getOrder());

        return PaymentResponse.from(paymentRepository.save(payment));
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
    }
}