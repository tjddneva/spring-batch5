package com.example.springbatch5.entity;

import java.time.LocalDate;
import java.util.Set;

public interface PaymentSourceRepositoryCustom {
    Set<LocalDate> findPaymentDatesByTodayUpdates();
}
