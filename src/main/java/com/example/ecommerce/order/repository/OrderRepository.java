package com.example.ecommerce.order.repository;

import com.example.ecommerce.order.entity.Order;
import com.example.ecommerce.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Page<Order> findByUser(User user, Pageable pageable);
    Optional<Order> findByIdAndUser(Long id, User user);
}