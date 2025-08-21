package com.example.springbatch5.job;

import com.example.springbatch5.ArgumentProperties;
import com.example.springbatch5.entity.PaymentDailyStatisticsRepository;
import com.example.springbatch5.entity.PaymentSourceRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class ClearExistingDataJobListener implements JobExecutionListener {

    private final PaymentDailyStatisticsRepository paymentDailyStatisticsRepository;
    private final ArgumentProperties properties;

    @Override
    public void beforeJob(JobExecution jobExecution) {
        if (properties.getClearExistingData()){
            log.info("paymentDate='{}'에 해당하는 기존 결제 통계 데이터 삭제를 시작합니다.", properties.getPaymentDate());
            Long deletedCount = paymentDailyStatisticsRepository.deleteByPaymentDate(properties.getPaymentDate());
            log.info("기존 결제 통계 데이터 {}건 삭제를 완료했습니다.", deletedCount);
        }
    }

    @Override
    public void afterJob(JobExecution jobExecution) {}
}
