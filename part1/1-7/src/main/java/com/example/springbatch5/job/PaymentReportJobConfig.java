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
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

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
                .listener(new JobDurationTrackerListener())
                .incrementer(new RunIdIncrementer())
                .start(paymentReportStep)
                .build();
    }

    @Bean
    public Step paymentReportStep(
            JpaPagingItemReader<PaymentSource> paymentReportReader
    ) {
        return new StepBuilder("paymentReportStep", jobRepository)
                .listener(new StepDurationTrackerListener())
                .<PaymentSource, Payment>chunk(10, transactionManager)
                .reader(paymentReportReader)
                .processor(paymentReportProcessor())
                .writer(paymentReportWriter())
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

