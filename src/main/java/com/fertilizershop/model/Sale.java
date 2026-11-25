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
 @Column(length=36) public String billGroup; // null for legacy single-item invoices
 public BigDecimal getDue(){return total.subtract(paid);}
 public String getPaymentStatus(){return getDue().signum()==0?"Paid":(paid.signum()==0?"Unpaid":"Partial");}

    // New weight-based sales only.
    // Null for historical bag-based sales.
    @Column(precision = 12, scale = 3)
    public BigDecimal weightKg;

    @Column(length = 10)
    public String weightUnit;

    @Column(precision = 12, scale = 2)
    public BigDecimal pricePerKg;

    public String getDisplayQuantity() {
        if (weightKg == null) {
            return quantity + " " +
                    (product.unit == null ? "Unit" : product.unit);
        }

        return weightKg.stripTrailingZeros()
                .toPlainString() + " kg";
    }

}
