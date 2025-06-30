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
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment")
@Getter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 결제 금액
     */
    @Column(nullable = false)
    private BigDecimal amount;

    /**
     * 결제일
     */
    @Column(nullable = false)
    private LocalDate paymentDate;

    /**
     * 결제 상태, 취소, 부분 취소
     */
    @Column(nullable = false, length = 50)
    private String status;
}