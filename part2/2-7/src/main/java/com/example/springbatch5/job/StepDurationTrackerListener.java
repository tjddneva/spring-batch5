package com.example.springbatch5.job;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;

import java.time.LocalDateTime;

@Slf4j
public class StepDurationTrackerListener implements StepExecutionListener {

    @Override
    public void beforeStep(StepExecution stepExecution) {
        log.info(">>> Step 시작: {} (Job={}, 시작 시각: {})",
                stepExecution.getStepName(),
                stepExecution.getJobExecution().getJobInstance().getJobName(),
                stepExecution.getStartTime());
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        final LocalDateTime startTime = stepExecution.getStartTime();
        final LocalDateTime endTime = stepExecution.getEndTime();
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

        log.info(
                ">>> Step 종료: {}, 상태={}, 읽음={}건, 처리={}건, 기록={}건, 스킵={}건, 소요시간={}",
                stepExecution.getStepName(),
                stepExecution.getStatus(),
                stepExecution.getReadCount(),
                stepExecution.getProcessSkipCount() + stepExecution.getWriteCount(),
                stepExecution.getWriteCount(),
                stepExecution.getSkipCount(),
                duration
        );

        // 스킵 발생 시 커스텀 ExitStatus 설정
        if (stepExecution.getSkipCount() > 0) {
            log.warn(">>> Step 내 일부 아이템 처리 누락(스킵) 발생");
            return new ExitStatus("COMPLETED WITH SKIPS");
        }
        return stepExecution.getExitStatus();
    }
}
