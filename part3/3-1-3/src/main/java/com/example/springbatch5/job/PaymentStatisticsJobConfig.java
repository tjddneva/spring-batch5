package com.example.springbatch5.job;

import com.example.springbatch5.ArgumentProperties;
import com.example.springbatch5.service.PaymentDailyStatisticsRecoveryService;
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
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.List;

@Slf4j
@Configuration
@AllArgsConstructor
public class PaymentStatisticsJobConfig {

    private final EntityManagerFactory entityManagerFactory;
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final DataSource dataSource;
    private final ArgumentProperties properties;
    private final ClearExistingDataJobListener clearExistingDataJobListener;
    private final PaymentDailyStatisticsRecoveryService paymentDailyStatisticsRecoveryService;

    private final int chunkSize = 100;

    /**
     * 일일 결제 통계 데이터를 생성하는 Spring Batch Job을 정의합니다.
     */
    @Bean
    public Job paymentStatisticsJob(Step paymentStatisticsStep) {
        return new JobBuilder("paymentStatisticsJob", jobRepository)
                .incrementer(new RunIdIncrementer())
//                .listener(clearExistingDataJobListener)
                .start(paymentStatisticsStep)
                .build();
    }

    /**
     * Job의 핵심 로직을 담당하는 Step을 정의합니다.
     * Reader, Processor, Writer를 사용하여 데이터를 읽고, 가공하고, 저장합니다.
     */
    @Bean
    public Step paymentStatisticsStep(
            JdbcCursorItemReader<PaymentStatisticsDailySum> paymentStatisticsReader,
            ItemWriter<PaymentStatisticsDailySum> paymentStatisticsWriter
    ) {
        return new StepBuilder("paymentStatisticsStep", jobRepository)
                .<PaymentStatisticsDailySum, PaymentStatisticsDailySum>chunk(chunkSize, transactionManager)
                .listener(new StepDurationTrackerListener()) // Step 소요 시간 측정 리스너
                .reader(paymentStatisticsReader)
                .writer(paymentStatisticsWriter)
                .listener(new ChunkDurationTrackerListener()) // Chunk 소요 시간 측정 리스너
                .build();
    }

    /**
     * [Reader]
     * 특정 날짜의 결제 데이터를 사업자 번호 기준으로 합산하여 읽어옵니다.
     *
     */
    @Bean
    @StepScope
    public JdbcCursorItemReader<PaymentStatisticsDailySum> paymentStatisticsReader() {
        // MySQL 기준 SQL 쿼리
        String sql = String.format("""
                SELECT
                    SUM(amount) as totalAmount,
                    corp_name as corpName,
                    business_registration_number as businessRegistrationNumber,
                    payment_date_time as paymentDateTime
                FROM payment_source
                WHERE payment_date_time >= '%s 00:00:00'
                  AND payment_date_time < '%s 00:00:00'
                GROUP BY business_registration_number, corp_name
                """, properties.getPaymentDate(), properties.getPaymentDate().plusDays(1));

        return new JdbcCursorItemReaderBuilder<PaymentStatisticsDailySum>()
                .name("paymentStatisticsReader")
                .dataSource(dataSource)
                .sql(sql)
                .rowMapper(new BeanPropertyRowMapper<>(PaymentStatisticsDailySum.class))
                // MySQL에서 서버 사이드 스트리밍을 사용하기 위한 설정입니다.
                // 이 값을 Integer.MIN_VALUE로 설정하면, MySQL JDBC 드라이버는 ResultSet을 한 번에 모두 메모리에 로드하는 대신
                // 한 행씩 스트리밍 방식으로 가져옵니다. 이는 대용량 데이터 처리 시 OutOfMemoryError를 방지하는 데 필수적입니다.
                .fetchSize(Integer.MIN_VALUE)
                // 스트리밍 모드에서는 ResultSet이 TYPE_FORWARD_ONLY로 열리므로, 커서의 현재 위치를 확인하는 getRow() 메소드를 호출할 수 없습니다.
                // Spring Batch의 JdbcCursorItemReader는 기본적으로 이 검증을 수행하므로, false로 설정하여 비활성화해야 합니다.
                // 그렇지 않으면 "Operation not allowed for a result set of type ResultSet.TYPE_FORWARD_ONLY" 예외가 발생합니다.
                .verifyCursorPosition(false)
                .build();
    }

    /**
     * [Writer]
     * Processor가 전달한 PaymentDailyStatistics 엔티티를 DB에 저장합니다.
     * JpaItemWriter는 엔티티의 상태에 따라 자동으로 INSERT 또는 UPDATE를 수행합니다.
     */
    @Bean
    public ItemWriter<PaymentStatisticsDailySum> writer() {
        return chunk -> {
            @SuppressWarnings("unchecked")
            final List<PaymentStatisticsDailySum> items = (List<PaymentStatisticsDailySum>) chunk.getItems();
            paymentDailyStatisticsRecoveryService.recovery(items);
        };
    }
}