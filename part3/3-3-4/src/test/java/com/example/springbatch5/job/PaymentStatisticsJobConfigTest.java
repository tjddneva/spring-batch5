package com.example.springbatch5.job;

import com.example.springbatch5.SpringBatchTestSupport;
import com.example.springbatch5.entity.PaymentDailyStatistics;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.List;

import static com.example.springbatch5.entity.QPaymentDailyStatistics.paymentDailyStatistics;
import static org.assertj.core.api.BDDAssertions.fail;
import static org.assertj.core.api.BDDAssertions.then;

@TestPropertySource(properties = {"args.payment-date=2025-01-05"})
class PaymentStatisticsJobConfigTest extends SpringBatchTestSupport {

    @Test
    @SqlGroup({
            @Sql(value = "/sql/payment-source-setup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD),
            @Sql(value = "/sql/payment-source-cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    })
    void paymentStatisticsJob_test() throws Exception {
        // given

        // when
        launchJob(paymentStatisticsJob);

        // then
        thenBatchCompleted();
        final List<PaymentDailyStatistics> dailyStatistics = query.selectFrom(paymentDailyStatistics)
                .where(paymentDailyStatistics.paymentDate.eq(properties.getPaymentDate()))
                .fetch();

        then(dailyStatistics).hasSize(2);
        then(dailyStatistics).allSatisfy(dailySum -> {
            switch (dailySum.getBusinessRegistrationNumber()) {
                case "10002000" -> {
                    then(dailySum.getCorpName()).isEqualTo("사업자1");
                    then(dailySum.getAmount()).isEqualByComparingTo(new BigDecimal("400"));
                }
                case "2002231" -> {
                    then(dailySum.getCorpName()).isEqualTo("사업자2");
                    then(dailySum.getAmount()).isEqualByComparingTo(new BigDecimal("1000"));
                }
                default -> fail("예상치 못한 사업자 번호입니다: " + dailySum.getBusinessRegistrationNumber());
            }
        });
    }
}