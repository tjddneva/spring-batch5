package com.example.springbatch5.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

public interface PaymentDailyStatisticsRepository extends JpaRepository<PaymentDailyStatistics, Long> {

    @Transactional
    @Modifying
    @Query("delete from PaymentDailyStatistics p where p.paymentDate = :paymentDate")
    int deleteByPaymentDate(@Param("paymentDate") LocalDate paymentDate);
}
