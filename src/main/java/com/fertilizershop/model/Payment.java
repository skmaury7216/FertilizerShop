package com.fertilizershop.model;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Entity @Table(name="payments")
public class Payment {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @ManyToOne(optional=false) public Sale sale;
 @Column(nullable=false,precision=12,scale=2) public BigDecimal amount;
 public String method;
 public String reference;
 public LocalDateTime paidAt;
}
