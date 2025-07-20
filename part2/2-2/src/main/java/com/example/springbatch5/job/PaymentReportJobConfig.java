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
    private final int chunkSize = 1_000;

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
//            JpaPagingItemReader<PaymentSource> limitOffsetItemReader
            NoOffsetItemReader<PaymentSource> noOffsetItemReader
    ) {
        return new StepBuilder("paymentReportStep", jobRepository)
                .<PaymentSource, Payment>chunk(chunkSize, transactionManager)
                // Step 소요 시간 측정
                .listener(new StepDurationTrackerListener())
//                .reader(limitOffsetItemReader)
                .reader(noOffsetItemReader)
                .processor(paymentReportProcessor())
                .writer(paymentReportWriter())
                // Chunk 소요 시간 측장
                .listener(new ChunkDurationTrackerListener())
                .build();
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<PaymentSource> limitOffsetItemReader(
            @Value("#{jobParameters['paymentDate']}") LocalDate paymentDate
    ) {
        return new JpaPagingItemReaderBuilder<PaymentSource>()
                .name("paymentSourceItemReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT ps FROM PaymentSource ps WHERE ps.paymentDate = :paymentDate")
                .parameterValues(Collections.singletonMap("paymentDate", paymentDate))
                .pageSize(chunkSize)
                .build();
    }

    /**
     * No Offset 기반의 ItemReader를 생성합니다.
     * 이 리더는 대용량 데이터 처리에 대한 성능 저하를 방지하기 위해 페이지 오프셋을 사용하지 않습니다.
     * 대신, 마지막으로 읽은 항목의 ID를 사용하여 다음 페이지를 조회합니다 (seek-method).
     * 'idExtractor'를 통해 각 항목의 고유 ID를 추출하고, 이를 정렬 및 다음 페이지 조회 조건에 사용합니다.
     *
     * @param paymentDate JobParameter로 전달받은 조회할 결제 날짜
     * @return NoOffsetItemReader 인스턴스
     */
    @Bean
    @StepScope
    public NoOffsetItemReader<PaymentSource> noOffsetItemReader(
            @Value("#{jobParameters['paymentDate']}") LocalDate paymentDate
    ) {
        return new NoOffsetItemReaderBuilder<PaymentSource>()
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT ps FROM PaymentSource ps WHERE ps.paymentDate = :paymentDate ORDER BY ps.id DESC")
                .parameterValues(Collections.singletonMap("paymentDate", paymentDate))
                .chunkSize(chunkSize)
                .name("noOffsetItemReader")
                .idExtractor(PaymentSource::getId)
                .targetType(PaymentSource.class)
                .build();
    }


    private ItemProcessor<PaymentSource, Payment> paymentReportProcessor() {
        return paymentSource -> {
            final Payment payment = new Payment(
                    null,
                    paymentSource.getFinalAmount(),
                    paymentSource.getPaymentDate(),
                    "partnerCorpName",
                    "PAYMENT"
            );
//            log.info("Processor payment: {}", payment);
            return payment;
        };
    }

    @Bean
    public ItemWriter<Payment> paymentReportWriter() {
        return chunk -> {

        };
    }
}