package com.example.springbatch5.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_daily_statistics",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {
                                "businessRegistrationNumber",
                                "paymentDate"
                        }
                )
        }
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDailyStatistics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 결제 상호명
    @Column(nullable = false, length = 100, updatable = false)
    private String corpName;

    // 결제 사업자 번호
    @Column(nullable = false, length = 100, updatable = false)
    private String businessRegistrationNumber;

    // 결제 금액
    @Column(nullable = false)
    private BigDecimal amount;

    // 결제 일자
    @Column(nullable = false, updatable = false)
    private LocalDate paymentDate;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public PaymentDailyStatistics(String corpName, String businessRegistrationNumber, BigDecimal amount, LocalDate paymentDate) {
        this.corpName = corpName;
        this.businessRegistrationNumber = businessRegistrationNumber;
        this.amount = amount;
        this.paymentDate = paymentDate;
    }

    public void updateAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public PaymentDailyStatisticsUniqueKey toUniqueKey() {
        return new PaymentDailyStatisticsUniqueKey(businessRegistrationNumber, paymentDate);
    }
}
