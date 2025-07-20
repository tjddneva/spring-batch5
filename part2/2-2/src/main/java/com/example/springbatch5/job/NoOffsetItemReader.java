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

public class NoOffsetItemReader<T> implements ItemStreamReader<T> {

    private final EntityManagerFactory entityManagerFactory;
    private EntityManager entityManager;
    private final String queryString;
    private final Map<String, Object> parameterValues;
    private final int pageSize;
    private final Function<T, Long> idExtractor;
    private Long lastId = 0L;
    private final Queue<T> buffer = new LinkedList<>();
    private boolean isEnd = false;
    private final Class<T> targetType; // 1. 클래스 타입을 저장할 필드
    @Getter
    @Setter
    private String name;

    // 2. 생성자에서 Class<T> targetType을 파라미터로 받음
    NoOffsetItemReader(
            EntityManagerFactory entityManagerFactory,
            String queryString,
            Map<String, Object> parameterValues,
            int pageSize,
            Function<T, Long> idExtractor,
            Class<T> targetType
    ) {
        this.entityManagerFactory = entityManagerFactory;
        this.queryString = queryString;
        this.parameterValues = parameterValues;
        this.pageSize = pageSize;
        this.idExtractor = idExtractor;
        this.targetType = targetType; // 3. 전달받은 타입 정보를 필드에 저장
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        this.entityManager = entityManagerFactory.createEntityManager();
        if (executionContext.containsKey("lastId")) {
            this.lastId = executionContext.getLong("lastId");
        } else {
            TypedQuery<T> query = entityManager
                    .createQuery(queryString, this.targetType)
                    .setMaxResults(1);
            parameterValues.forEach(query::setParameter);
            query.setParameter("lastId", 0L);

            List<T> resultList = query.getResultList();

            if (resultList.isEmpty()) {
                this.lastId = 0L;
            } else {
                this.lastId = idExtractor.apply(resultList.get(0)) - 1;
            }
        }
    }

    @Override
    public T read() {
        if (buffer.isEmpty() && !isEnd) {
            fillBuffer();
        }
        return buffer.poll();
    }

    private void fillBuffer() {
        // 1. 외부에서 받은 기본 쿼리(queryString)에 No-Offset 조건을 동적으로 추가합니다.
        //    사용자 쿼리에 WHERE 절이 있다는 전제 하에 AND로 연결합니다.

        TypedQuery<T> query = entityManager
                .createQuery(queryString, this.targetType) // 2. 최종적으로 조립된 쿼리를 사용합니다.
                .setMaxResults(this.pageSize);

        // 외부에서 주입된 파라미터 설정 (e.g., paymentDate)
        parameterValues.forEach(query::setParameter);
        // 내부 상태인 lastId 파라미터 설정
        query.setParameter("lastId", this.lastId);

        List<T> resultList = query.getResultList();

        if (resultList.isEmpty()) {
            this.isEnd = true;
        } else {
            buffer.addAll(resultList);
            this.lastId = idExtractor.apply(resultList.get(resultList.size() - 1));
        }
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        executionContext.putLong("lastId", this.lastId);
    }

    @Override
    public void close() throws ItemStreamException {
        if (entityManager != null) {
            entityManager.close();
        }
    }
}