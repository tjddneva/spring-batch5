package com.example.springbatch5.job;

import com.example.springbatch5.entity.Coupon;
import com.example.springbatch5.reader.HttpPageItemReaderBuilder;
import com.example.springbatch5.reader.MemberResponse;
import jakarta.persistence.EntityManagerFactory;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.client.RestTemplate;
import java.math.BigDecimal;
import java.time.LocalDate;

@Slf4j
@Configuration
@AllArgsConstructor
public class CouponJobConfig {

    private final EntityManagerFactory entityManagerFactory;
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    private final int chunkSize = 10;

    @Bean
    public Job couponJob(
            Step couponStep
    ) {
        return new JobBuilder("couponJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(couponStep)
                .build();
    }

    @Bean
    public Step couponStep(
            ItemReader<MemberResponse> couponReader
    ) {
        return new StepBuilder("couponStep", jobRepository)
                .<MemberResponse, Coupon>chunk(chunkSize, transactionManager)
                // Step 소요 시간 측정
                .listener(new StepDurationTrackerListener())
                .reader(couponReader)
                .processor(couponProcessor())
                .writer(couponWriter())
                // Chunk 소요 시간 측정
                .listener(new ChunkDurationTrackerListener())
                .build();
    }

    /**
     * RestTemplate을 Bean으로 등록하여 다른 곳에서도 재사용할 수 있도록 합니다.
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public ItemReader<MemberResponse> couponReader(RestTemplate restTemplate) {
        return new HttpPageItemReaderBuilder<MemberResponse>()
                .baseUrl("http://localhost:8080/api/v1/members") // 요청할 API의 기본 URL
                .size(chunkSize)
                .restTemplate(restTemplate)
                .responseType(new ParameterizedTypeReference<>() {})
                .ignoreErrors(true) // 오류 발생시 무시
                .build();
    }

    private ItemProcessor<MemberResponse, Coupon> couponProcessor() {
        return member -> new Coupon(
                "회원가입 쿠폰",
                BigDecimal.valueOf(1000),
                LocalDate.now().plusDays(30),
                false,
                member.getId()
        );
    }

    @Bean
    public JpaItemWriter<Coupon> couponWriter() {
        return new JpaItemWriterBuilder<Coupon>()
                .entityManagerFactory(entityManagerFactory)
                .build();
    }
}
