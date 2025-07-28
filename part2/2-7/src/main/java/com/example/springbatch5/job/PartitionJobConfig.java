package com.example.springbatch5.job;

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
import org.springframework.batch.item.database.JpaCursorItemReader;
import org.springframework.batch.item.database.builder.JpaCursorItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;

/**
 * Spring Batch 파티셔닝을 사용하여 특정 기간의 결제 데이터를 병렬로 처리하는 Job 설정 클래스입니다.
 * JobParameter로 `start_date`와 `end_date`를 받아, 해당 기간의 각 날짜에 대한 파티션을 생성하고 병렬로 처리합니다.
 */
@Slf4j
@Configuration
@AllArgsConstructor
public class PartitionJobConfig {
    private final EntityManagerFactory entityManagerFactory;
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final int chunkSize = 1_000;

    /**
     * 파티셔닝을 사용하는 Job을 생성합니다.
     *
     * @param managerStep 파티션을 관리하는 매니저 스텝
     * @return Job
     */
    @Bean
    public Job partitionerJob(Step managerStep) {
        return new JobBuilder("partitionerJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(managerStep)
                .build();
    }

    /**
     * 파티션을 생성하고 각 파티션을 워커 스텝에 위임하는 매니저 스텝을 생성합니다.
     *
     * @param workerStep 각 파티션에서 실행될 워커 스텝
     * @return Step
     */
    @Bean
    public Step managerStep(Step workerStep) {
        return new StepBuilder("managerStep", jobRepository)
                // "workerStep"이라는 이름으로 파티셔너를 설정합니다.
                .partitioner("workerStep", partitioner(null, null))
                // 각 파티션에서 실행될 스텝을 지정합니다.
                .step(workerStep)
                // 동시에 실행할 파티션의 수를 설정합니다.
                .gridSize(31)
                // 파티션을 병렬로 처리하기 위한 TaskExecutor를 설정합니다.
                .taskExecutor(taskExecutor())
                .build();
    }

    /**
     * JobParameter로 받은 시작일과 종료일을 사용하여 LocalDatePartitioner를 생성합니다.
     *
     * @param startDate JobParameter로 전달되는 시작일 문자열 (yyyy-MM-dd 형식)
     * @param endDate   JobParameter로 전달되는 종료일 문자열 (yyyy-MM-dd 형식)
     * @return LocalDatePartitioner
     */
    @Bean
    @StepScope
    public LocalDatePartitioner partitioner(
            @Value("#{jobParameters['startDate']}") LocalDate startDate,
            @Value("#{jobParameters['endDate']}") LocalDate endDate
    ) {
        return new LocalDatePartitioner(startDate, endDate);
    }

    /**
     * 파티션 처리를 위한 스레드 풀을 생성합니다.
     *
     * @return TaskExecutor
     */
    @Bean
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(31); // 기본 스레드 수
        taskExecutor.setMaxPoolSize(50);  // 최대 스레드 수
        taskExecutor.setThreadNamePrefix("partition-thread-"); // 스레드 이름 접두사
        taskExecutor.setWaitForTasksToCompleteOnShutdown(true); // 종료 시 모든 태스크가 완료될 때까지 대기
        taskExecutor.initialize();
        return taskExecutor;
    }

    /**
     * 각 파티션에서 실제 데이터 처리를 수행하는 워커 스텝을 생성합니다.
     *
     * @param cursorItemReader 각 파티션의 데이터를 읽는 JpaCursorItemReader
     * @return Step
     */
    @Bean
    public Step workerStep(
            JpaCursorItemReader<PaymentSource> cursorItemReader
    ) {
        return new StepBuilder("workerStep", jobRepository)
                .<PaymentSource, Payment>chunk(chunkSize, transactionManager)
                .listener(new StepDurationTrackerListener())
                .reader(cursorItemReader)
                .processor(paymentReportProcessor())
                .writer(paymentReportWriter())
                .listener(new ChunkDurationTrackerListener())
                .build();
    }

    /**
     * StepExecutionContext로부터 `paymentDate`를 주입받아 특정 날짜의 결제 원천 데이터를 조회하는 JpaCursorItemReader를 생성합니다.
     *
     * @param paymentDateStr StepExecutionContext에서 전달되는 결제일 문자열 (yyyy-MM-dd 형식)
     * @return JpaCursorItemReader<PaymentSource>
     */
    @Bean
    @StepScope
    public JpaCursorItemReader<PaymentSource> cursorItemReader(
            @Value("#{stepExecutionContext['paymentDate']}") String paymentDateStr
    ) {
        LocalDate paymentDate = LocalDate.parse(paymentDateStr, DateTimeFormatter.ISO_LOCAL_DATE);
        return new JpaCursorItemReaderBuilder<PaymentSource>()
                .name("cursorItemReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT ps FROM PaymentSource ps WHERE ps.paymentDate = :paymentDate")
                .parameterValues(Collections.singletonMap("paymentDate", paymentDate))
                .build();
    }

    /**
     * PaymentSource를 Payment로 변환하는 ItemProcessor를 생성합니다.
     *
     * @return ItemProcessor<PaymentSource, Payment>
     */
    private ItemProcessor<PaymentSource, Payment> paymentReportProcessor() {
        return paymentSource -> new Payment(
                null,
                paymentSource.getFinalAmount(),
                paymentSource.getPaymentDate(),
                "partnerCorpName",
                "PAYMENT"
        );
    }

    /**
     * 처리된 Payment 데이터를 기록하는 ItemWriter를 생성합니다. (현재는 비어 있음)
     *
     * @return ItemWriter<Payment>
     */

    private ItemWriter<Payment> paymentReportWriter() {
        return chunk -> {

            var item = chunk.getItems().stream().findFirst().get();
            System.out.println(item.getPaymentDate());

        };
    }
}
