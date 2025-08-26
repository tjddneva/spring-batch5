package com.example.springbatch5;

import com.querydsl.core.types.EntityPath;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.batch.core.*;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestConstructor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBatchTest
@SpringBootTest(properties = {"spring.batch.job.enabled=false"})
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
public abstract class SpringBatchTestSupport {
    @Autowired
    protected JobLauncherTestUtils jobLauncherTestUtils;
    @Autowired
    protected EntityManagerFactory entityManagerFactory;
    @Autowired
    protected Job paymentStatisticsJob;
    @Autowired
    protected ArgumentProperties properties;

    protected JobExecution jobExecution;
    protected EntityManager entityManager;
    protected JPAQueryFactory query;

    @BeforeAll
    void setUp() {
        this.entityManager = entityManagerFactory.createEntityManager();
        this.query = new JPAQueryFactory(this.entityManager);
    }

    protected void launchJob(Job job, JobParameters jobParameters) throws Exception {
        this.jobLauncherTestUtils.setJob(job);
        this.jobExecution = this.jobLauncherTestUtils.launchJob(jobParameters);
    }

    protected void launchJob(Job job) throws Exception {
        launchJob(job, this.jobLauncherTestUtils.getUniqueJobParameters());
    }

    protected void launchStep(String stepName, JobParameters jobParameters, ExecutionContext executionContext) {
        this.jobExecution = this.jobLauncherTestUtils.launchStep(stepName, jobParameters, executionContext);
    }

    protected void launchStep(String stepName, JobParameters jobParameters) throws Exception {
        launchStep(stepName, jobParameters, null);
    }

    protected void launchStep(String stepName) throws Exception {
        launchStep(stepName, this.jobLauncherTestUtils.getUniqueJobParameters(), null);
    }

    protected void thenBatchCompleted() {
        assertThat(jobExecution).isNotNull();
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }

    protected void thenBatchStatus(BatchStatus batchStatus) {
        assertThat(jobExecution).isNotNull();
        assertThat(jobExecution.getStatus()).isEqualTo(batchStatus);
    }

    protected <T> T save(T entity) {
        entityManager.getTransaction().begin();
        entityManager.persist(entity);
        entityManager.getTransaction().commit();
        entityManager.clear();
        return entity;
    }

    protected <T> List<T> saveAll(List<T> entities) {
        entityManager.getTransaction().begin();
        for (T entity : entities) {
            entityManager.persist(entity);
        }
        entityManager.getTransaction().commit();
        entityManager.clear();
        return entities;
    }

    protected <T> void deleteAll(EntityPath<T> path) {
        entityManager.getTransaction().begin();
        query.delete(path).execute();
        entityManager.getTransaction().commit();
    }
}
