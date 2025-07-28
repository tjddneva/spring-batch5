package com.example.springbatch5.job;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * {@link LocalDate}를 기준으로 파티션을 생성하는 Partitioner 구현체입니다.
 * JobParameter로 전달받은 시작일과 종료일을 기준으로, 각 날짜에 대한 파티션을 생성합니다.
 * 생성된 각 파티션은 `paymentDate`라는 키로 해당 날짜를 StepExecutionContext에 저장합니다.
 */
@Slf4j
public class LocalDatePartitioner implements Partitioner {
    // 파티션 번호를 식별하기 위한 키
    private static final String PARTITION_KEY = "partition";
    // StepExecutionContext에 저장될 날짜 데이터의 키
    private static final String EXECUTION_CONTEXT_KEY_PAYMENT_DATE = "paymentDate";

    private final LocalDate startDate;
    private final LocalDate endDate;

    /**
     * LocalDatePartitioner 생성자
     *
     * @param startDate 파티션 생성 시작일
     * @param endDate   파티션 생성 종료일
     */
    public LocalDatePartitioner(LocalDate startDate, LocalDate endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
    }

    /**
     * gridSize만큼의 파티션을 생성합니다.
     * 시작일부터 종료일까지 하루씩 증가하며 각 날짜에 대한 ExecutionContext를 생성합니다.
     *
     * @param gridSize 파티션의 개수 (사용되지는 않음)
     * @return 각 파티션의 이름과 ExecutionContext를 담은 Map
     */
    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        Map<String, ExecutionContext> partitions = new HashMap<>();
        LocalDate currentDate = this.startDate;
        int partitionIndex = 0;

        // 시작일부터 종료일까지 반복하여 파티션 생성
        while (!currentDate.isAfter(this.endDate)) {
            ExecutionContext context = new ExecutionContext();
            // "paymentDate" 키에 현재 날짜를 ISO_LOCAL_DATE 형식의 문자열로 저장
            context.putString(EXECUTION_CONTEXT_KEY_PAYMENT_DATE, currentDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
            // "partition" + 인덱스 형태의 키로 파티션 추가
            partitions.put(PARTITION_KEY + partitionIndex, context);

            log.info("Created partition {} with paymentDate {}", PARTITION_KEY + partitionIndex, context.get(EXECUTION_CONTEXT_KEY_PAYMENT_DATE));

            // 다음 날짜로 이동
            currentDate = currentDate.plusDays(1);
            partitionIndex++;
        }

        return partitions;
    }
}
