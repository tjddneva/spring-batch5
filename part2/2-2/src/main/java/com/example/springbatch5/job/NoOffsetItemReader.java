package com.example.springbatch5.job;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.TypedQuery;
import lombok.Getter;
import lombok.Setter;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.function.Function;

/**
 * No-Offset 기법을 사용하여 페이징 처리를 하는 ItemReader 구현체.
 * 대용량 데이터 조회 시 성능 저하를 방지하기 위해 id 값을 기준으로 다음 페이지를 조회합니다.
 *
 * @param <T> 조회할 엔티티 타입
 */
public class NoOffsetItemReader<T> implements ItemStreamReader<T> {

    private final EntityManagerFactory entityManagerFactory;
    private EntityManager entityManager;
    private final String queryString;
    private final Map<String, Object> parameterValues;
    private final int chunkSize;
    private final Function<T, Long> idExtractor; // 조회된 엔티티에서 ID를 추출하는 함수
    private Long firstId; // 현재 페이지의 시작 ID
    private final Queue<T> buffer = new LinkedList<>(); // 조회된 데이터를 임시 저장하는 버퍼
    private boolean isEnd = false; // 모든 데이터를 다 읽었는지 여부
    private final Class<T> targetType; // 조회할 엔티티의 클래스 타입
    private final String name;

    /**
     * NoOffsetItemReader 생성자.
     *
     * @param entityManagerFactory JPA EntityManagerFactory
     * @param queryString          조회 JPQL 쿼리
     * @param parameterValues      쿼리 파라미터
     * @param chunkSize             페이지 사이즈
     * @param idExtractor          엔티티에서 ID를 추출하는 함수
     * @param targetType           조회할 엔티티의 클래스 타입
     */
    NoOffsetItemReader(
            EntityManagerFactory entityManagerFactory,
            String queryString,
            Map<String, Object> parameterValues,
            int chunkSize,
            Function<T, Long> idExtractor,
            Class<T> targetType,
            String name
    ) {
        this.entityManagerFactory = entityManagerFactory;
        this.queryString = queryString;
        this.parameterValues = parameterValues;
        this.chunkSize = chunkSize;
        this.idExtractor = idExtractor;
        this.targetType = targetType;
        this.name = name;
    }

    /**
     * ItemStream을 엽니다. Job 실행 전에 호출됩니다.
     * EntityManager를 생성하고, ExecutionContext에서 이전에 저장된 firstId를 복원합니다.
     *
     * @param executionContext Job 실행 컨텍스트
     * @throws ItemStreamException
     */
    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        this.entityManager = entityManagerFactory.createEntityManager();
        if (executionContext.containsKey("firstId")) {
            // Job 재시작 시, 이전에 저장된 firstId를 가져와서 중단된 지점부터 다시 시작
            this.firstId = executionContext.getLong("firstId");
        } else {
            // Job이 처음 시작될 때, 쿼리에서 가장 큰 ID 값을 가져와 firstId에 저장합니다.
            // 이 ID는 다음 페이지 조회의 시작점이 됩니다.
            TypedQuery<T> query = entityManager
                    .createQuery(queryString, this.targetType)
                    .setMaxResults(1);
            parameterValues.forEach(query::setParameter);
            List<T> results = query.getResultList();

            if (results.isEmpty()) {
                // 조회 결과가 없으면 firstId를 0으로 설정
                this.firstId = 0L;
            } else {
                // 가장 큰 ID + 1을 시작점으로 설정하여 모든 데이터를 포함하도록 함
                this.firstId = idExtractor.apply(results.get(0)) + 1;
            }
        }
    }

    /**
     * 다음 아이템을 읽습니다.
     * 버퍼가 비어있고 아직 읽을 데이터가 남아있으면 `fillBuffer()`를 호출하여 버퍼를 채웁니다.
     *
     * @return 다음 아이템 또는 null (더 이상 아이템이 없을 경우)
     */
    @Override
    public T read() {
        if (buffer.isEmpty() && !isEnd) {
            fillBuffer();
        }
        return buffer.poll();
    }

    /**
     * 데이터베이스에서 다음 페이지를 조회하여 버퍼를 채웁니다.
     * No-Offset 기법을 사용하여 `id < :firstId` 조건을 추가하여 다음 페이지를 조회합니다.
     */
    private void fillBuffer() {
        // 외부에서 받은 기본 쿼리(queryString)에 No-Offset 조건을 동적으로 추가합니다.
        // 사용자 쿼리에 WHERE 절이 있다는 전제 하에 AND로 연결합니다.
        String queryWithNoOffset = queryString.replace("WHERE", "WHERE id < :firstId AND");

        TypedQuery<T> query = entityManager
                .createQuery(queryWithNoOffset, this.targetType) // 최종적으로 조립된 쿼리를 사용합니다.
                .setMaxResults(this.chunkSize);

        // 외부에서 주입된 파라미터 설정 (e.g., paymentDate)
        parameterValues.forEach(query::setParameter);
        // 내부 상태인 firstId 파라미터 설정
        query.setParameter("firstId", this.firstId);

        List<T> results = query.getResultList();
        if (results.isEmpty()) {
            // 조회 결과가 없으면 더 이상 읽을 데이터가 없음을 표시
            this.isEnd = true;
        } else {
            // 조회된 데이터를 버퍼에 추가하고, 다음 페이지 조회를 위해 firstId를 업데이트
            buffer.addAll(results);
            this.firstId = idExtractor.apply(results.get(results.size() - 1));
        }
    }

    /**
     * ItemStream의 상태를 업데이트합니다. Step 실행 중간에 주기적으로 호출됩니다.
     * 현재 페이지의 마지막 ID를 `firstId`로 ExecutionContext에 저장하여 Job 실패 시 복구할 수 있도록 합니다.
     *
     * @param executionContext Job 실행 컨텍스트
     * @throws ItemStreamException
     */
    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        executionContext.putLong("firstId", this.firstId);
    }

    /**
     * ItemStream을 닫습니다. Job 실행 완료 또는 실패 시 호출됩니다.
     * 사용된 EntityManager를 닫습니다.
     *
     * @throws ItemStreamException
     */
    @Override
    public void close() throws ItemStreamException {
        if (entityManager != null) {
            entityManager.close();
        }
    }
}