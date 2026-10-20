package com.example.demo.model;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.Timestamp;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter 
@NoArgsConstructor 
@Setter 
@AllArgsConstructor 
public class PaymentUser {

    private BigInteger payment_id;
    private BigInteger user_id;
    private BigDecimal amount;
    private boolean is_paid;
    private Timestamp created_at;
    private Timestamp paid_at;

    

}
