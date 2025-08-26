package com.example.springbatch5;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.Set;


@ToString
@Getter
@Setter
@ConfigurationProperties(prefix = "args")
public class ArgumentProperties {

    /**
     * 결제 일자
     */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate paymentDate;

    /**
     * 데이터 삭제 여부
     */
    private Boolean clearExistingData;

    /**
     * 처리 대상의 결제 일자
     */
    private Set<LocalDate> targetPaymentDates;
}
