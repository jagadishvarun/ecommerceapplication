package com.example.ecommerce.payment.repository;

import com.example.ecommerce.order.entity.Order;
import com.example.ecommerce.payment.entity.Payment;
import com.example.ecommerce.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrder(Order order);
    Optional<Payment> findByOrder_IdAndOrder_User(Long orderId, User user);
    Page<Payment> findAllByOrderByCreatedAtDesc(Pageable pageable);
}