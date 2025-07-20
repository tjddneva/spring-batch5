package com.example.springbatch5.job;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class NoOffsetItemReaderBuilder<T> {

    private EntityManagerFactory entityManagerFactory;
    private int pageSize = 100; // 기본값
    private String queryString;
    private Map<String, Object> parameterValues = new HashMap<>();
    private Function<T, Long> idExtractor;
    private String name;
    private Class<T> targetType;

    public NoOffsetItemReaderBuilder<T> name(String name) {
        this.name = name;
        return this;
    }

    public NoOffsetItemReaderBuilder<T> entityManagerFactory(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
        return this;
    }

    public NoOffsetItemReaderBuilder<T> pageSize(int pageSize) {
        this.pageSize = pageSize;
        return this;
    }

    public NoOffsetItemReaderBuilder<T> queryString(String queryString) {
        this.queryString = queryString;
        return this;
    }

    public NoOffsetItemReaderBuilder<T> parameterValues(Map<String, Object> parameterValues) {
        this.parameterValues = parameterValues;
        return this;
    }

    public NoOffsetItemReaderBuilder<T> idExtractor(Function<T, Long> idExtractor) {
        this.idExtractor = idExtractor;
        return this;
    }

    public NoOffsetItemReaderBuilder<T> targetType(Class<T> targetType) {
        this.targetType = targetType;
        return this;
    }

    // 최종적으로 설정된 값으로 NoOffsetItemReader 인스턴스를 생성하여 반환
    public NoOffsetItemReader<T> build() {
        // 필수 값 검증
        Assert.state(entityManagerFactory != null, "EntityManagerFactory is required.");
        Assert.state(queryString != null, "Query string is required.");
        Assert.state(idExtractor != null, "ID extractor function is required.");
        Assert.state(targetType != null, "Target type is required.");

        NoOffsetItemReader<T> reader = new NoOffsetItemReader<>(
                this.entityManagerFactory,
                this.queryString,
                this.parameterValues,
                this.pageSize,
                this.idExtractor,
                this.targetType // 생성자에 targetType 전달
        );
        reader.setName(this.name); // 배치 메타데이터 관리를 위한 이름 설정
        return reader;
    }
}