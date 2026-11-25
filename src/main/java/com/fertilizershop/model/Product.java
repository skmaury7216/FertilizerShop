package com.fertilizershop.model;
import jakarta.persistence.*;
import java.math.BigDecimal;
@Entity @Table(name="products")
public class Product {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(nullable=false) public String name;
 @Column(length=80,unique=true) public String barcode;
 public String brand;
 public String category;
 public String unit="Bag";
 @Column(nullable=false,precision=12,scale=2) public BigDecimal purchasePrice=BigDecimal.ZERO;
 @Column(nullable=false,precision=12,scale=2) public BigDecimal sellingPrice=BigDecimal.ZERO;
 @Column(nullable=false) public Integer stock=0;
 @Column(nullable=false) public Integer reorderLevel=10;
 public String description;
 public boolean active=true;

    // Weight of one full fertilizer bag, in kilograms
    @Column(precision = 10, scale = 3)
    public BigDecimal bagWeightKg;

    // Fertilizer remaining in an opened bag
    @Column(precision = 12, scale = 3)
    public BigDecimal looseStockKg = BigDecimal.ZERO;

    public BigDecimal getAvailableKg() {
        if (bagWeightKg == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal loose = looseStockKg == null
                ? BigDecimal.ZERO : looseStockKg;

        return bagWeightKg.multiply(
                BigDecimal.valueOf(stock)
        ).add(loose);
    }

}
