package com.fertilizershop.model;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;
@Entity @Table(name="stock_purchases")
public class StockPurchase {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @ManyToOne(optional=false) public Product product;
 @ManyToOne public Supplier supplier;
 public Integer quantity;
 @Column(precision=12,scale=2) public BigDecimal unitCost;
 @Column(precision=12,scale=2) public BigDecimal totalCost;
 public LocalDateTime purchasedAt;
 public String reference;
}
