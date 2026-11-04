package com.fertilizershop.model;
import jakarta.persistence.*;
import java.math.BigDecimal;
@Entity @Table(name="products")
public class Product {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(nullable=false) public String name;
 public String brand;
 public String category;
 public String unit="Bag";
 @Column(nullable=false,precision=12,scale=2) public BigDecimal purchasePrice=BigDecimal.ZERO;
 @Column(nullable=false,precision=12,scale=2) public BigDecimal sellingPrice=BigDecimal.ZERO;
 @Column(nullable=false) public Integer stock=0;
 @Column(nullable=false) public Integer reorderLevel=10;
 public String description;
 public boolean active=true;
}
