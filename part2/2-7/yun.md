# Spring Batch 파티셔닝(Partitioning)을 이용한 병렬 처리 상세 설명

## 1. 파티셔닝 (Partitioning) 이란?

Spring Batch의 파티셔닝은 단일 Step을 여러 개의 작은 단위(파티션)로 나누어 동시에 병렬로 실행하는 고급 기능입니다. 이를 통해 대용량 데이터 처리 시간을 획기적으로 단축하고 시스템 리소스를 효율적으로 활용할 수 있습니다.

**주요 사용 사례:**
- **기간별 데이터 처리:** 특정 기간(예: 일별, 월별)의 데이터를 각각의 파티션에서 병렬로 처리합니다.
- **데이터 속성별 분할:** 데이터의 특정 속성(예: 사용자 등급, 지역 코드)을 기준으로 파티션을 나누어 처리합니다.
- **대용량 파일 처리:** 거대한 파일을 여러 조각으로 나누어 각 파티션에서 병렬로 읽고 처리합니다.

---

## 2. 핵심 아키텍처: Manager와 Worker

파티셔닝은 크게 '매니저(Manager)'와 '워커(Worker)' 두 가지 역할로 구성된 스텝으로 구현됩니다.

![Partitioning Architecture](https://docs.spring.io/spring-batch/docs/current/reference/html/images/partitioning.png)
*(Image Source: Spring Batch Official Documentation)*

### 가. 매니저 스텝 (Manager Step)
- **역할:** 전체 작업을 조율하고 워커 스텝을 관리합니다.
- **주요 책임:**
    1.  **파티션 생성 (`Partitioner`):** `Partitioner`를 사용하여 전체 데이터를 어떻게 나눌지 결정하고, 각 파티션(워커 스텝)에 필요한 데이터(파라미터)를 `ExecutionContext`에 담아 생성합니다.
    2.  **워커 스텝 실행:** 생성된 파티션의 수만큼 워커 스텝을 실행합니다.
    3.  **병렬 처리:** `TaskExecutor`를 통해 워커 스텝들을 병렬로 실행시킵니다.

### 나. 워커 스텝 (Worker Step)
- **역할:** 매니저 스텝으로부터 할당받은 파티션의 실제 데이터 처리를 담당합니다.
- **주요 책임:**
    1.  **데이터 수신:** 매니저가 `ExecutionContext`에 담아 전달한 파라미터를 `@StepScope`를 통해 주입받습니다.
    2.  **독립적 실행:** 각 워커 스텝은 독립적인 `Reader`, `Processor`, `Writer`를 가지고 자신에게 할당된 데이터 범위만 처리합니다.
    3.  **트랜잭션 관리:** 각 워커 스텝은 자신만의 트랜잭션 내에서 동작합니다.

---

## 3. 코드 상세 분석

이번 예제에서는 `start_date`와 `end_date`를 Job Parameter로 받아, 그 기간에 포함된 각 날짜를 하나의 파티션으로 만들어 병렬 처리하는 시나리오를 구현했습니다.

### 가. `LocalDatePartitioner.java` - 파티션 생성기

`Partitioner` 인터페이스의 구현체로, 매니저 스텝의 요청에 따라 파티션을 생성하는 역할을 합니다.

```java
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

    public LocalDatePartitioner(LocalDate startDate, LocalDate endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
    }

    /**
     * gridSize만큼의 파티션을 생성합니다.
     * 시작일부터 종료일까지 하루씩 증가하며 각 날짜에 대한 ExecutionContext를 생성합니다.
     */
    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        Map<String, ExecutionContext> partitions = new HashMap<>();
        LocalDate currentDate = startDate;
        int partitionIndex = 0;

        // 시작일부터 종료일까지 반복하여 파티션 생성
        while (!currentDate.isAfter(endDate)) {
            ExecutionContext context = new ExecutionContext();
            // "paymentDate" 키에 현재 날짜를 ISO_LOCAL_DATE 형식의 문자열로 저장
            // 이 context가 각 Worker Step의 StepExecutionContext가 됩니다.
            context.putString(EXECUTION_CONTEXT_KEY_PAYMENT_DATE, currentDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
            
            // "partition0", "partition1", ... 형태의 키로 파티션 추가
            partitions.put(PARTITION_KEY + partitionIndex, context);

            log.info("Created partition {} with paymentDate {}", PARTITION_KEY + partitionIndex, context.get(EXECUTION_CONTEXT_KEY_PAYMENT_DATE));

            currentDate = currentDate.plusDays(1);
            partitionIndex++;
        }

        return partitions;
    }
}
```

**핵심 로직:**
1.  `partition()` 메소드는 매니저 스텝에 의해 호출됩니다.
2.  `startDate`부터 `endDate`까지 하루씩 순회하면서 `ExecutionContext`를 생성합니다.
3.  생성된 `ExecutionContext`에는 `paymentDate`라는 키로 해당 날짜의 정보가 문자열 형태로 저장됩니다.
4.  이 `ExecutionContext` 맵(`partitions`)이 매니저 스텝으로 반환되면, 매니저는 맵의 각 엔트리(`"partition0"`, `"partition1"`, ...)를 하나의 워커 스텝으로 만들어 실행합니다.

### 나. `PartitionJobConfig.java` - Job 및 Step 설정

파티셔닝 Job의 전체적인 흐름과 매니저/워커 스텝을 설정하는 Configuration 클래스입니다.

```java
@Slf4j
@Configuration
@AllArgsConstructor
public class PartitionJobConfig {
    // ... (생성자 주입)

    // 1. Job 정의
    @Bean
    public Job partitionerJob(Step managerStep) {
        return new JobBuilder("partitionerJob", jobRepository)
                .start(managerStep)
                .build();
    }

    // 2. 매니저 스텝(Manager Step) 정의
    @Bean
    public Step managerStep(Step workerStep) {
        return new StepBuilder("managerStep", jobRepository)
                // "workerStep"이라는 이름으로 파티셔너를 설정
                .partitioner("workerStep", partitioner(null, null))
                // 각 파티션에서 실행될 스텝(워커 스텝)을 지정
                .step(workerStep)
                // 동시에 실행할 파티션(스레드)의 수를 설정
                .gridSize(5)
                // 파티션을 병렬로 처리하기 위한 TaskExecutor 설정
                .taskExecutor(taskExecutor())
                .build();
    }

    // 3. Partitioner Bean 생성
    @Bean
    @StepScope // JobParameter를 받기 위해 @StepScope 사용
    public LocalDatePartitioner partitioner(
            @Value("#{jobParameters['start_date']}") String startDateStr,
            @Value("#{jobParameters['end_date']}") String endDateStr
    ) {
        LocalDate startDate = LocalDate.parse(startDateStr, DateTimeFormatter.ISO_LOCAL_DATE);
        LocalDate endDate = LocalDate.parse(endDateStr, DateTimeFormatter.ISO_LOCAL_DATE);
        return new LocalDatePartitioner(startDate, endDate);
    }

    // 4. TaskExecutor (스레드 풀) 설정
    @Bean
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(10);
        taskExecutor.setMaxPoolSize(20);
        taskExecutor.setThreadNamePrefix("partition-thread-");
        taskExecutor.initialize();
        return taskExecutor;
    }

    // 5. 워커 스텝(Worker Step) 정의
    @Bean
    public Step workerStep(JpaCursorItemReader<PaymentSource> cursorItemReader) {
        return new StepBuilder("workerStep", jobRepository)
                .<PaymentSource, Payment>chunk(chunkSize, transactionManager)
                .reader(cursorItemReader)
                .processor(paymentReportProcessor())
                .writer(paymentReportWriter())
                .build();
    }

    // 6. 워커 스텝의 Reader 정의
    @Bean
    @StepScope // StepExecutionContext의 데이터를 받기 위해 @StepScope 사용
    public JpaCursorItemReader<PaymentSource> cursorItemReader(
            @Value("#{stepExecutionContext['paymentDate']}") String paymentDateStr
    ) {
        LocalDate paymentDate = LocalDate.parse(paymentDateStr, DateTimeFormatter.ISO_LOCAL_DATE);
        return new JpaCursorItemReaderBuilder<PaymentSource>()
                .name("cursorItemReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT ps FROM PaymentSource ps WHERE ps.paymentDate = :paymentDate")
                .parameterValues(Collections.singletonMap("paymentDate", paymentDate))
                .build();
    }
    
    // ... (Processor, Writer 생략)
}
```

**설정 흐름:**
1.  **`partitionerJob`**이 실행되면 **`managerStep`**이 시작됩니다.
2.  **`managerStep`**은 `partitioner()` 메소드를 호출하여 파티션을 생성합니다.
    -   이때 `partitioner()`는 `@StepScope`로 선언되어 Job 실행 시점에 전달된 `jobParameters` (`start_date`, `end_date`)를 주입받을 수 있습니다.
3.  `partitioner()`가 생성한 `Map<String, ExecutionContext>`를 기반으로, `managerStep`은 여러 개의 **`workerStep`** 인스턴스를 생성합니다.
4.  `gridSize(5)` 설정에 따라, **`taskExecutor`**가 제공하는 스레드 풀에서 최대 5개의 `workerStep`이 동시에 실행됩니다.
5.  각 **`workerStep`**은 자신만의 `cursorItemReader`를 가집니다.
6.  `cursorItemReader`는 `@StepScope`로 선언되어, `managerStep`이 `Partitioner`를 통해 전달한 `stepExecutionContext`의 `paymentDate` 값을 주입받습니다.
    -   **`@Value("#{stepExecutionContext['paymentDate']}")`**: 이 SpEL(Spring Expression Language)은 각 워커 스텝의 `StepExecutionContext`에서 `paymentDate` 키의 값을 찾아 주입해달라는 의미입니다.
7.  각 `cursorItemReader`는 주입받은 `paymentDate`를 사용하여 DB에서 해당 날짜의 데이터만 읽어오고, 이후 `Processor`와 `Writer`를 통해 처리합니다.

---

## 4. 실행 방법 및 흐름 요약

### 가. Job 실행 명령어 (예시)

이 Job을 실행하려면 반드시 `start_date`와 `end_date`를 Job Parameter로 전달해야 합니다.

```shell
java -jar build/libs/your-project.jar --job.name=partitionerJob start_date=2024-01-01 end_date=2024-01-10
```

### 나. 전체 실행 흐름

1.  `partitionerJob` 실행. `start_date=2024-01-01`, `end_date=2024-01-10` 파라미터 전달.
2.  `managerStep` 시작.
3.  `managerStep`이 `@StepScope`인 `partitioner` Bean을 생성하며 Job Parameter를 전달.
4.  `LocalDatePartitioner`는 2024-01-01부터 2024-01-10까지 총 10개의 파티션을 생성.
    -   `partition0`: `ExecutionContext("paymentDate"="2024-01-01")`
    -   `partition1`: `ExecutionContext("paymentDate"="2024-01-02")`
    -   ...
    -   `partition9`: `ExecutionContext("paymentDate"="2024-01-10")`
5.  `managerStep`은 10개의 `workerStep`을 실행할 준비를 하고, `gridSize`가 5이므로 스레드 풀에서 5개의 스레드를 할당받아 `workerStep` 5개를 먼저 실행.
6.  **(동시 실행)**
    -   **Thread 1 (`workerStep` for `partition0`):** `cursorItemReader`는 `paymentDate`로 "2024-01-01"을 주입받아 해당 날짜의 데이터를 처리.
    -   **Thread 2 (`workerStep` for `partition1`):** `cursorItemReader`는 `paymentDate`로 "2024-01-02"을 주입받아 해당 날짜의 데이터를 처리.
    -   **Thread 3 (`workerStep` for `partition2`):** `cursorItemReader`는 `paymentDate`로 "2024-01-03"을 주입받아 해당 날짜의 데이터를 처리.
    -   **Thread 4 (`workerStep` for `partition3`):** `cursorItemReader`는 `paymentDate`로 "2024-01-04"을 주입받아 해당 날짜의 데이터를 처리.
    -   **Thread 5 (`workerStep` for `partition4`):** `cursorItemReader`는 `paymentDate`로 "2024-01-05"을 주입받아 해당 날짜의 데이터를 처리.
7.  실행이 끝난 스레드가 생기면, 대기 중이던 다음 `workerStep`(예: `partition5`)이 해당 스레드를 할당받아 실행.
8.  모든 `workerStep`이 성공적으로 완료되면 `managerStep`도 종료되고, 최종적으로 `partitionerJob`이 성공적으로 완료됩니다.
