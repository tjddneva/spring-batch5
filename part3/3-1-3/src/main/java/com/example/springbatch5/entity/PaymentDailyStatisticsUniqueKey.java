package com.example.springbatch5.entity;


import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
@EqualsAndHashCode
public class PaymentDailyStatisticsUniqueKey {
    private String businessRegistrationNumber;
    private LocalDate paymentDate;
}
