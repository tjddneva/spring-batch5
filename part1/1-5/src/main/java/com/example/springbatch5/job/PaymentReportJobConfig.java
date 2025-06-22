package com.example.springbatch5.job;

import com.example.springbatch5.entity.InvalidPaymentAmountException;
import com.example.springbatch5.entity.Payment;
import com.example.springbatch5.entity.PaymentSource;
import jakarta.persistence.EntityManagerFactory;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.skip.AlwaysSkipItemSkipPolicy;
import org.springframework.batch.core.step.skip.LimitCheckingItemSkipPolicy;
import org.springframework.batch.core.step.skip.NeverSkipItemSkipPolicy;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;

@Slf4j
@Configuration
@AllArgsConstructor
public class PaymentReportJobConfig {

    private final EntityManagerFactory entityManagerFactory;
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;


    @Bean
    public Job paymentReportJob(
            Step paymentReportStep
    ) {
        return new JobBuilder("paymentReportJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(paymentReportStep)
                .build();
    }

    @Bean
    public Step paymentReportStep(
            JpaPagingItemReader<PaymentSource> paymentReportReader
    ) {
        // FaultTolerantStepBuilder 을 통해 기본 정책 할당, 기본 Policy 정책, Skip limit
        // FaultTolerantChunkProcessor 실질적으로 폴트 톨러런스 내결함 성의 관한 내용이 동작합니다.
        return new StepBuilder("paymentReportStep", jobRepository)
                .<PaymentSource, Payment>chunk(10, transactionManager)
                .reader(paymentReportReader)
                .processor(paymentReportProcessor())
                .writer(paymentReportWriter())
                .faultTolerant()
                .skip(InvalidPaymentAmountException.class) // InvalidPaymentAmountException 예외 발생 시 skip
                .skipLimit(2) // 최대 2번까지 skip 허용
//                .skipPolicy(new LimitCheckingItemSkipPolicy())
//                .skipPolicy(new LimitCheckingItemSkipPolicy(
//                        10, // 최대 10번까지 skip 허용
//                        throwable -> { // 예외에 따라 skip 여부 결정
//                            if (throwable instanceof InvalidPaymentAmountException) {
//                                return true; // InvalidPaymentAmountException 발생 시 skip
//                            } else if (throwable instanceof IllegalStateException) {
//                                return false; // IllegalStateException 발생 시 skip하지 않음
//                            } else {
//                                return false; // 그 외 예외는 skip하지 않음
//                            }
//                        }
//                ))
//                .skipPolicy(new AlwaysSkipItemSkipPolicy()) // 항상 skip하는 정책
//                .skipPolicy(new NeverSkipItemSkipPolicy()) // 절대 skip하지 않는 정책
                .build();
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<PaymentSource> paymentReportReader(
            @Value("#{jobParameters['paymentDate']}") LocalDate paymentDate
    ) {
        return new JpaPagingItemReaderBuilder<PaymentSource>()
                .name("paymentSourceItemReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT ps FROM PaymentSource ps WHERE ps.paymentDate = :paymentDate")
                .parameterValues(Collections.singletonMap("paymentDate", paymentDate))
                .pageSize(10)
                .build();
    }

    private ItemProcessor<PaymentSource, Payment> paymentReportProcessor() {
        return paymentSource -> {
            // 할인 금액으로 최종 금액이 0원이 케이스 제외
//            if (paymentSource.getFinalAmount().compareTo(BigDecimal.ZERO) == 0) {
//                return null;
//            }

            // 할인 금액이 0이 아닌 경우(양수 음수)
            if (paymentSource.getDiscountAmount().signum() == -1) {
                // 할인 금액이 0이 아닌 경우의 처리 로직
                final String msg = "할인 금액이 0이 아닌 결제는 처리할 수 없습니다. 현재 할인 금액: " + paymentSource.getDiscountAmount();
                log.error(msg);
                throw new InvalidPaymentAmountException(msg);
            }

            final Payment payment = new Payment(
                    null,
                    paymentSource.getFinalAmount(),
                    paymentSource.getPaymentDate(),
                    "PAYMENT"
            );

            log.info("Processor payment: {}", payment);
            return payment;
        };
    }

    @Bean
    public ItemWriter<Payment> paymentReportWriter() {
        return chunk -> {
            for (Payment payment : chunk) {
                log.info("Writer payment: {}", payment);
            }
        };
    }
}

