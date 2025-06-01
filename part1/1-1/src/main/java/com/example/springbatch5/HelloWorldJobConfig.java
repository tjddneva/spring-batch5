package com.example.springbatch5;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;

@Configuration
public class HelloWorldJobConfig {

    @Bean
    public Job helloWorldJob(
            JobRepository jobRepository,
            Step helloWorldStep
    ) {
        return new JobBuilder("helloWorldJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(helloWorldStep)
                .build();
    }

    @Bean
    @JobScope
    public Step helloWorldStep(
            JobRepository jobRepository,
            Tasklet helloWorlTasklet,
            PlatformTransactionManager transactionManager
    ) {
        return new StepBuilder("helloWorldStep", jobRepository)
                .tasklet(helloWorlTasklet, transactionManager)
                .build();
    }


    @StepScope
    @Bean
    public Tasklet helloWorlTasklet() {
        return (contribution, chunkContext) -> {
            var items = getItems();
            for (String item : items) {
                System.out.println("Hello, World! + " + item);
            }
            return RepeatStatus.FINISHED;
        };
    }

    private static List<String> getItems() {
        return List.of("항목1", "항목2", "항목3", "항목4", "항목5", "항목6", "항목7", "항목8", "항목9", "항목10");
    }
}