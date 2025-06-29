package com.example.springbatch5.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class PartnerCorporationService {

    // 호출 횟수를 추적하는 원자적 카운터 (스레드 안전)
    private int failureCount = 0;
    private static final String TIMEOUT_ERROR_MESSAGE = "파트너 API 서버 연결 실패: 타임아웃 발생";
    private static final int HTTP_REQUEST_DELAY_MS = 200;


    public static final Map<String, String> PARTNER_CORP = Map.ofEntries(
            Map.entry("000-01-00001", "삼성전자"),
            Map.entry("000-01-00002", "LG전자"),
            Map.entry("000-01-00003", "현대자동차"),
            Map.entry("000-01-00004", "SK텔레콤"),
            Map.entry("000-01-00005", "네이버"),
            Map.entry("000-01-00006", "카카오"),
            Map.entry("000-01-00007", "쿠팡"),
            Map.entry("000-01-00008", "배달의민족"),
            Map.entry("000-01-00009", "토스"),
            Map.entry("000-01-00010", "당근마켓"),
            Map.entry("000-01-00011", "KT"),
            Map.entry("000-01-00012", "롯데그룹"),
            Map.entry("000-01-00013", "포스코"),
            Map.entry("000-01-00014", "신한금융그룹"),
            Map.entry("000-01-00015", "KB금융그룹"),
            Map.entry("000-01-00016", "농협"),
            Map.entry("000-01-00017", "하나금융그룹"),
            Map.entry("000-01-00018", "대한항공"),
            Map.entry("000-01-00019", "아시아나항공"),
            Map.entry("000-01-00020", "CJ그룹")
    );


    /**
     * 파트너 회사명을 HTTP API 호출을 통해 가져오는 메서드 (가상)
     * 200ms 지연이 있으며, 10번 중 1번은 HTTP 통신 실패 예외가 발생함
     *
     * @param businessRegistrationNumber 파트너 사업자 번호
     * @return 파트너 회사명
     * @throws PartnerHttpException HTTP 통신 실패 시 발생하는 예외
     */
    public String getPartnerCorpName(String businessRegistrationNumber) {
//        log.info("파트너 사업자번호 {}에 대한 회사명 조회 요청", businessRegistrationNumber);

        // 호출 횟수 증가 및 주기적 실패 체크
        checkFailureByCallCount();

        final String partnerCorpName = PARTNER_CORP.getOrDefault(businessRegistrationNumber, "NONE");
        log.info("파트너 사업자번호 {}의 회사명 조회 성공: {}", businessRegistrationNumber, partnerCorpName);
        return partnerCorpName;
    }

    /**
     * 호출 횟수에 따라 예외 발생 여부를 결정하는 메서드
     * FAILURE_INTERVAL 횟수마다 한 번씩 예외를 발생시킴
     */
    private void checkFailureByCallCount() {
        // HTTP 요청 시뮬레이션 (200ms 지연)
        try {
            TimeUnit.MILLISECONDS.sleep(HTTP_REQUEST_DELAY_MS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // 랜덤하게 10% 확률로 실패하도록 변경
        if (failureCount < 2 && Math.random() < 0.1) {  // 0.0~1.0 사이의 난수 생성, 0.1 미만이면 10% 확률로 예외 발생
            failureCount++;
            log.warn("{}번째 호출에서 랜덤하게 예외 발생", failureCount);
            throw new PartnerHttpException(TIMEOUT_ERROR_MESSAGE);
        }
    }
}

