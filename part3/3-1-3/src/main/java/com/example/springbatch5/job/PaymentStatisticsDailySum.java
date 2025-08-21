package com.example.springbatch5.job;

import com.example.springbatch5.entity.PaymentDailyStatistics;
import com.example.springbatch5.entity.PaymentDailyStatisticsUniqueKey;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Reader가 SQL 조회 결과를 매핑할 DTO(Data Transfer Object) 클래스입니다.
 */
@Getter
@Setter
@NoArgsConstructor
public class PaymentStatisticsDailySum {
    private BigDecimal totalAmount;
    private String corpName;
    private String businessRegistrationNumber;
    private LocalDateTime paymentDateTime;

    public LocalDate getPaymentDate(){
        return paymentDateTime.toLocalDate();
    }

    public PaymentDailyStatisticsUniqueKey toUniqueKey() {
        return new PaymentDailyStatisticsUniqueKey(
                businessRegistrationNumber,
                getPaymentDate()
        );
    }

    public PaymentDailyStatistics toEntity(){
        return new PaymentDailyStatistics(
                corpName,
                businessRegistrationNumber,
                totalAmount,
                getPaymentDate()
        );
    }
}
