package com.example.springbatch5.reader;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;

/**
 * HttpPageItemReader를 생성하기 위한 빌더 클래스.
 *
 * @param <T> 아이템 타입
 */
public class HttpPageItemReaderBuilder<T> {
    protected String baseUrl;
    protected RestTemplate restTemplate;
    protected int size = 10;
    protected ParameterizedTypeReference<PageResponse<T>> responseType;

    public HttpPageItemReaderBuilder<T> baseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    public HttpPageItemReaderBuilder<T> restTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        return this;
    }

    public HttpPageItemReaderBuilder<T> size(int size) {
        this.size = size;
        return this;
    }

    public HttpPageItemReaderBuilder<T> responseType(ParameterizedTypeReference<PageResponse<T>> responseType) {
        this.responseType = responseType;
        return this;
    }

    public HttpPageItemReader<T> build() {
        Assert.notNull(baseUrl, "Base URL은 필수입니다.");
        Assert.notNull(restTemplate, "RestTemplate은 필수입니다.");
        Assert.notNull(responseType, "응답 타입(responseType)은 필수입니다.");
        Assert.isTrue(size > 0, "size는 0보다 커야 합니다.");

        return new HttpPageItemReader<>(this);
    }
}
