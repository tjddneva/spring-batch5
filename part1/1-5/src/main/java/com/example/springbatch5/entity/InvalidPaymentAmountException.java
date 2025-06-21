package com.example.springbatch5.entity;

public class InvalidPaymentAmountException extends RuntimeException {

    public InvalidPaymentAmountException(String msg) {
        super(msg);
    }
}
