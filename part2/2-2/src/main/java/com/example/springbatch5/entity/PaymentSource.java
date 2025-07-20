package com.example.springbatch5.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "payment_source")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String partnerCorpName;

    @Column(nullable = false, length = 100)
    private String partnerBusinessRegistrationNumber;

    // 원래 금액
    @Column(nullable = false)
    private BigDecimal originalAmount;

    // 할인 금액
    @Column(nullable = false)
    private BigDecimal discountAmount;

    // 최종 금액
    @Column(nullable = false)
    private BigDecimal finalAmount;

    // 결제 일자
    @Column(nullable = false)
    private LocalDate paymentDate;
}
