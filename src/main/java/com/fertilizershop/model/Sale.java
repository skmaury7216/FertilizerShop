package com.fertilizershop.model;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Entity @Table(name="sales")
public class Sale {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @ManyToOne(optional=false) public Product product;
 @ManyToOne(optional=false) public Customer customer;
 public Integer quantity;
 @Column(precision=12,scale=2) public BigDecimal unitPrice;
 @Column(precision=12,scale=2) public BigDecimal total;
 @Column(precision=12,scale=2) public BigDecimal paid=BigDecimal.ZERO;
 public LocalDateTime createdAt;
 public String notes;
 public BigDecimal getDue(){return total.subtract(paid);}
 public String getPaymentStatus(){return getDue().signum()==0?"Paid":(paid.signum()==0?"Unpaid":"Partial");}
}
