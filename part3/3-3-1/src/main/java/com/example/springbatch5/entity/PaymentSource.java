package com.example.springbatch5.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_source")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 결제 상호명
    @Column(nullable = false, length = 100)
    private String corpName;

    // 결제 사업자 번호
    @Column(nullable = false, length = 100)
    private String businessRegistrationNumber;

    // 결제 금액
    @Column(nullable = false)
    private BigDecimal amount;

    // 결제 일시
    @Column(nullable = false)
    private LocalDateTime paymentDateTime;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public PaymentSource(String corpName, String businessRegistrationNumber, BigDecimal amount, LocalDateTime paymentDateTime) {
        this.corpName = corpName;
        this.businessRegistrationNumber = businessRegistrationNumber;
        this.amount = amount;
        this.paymentDateTime = paymentDateTime;
    }
}