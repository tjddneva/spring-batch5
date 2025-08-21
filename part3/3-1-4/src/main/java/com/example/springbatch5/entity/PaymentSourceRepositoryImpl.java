package com.example.springbatch5.entity;

import com.example.springbatch5.support.QuerydslCustomRepositorySupport;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;
import java.util.stream.Collectors;

import static com.example.springbatch5.entity.QPaymentSource.paymentSource;

@Repository
public class PaymentSourceRepositoryImpl extends QuerydslCustomRepositorySupport implements PaymentSourceRepositoryCustom {

    public PaymentSourceRepositoryImpl() {
        super(PaymentSource.class);
    }

    @Override
    public Set<LocalDate> findPaymentDatesByTodayUpdates() {
        return selectFrom(paymentSource)
                .where(paymentSource.updatedAt.between(
                        LocalDate.now().atStartOfDay(),
                        LocalDateTime.of(LocalDate.now(), LocalTime.MAX)
                ))
                .fetch()
                .stream()
                .map(it -> it.getPaymentDateTime().toLocalDate())
                .collect(Collectors.toSet());
    }
}
