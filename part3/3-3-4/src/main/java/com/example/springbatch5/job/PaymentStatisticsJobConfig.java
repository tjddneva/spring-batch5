package com.example.springbatch5.job;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@AllArgsConstructor
public class PaymentStatisticsJobConfig {

    private final JobRepository jobRepository;
    private final PrepareTargetDatesJobListener prepareTargetDatesJobListener;
    public static final int CHUNK_SIZE = 100;

    /**
     * 일일 결제 통계 데이터를 생성하는 Spring Batch Job을 정의합니다.
     */
    @Bean
    public Job paymentStatisticsJob(Step paymentStatisticsStep) {
        return new JobBuilder("paymentStatisticsJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(prepareTargetDatesJobListener)
                .start(paymentStatisticsStep)
                .build();
    }
}