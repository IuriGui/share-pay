package com.example.demo.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter 
@AllArgsConstructor 
@RequiredArgsConstructor 
public class User {
    private String name;
    private String email;
    private String password;
    private String paymentKey;



}
