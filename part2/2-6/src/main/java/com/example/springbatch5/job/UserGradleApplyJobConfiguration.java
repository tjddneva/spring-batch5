package com.example.springbatch5.job;

import com.example.springbatch5.entity.User;
import com.example.springbatch5.entity.UserRepository;
import com.example.springbatch5.service.OrderClient;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.schedulers.Schedulers;
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
import org.springframework.batch.item.database.JpaCursorItemReader;
import org.springframework.batch.item.database.builder.JpaCursorItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.stream.Collectors;

/**
 * MembershipGradeUpdateJob
 */
@Slf4j
@Configuration
@AllArgsConstructor
public class UserGradleApplyJobConfiguration {

    private final EntityManagerFactory entityManagerFactory;
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final OrderClient orderClient;
    private final UserRepository userRepository;
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
            ItemWriter<User> writer
    ) {
        return new StepBuilder("userGradleApplyStep", jobRepository)
                .<User, User>chunk(chunkSize, transactionManager)
                // Step 소요 시간 측정
                .listener(new StepDurationTrackerListener())
                .reader(cursorItemReader)
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
    public ItemWriter<User> writer() {
        return users -> {
            var appliedGradeUsers = Flowable.fromIterable(users.getItems())
                    .parallel()
                    .runOn(Schedulers.io())
                    .map(user -> {
                        final var grade = orderClient.getGrade(user.getId());
                        user.setGrade(grade);
                        return user;
                    })
                    .sequential()
                    .toList()
                    .blockingGet();

            appliedGradeUsers.stream()
                    .collect(Collectors.groupingBy(User::getGrade))
                    .forEach((grade, targetUsers) -> {
                                final var userIds = targetUsers.stream().map(User::getId).toList();
                                userRepository.updateGrade(grade, userIds);
                            }
                    );
        };
    }
}
