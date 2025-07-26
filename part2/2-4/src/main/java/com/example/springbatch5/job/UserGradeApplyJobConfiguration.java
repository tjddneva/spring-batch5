package com.example.springbatch5.job;

import com.example.springbatch5.entity.Grade;
import com.example.springbatch5.entity.User;
import com.example.springbatch5.service.OrderClient;
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
import org.springframework.batch.item.database.JpaCursorItemReader;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.builder.JpaCursorItemReaderBuilder;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * MembershipGradeUpdateJob
 */
@Slf4j
@Configuration
@AllArgsConstructor
public class UserGradeApplyJobConfiguration {

    private final EntityManagerFactory entityManagerFactory;
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final OrderClient orderClient;
    private final int chunkSize = 1_000;

    @Bean
    public Job userGradleApplyJob(
            Step userGradleApplyStep
    ) {
        return new JobBuilder("userGradleApplyJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(userGradleApplyStep)
                .build();
    }

    @Bean
    public Step userGradleApplyStep(
            JpaCursorItemReader<User> cursorItemReader,
            ItemProcessor<User, User> processor,
            JpaItemWriter<User> writer
    ) {
        return new StepBuilder("userGradleApplyStep", jobRepository)
                .<User, User>chunk(chunkSize, transactionManager)
                // Step 소요 시간 측정
                .listener(new StepDurationTrackerListener())
                .reader(cursorItemReader)
                .processor(processor)
                .writer(writer)
                .listener(new ChunkDurationTrackerListener())
                .build();
    }

    @Bean
    @StepScope
    public JpaCursorItemReader<User> cursorItemReader() {
        return new JpaCursorItemReaderBuilder<User>()
                .name("cursorItemReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT u FROM User u WHERE u.grade = 'INIT'")
                .build();
    }

    @Bean
    public ItemProcessor<User, User> processor() {
        return user -> {
            // user 등급 조회 API 150ms 으로 고정
            // order api에서 계산된 유저의 등급정보를 유저 객체에 반영
            final Grade grade = orderClient.getGrade(user.getId());
            user.setGrade(grade);
            return user;
        };
    }

    @Bean
    public JpaItemWriter<User> writer() {
        // JpaItemWriter 으로 update 반영
        return new JpaItemWriterBuilder<User>()
                .entityManagerFactory(entityManagerFactory)
                .build();
    }
}