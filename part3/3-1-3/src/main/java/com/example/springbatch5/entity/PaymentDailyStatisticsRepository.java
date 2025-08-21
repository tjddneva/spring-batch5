package com.example.springbatch5.entity;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentDailyStatisticsRepository extends JpaRepository<PaymentDailyStatistics, Long>, PaymentDailyStatisticsRepositoryCustom {
}
