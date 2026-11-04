package com.fertilizershop.model;
import jakarta.persistence.*;
@Entity @Table(name="suppliers")
public class Supplier {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(nullable=false) public String name;
 public String phone;
 public String address;
}
