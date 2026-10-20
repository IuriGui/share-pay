package com.example.demo.model;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;


@Getter 
@Setter 
@AllArgsConstructor 
public class Payment {

    private BigInteger id;
    private BigDecimal amount;
    private String description;
    private User createdBy;
    private List<PaymentUser> payments;
}
