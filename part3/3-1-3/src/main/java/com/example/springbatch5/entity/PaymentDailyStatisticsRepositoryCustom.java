package com.example.springbatch5.entity;

import java.time.LocalDate;
import java.util.List;

public interface PaymentDailyStatisticsRepositoryCustom {
    long deleteByPaymentDate(LocalDate paymentDate);

    List<PaymentDailyStatistics> findBy(List<PaymentDailyStatisticsUniqueKey> keys);
}
