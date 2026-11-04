package com.fertilizershop.model;
import jakarta.persistence.*;
@Entity @Table(name="customers")
public class Customer {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(nullable=false) public String name;
 public String phone;
 public String village;
 public String address;
}
