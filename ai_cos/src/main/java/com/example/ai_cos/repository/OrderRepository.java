package com.example.ai_cos.repository;

import com.example.ai_cos.model.Order;
import com.example.ai_cos.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByClientId(Long clientId);
    List<Order> findByStatus(OrderStatus status);
    
    List<Order> findByProductNameContainingIgnoreCase(String productName);
    
    @Query("SELECT SUM(o.price * o.quantity) FROM Order o WHERE o.client.id = :clientId")
    BigDecimal getTotalSpentByClient(Long clientId);
    
    @Query("SELECT COUNT(o) FROM Order o WHERE o.client.id = :clientId")
    Long countOrdersByClient(Long clientId);
    
    @Query("SELECT SUM(o.price * o.quantity) FROM Order o")
    BigDecimal getTotalRevenue();
    
    @Query("SELECT AVG(o.price * o.quantity) FROM Order o")
    BigDecimal getAverageOrderValue();
}
