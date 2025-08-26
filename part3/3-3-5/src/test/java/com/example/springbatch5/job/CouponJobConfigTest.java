package com.example.springbatch5.job;

import com.example.springbatch5.SpringBatchTestSupport;
import com.example.springbatch5.entity.Coupon;
import com.example.springbatch5.entity.QCoupon;
import com.example.springbatch5.reader.MemberResponse;
import com.example.springbatch5.reader.PageResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.batch.core.Job;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;


class CouponJobConfigTest extends SpringBatchTestSupport {
    @Autowired
    private Job couponJob;
    @Autowired
    private RestTemplate mockRestTemplate;
    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    public void resetMock() {
        Mockito.reset(mockRestTemplate);
    }

    @Test
    void couponJobTest() throws Exception {
        // given
        // Mock for page 0
        final String pageResponse0 = readFile("/json/page-response-page-0.json");
        final PageResponse<MemberResponse> responsePage0 = objectMapper.readValue(pageResponse0, new TypeReference<>() {});
        final UriComponentsBuilder queryParam0 = UriComponentsBuilder.fromUriString("http://localhost:8080/api/v1/members")
                .queryParam("page", 0)
                .queryParam("size", 10);

        given(
                mockRestTemplate.exchange(
                        queryParam0.toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<PageResponse<MemberResponse>>() {
                        }
                )
        ).willReturn(ResponseEntity.ok(responsePage0));

        // Mock for page 1
        final String page1Json = readFile("/json/page-response-page-1.json");
        final PageResponse<MemberResponse> responsePage1 = objectMapper.readValue(page1Json, new TypeReference<>() {});
        final UriComponentsBuilder queryParam1 = UriComponentsBuilder.fromUriString("http://localhost:8080/api/v1/members")
                .queryParam("page", 1)
                .queryParam("size", 10);

        given(
                mockRestTemplate.exchange(
                        queryParam1.toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<PageResponse<MemberResponse>>() {
                        }
                )
        ).willReturn(ResponseEntity.ok(responsePage1));

        // when
        launchJob(couponJob);

        // then
        thenBatchCompleted();
        final List<Coupon> coupons = query.selectFrom(QCoupon.coupon).fetch();

        // page-0.json(10개) + page-1.json(5개) = 15개
        then(coupons).hasSize(15);
    }
}