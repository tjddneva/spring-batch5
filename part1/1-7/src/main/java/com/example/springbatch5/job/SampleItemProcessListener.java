package com.example.springbatch5.job;

import com.example.springbatch5.entity.Payment;
import com.example.springbatch5.entity.PaymentSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ItemProcessListener;

@Slf4j
public class SampleItemProcessListener implements ItemProcessListener<PaymentSource, Payment> {

    @Override
    public void beforeProcess(PaymentSource item) {
        log.info("sample - 3 SampleItemProcessListener beforeProcess");
    }

    @Override
    public void afterProcess(PaymentSource item, Payment result) {
        log.info("sample - 3 SampleItemProcessListener afterProcess");
    }

    @Override
    public void onProcessError(PaymentSource item, Exception e) {
        log.info("sample - 3 SampleItemProcessListener onProcessError");
    }
}
