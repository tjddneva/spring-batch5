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
    protected boolean ignoreErrors = false; // 기본값은 false

    public HttpPageItemReaderBuilder<T> baseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    public HttpPageItemReaderBuilder<T> restTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        return this;
    }

    public HttpPageItemReaderBuilder<T> size(int size) { // size -> pageSize로 변경
        this.size = size;
        return this;
    }

    public HttpPageItemReaderBuilder<T> responseType(ParameterizedTypeReference<PageResponse<T>> responseType) {
        this.responseType = responseType;
        return this;
    }

    /**
     * API 호출 중 오류 발생 시 예외를 무시할지 여부를 설정합니다.
     * @param ignoreErrors true로 설정하면 오류 발생 시 해당 페이지만 건너뛰고 배치를 계속 진행합니다.
     *                     false(기본값)로 설정하면 예외를 던져 배치를 실패시킵니다.
     * @return 빌더 자신
     */
    public HttpPageItemReaderBuilder<T> ignoreErrors(boolean ignoreErrors) {
        this.ignoreErrors = ignoreErrors;
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
