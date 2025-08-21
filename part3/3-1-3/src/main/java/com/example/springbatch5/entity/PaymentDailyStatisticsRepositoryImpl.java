package com.example.springbatch5.entity;

import com.example.springbatch5.support.QuerydslCustomRepositorySupport;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static com.example.springbatch5.entity.QPaymentDailyStatistics.paymentDailyStatistics;

@Repository
public class PaymentDailyStatisticsRepositoryImpl extends QuerydslCustomRepositorySupport implements PaymentDailyStatisticsRepositoryCustom {

    public PaymentDailyStatisticsRepositoryImpl() {
        super(PaymentDailyStatistics.class);
    }

    @Override
    @Transactional
    public long deleteByPaymentDate(LocalDate paymentDate) {
        return delete(paymentDailyStatistics)
                .where(paymentDailyStatistics.paymentDate.eq(paymentDate))
                .execute();
    }

    @Override
    public List<PaymentDailyStatistics> findBy(List<PaymentDailyStatisticsUniqueKey> keys) {
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> businessRegistrationNumbers = keys.stream()
                .map(PaymentDailyStatisticsUniqueKey::getBusinessRegistrationNumber)
                .toList();

        List<LocalDate> paymentDates = keys.stream()
                .map(PaymentDailyStatisticsUniqueKey::getPaymentDate)
                .toList();

        return selectFrom(paymentDailyStatistics)
                .where(paymentDailyStatistics.businessRegistrationNumber.in(businessRegistrationNumbers))
                .where(paymentDailyStatistics.paymentDate.in(paymentDates))
                .fetch();
    }
}
