package com.example.springbatch5.job;

import com.example.springbatch5.ArgumentProperties;
import com.example.springbatch5.entity.PaymentSourceRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Set;

@Service
@AllArgsConstructor
@Slf4j
public class PrepareTargetDatesJobListener implements JobExecutionListener {

    private final PaymentSourceRepository paymentSourceRepository;
    private final ArgumentProperties properties;

    @Override
    public void beforeJob(JobExecution jobExecution) {

        final Set<LocalDate> paymentDateTargets = paymentSourceRepository.findPaymentDatesByTodayUpdates();
        log.info("재처리 대상 paymentDateTargets={}", paymentDateTargets);
        properties.setTargetPaymentDates(paymentDateTargets);

    }

    @Override
    public void afterJob(JobExecution jobExecution) {}
}
