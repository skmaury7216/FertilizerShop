
package com.fertilizershop.model;

import jakarta.persistence.*;

@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false)
    public String name;

    public String phone;

    public String village;

    public String address;

    // Store only the last 4 digits of Aadhaar
    @Column(name = "aadhaar_last4", length = 4)
    public String aadhaarLast4;

    // Display masked Aadhaar on invoice
    @Transient
    public String getMaskedAadhaar() {
        if (aadhaarLast4 == null ||
                !aadhaarLast4.matches("\\d{4}")) {
            return "";
        }

        return "XXXX XXXX " + aadhaarLast4;
    }
}

