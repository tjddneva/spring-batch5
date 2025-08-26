package com.example.springbatch5.job;

import com.example.springbatch5.entity.PaymentDailyStatistics;
import com.example.springbatch5.entity.PaymentDailyStatisticsUniqueKey;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

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
    private LocalDate paymentDate;

    public PaymentDailyStatisticsUniqueKey toUniqueKey() {
        return new PaymentDailyStatisticsUniqueKey(
                businessRegistrationNumber,
                paymentDate
        );
    }

    public PaymentDailyStatistics toEntity(){
        return new PaymentDailyStatistics(
                corpName,
                businessRegistrationNumber,
                totalAmount,
                paymentDate
        );
    }
}
