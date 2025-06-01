package com.example.springbatch5;

import lombok.AllArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Configuration
@AllArgsConstructor
public class PaymentReportJobConfig {

    @Bean
    public Job paymentReportJob(
            JobRepository jobRepository,
            Step paymentReportStep
    ) {
        return new JobBuilder("paymentReportJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(paymentReportStep)
                .build();
    }


    @Bean
    @JobScope
    public Step paymentReportStep(
            JobRepository jobRepository,
            ItemReader<BigDecimal> paymentItemReader,
            ItemWriter<BigDecimal> paymentItemWriter,
            PlatformTransactionManager transactionManager
    ) {
        return new StepBuilder("paymentReportStep", jobRepository)
                .<BigDecimal, BigDecimal>chunk(5, transactionManager)
                .reader(paymentItemReader)
                .writer(paymentItemWriter)
                .build();
    }

    @Bean
    @StepScope
    public ItemReader<BigDecimal> paymentItemReader(
            @Value("#{jobParameters['targetDate']}") String targetDate
    ) {
        System.out.println("Reader targetDate: " + targetDate);
        return new ListItemReader<>(getPayments());
    }

    @Bean
    @StepScope
    public ItemWriter<BigDecimal> paymentItemWriter(
            @Value("#{jobParameters['targetDate']}") String targetDateStr
    ) {
        return items -> {
            System.out.println("targetDate: " + targetDateStr);
            items.forEach(item -> {
                System.out.println("Payment: " + item);
            });
        };
    }

    private List<BigDecimal> getPayments() {
        return List.of(
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(200),
                BigDecimal.valueOf(300),
                BigDecimal.valueOf(400),
                BigDecimal.valueOf(500),
                BigDecimal.valueOf(600),
                BigDecimal.valueOf(700),
                BigDecimal.valueOf(800),
                BigDecimal.valueOf(900),
                BigDecimal.valueOf(1000),
                BigDecimal.valueOf(1100),
                BigDecimal.valueOf(1200),
                BigDecimal.valueOf(1300),
                BigDecimal.valueOf(1400)
        );
    }
}