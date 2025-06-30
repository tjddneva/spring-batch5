package com.example.springbatch5.job;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;

import java.time.LocalDateTime;

@Slf4j
public class JobDurationTrackerListener implements JobExecutionListener {

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info(">>> Job 시작: {} (시작 시각: {})",
                jobExecution.getJobInstance().getJobName(),
                jobExecution.getStartTime());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        final LocalDateTime startTime = jobExecution.getStartTime();
        final LocalDateTime endTime = jobExecution.getEndTime();
        long durationMillis = java.time.Duration.between(startTime, endTime).toMillis();
        long hours = durationMillis / (1000 * 60 * 60);
        long minutes = (durationMillis % (1000 * 60 * 60)) / (1000 * 60);
        long seconds = (durationMillis % (1000 * 60)) / 1000;

        String duration;
        if (hours > 0) {
            duration = String.format("%d시간 %d분", hours, minutes);
        } else if (minutes > 0) {
            duration = String.format("%d분", minutes);
        } else {
            duration = String.format("%d초", seconds);
        }

        log.info(">>> Job 종료: 상태={}, 총 소요시간={}, 종료 시각={}", jobExecution.getStatus(), duration, jobExecution.getEndTime());
        if (jobExecution.getStatus().isUnsuccessful()) {
            log.error(">>> Job 실패 원인: {}", jobExecution.getAllFailureExceptions());
        }
    }
}

