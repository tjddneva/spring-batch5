package com.example.springbatch5.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "coupon")
@Getter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "coupon_name", nullable = false)
    private String couponName;

    @Column(name = "discount_amount", nullable = false)
    private BigDecimal discountAmount;

    @Column(name = "expiration_date", nullable = false)
    private LocalDate expirationDate;

    @Column(name = "issued", nullable = false)
    private boolean issued;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    public Coupon(String couponName, BigDecimal discountAmount, LocalDate expirationDate, boolean issued, Long memberId) {
        this.couponName = couponName;
        this.discountAmount = discountAmount;
        this.expirationDate = expirationDate;
        this.issued = issued;
        this.memberId = memberId;
    }
}
