package com.example.springbatch5.reader;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

/**
 * HTTP를 통해 외부 API로부터 페이징 처리된 데이터를 읽어오는 Spring Batch ItemReader 입니다.
 *
 * 이 클래스는 {@link AbstractItemCountingItemStreamItemReader}를 상속받아,
 * Spring Batch가 재시작(restart) 시 상태를 올바르게 관리할 수 있도록 아이템 개수를 자동으로 카운팅합니다.
 * RestTemplate을 사용하여 지정된 URL로 GET 요청을 보내고, 페이징된 응답을 처리합니다.
 *
 * @param <T> API 응답의 content 필드에 포함된 개별 아이템의 DTO(Data Transfer Object) 타입입니다.
 */

@Slf4j
public class HttpPageItemReader<T> extends AbstractItemCountingItemStreamItemReader<T> {


    // 요청을 보낼 기본 URL
    private final String baseUrl;

    // HTTP 통신을 위해 사용하는 Spring의 동기 클라이언트
    private final RestTemplate restTemplate;

    // 한 번의 API 호출로 가져올 데이터의 개수 (페이지 크기)
    private final int size;

    /**
     * RestTemplate이 제네릭 타입을 포함한 응답을 올바르게 역직렬화(deserialize)하도록 돕는 객체입니다.
     *
     * ### 왜 ParameterizedTypeReference가 필요한가? (중요)
     * Java의 제네릭은 컴파일 시점에만 타입 검사를 하고, 런타임에는 타입 정보가 지워지는 '타입 소거(Type Erasure)' 특징이 있습니다.
     * 만약 `restTemplate.exchange(url, ..., PageResponse.class)` 와 같이 호출하면,
     * RestTemplate은 `PageResponse`라는 클래스 정보만 알 수 있고, 제네릭 `<T>`가 어떤 타입인지 알 수 없습니다.
     * 결과적으로, JSON 응답의 `content` 필드에 있는 객체들을 `T` 타입(예: MemberResponse)으로 변환하지 못하고,
     * 기본 자료구조인 `LinkedHashMap`으로 변환하게 됩니다. 이로 인해 이후 처리 과정에서 `ClassCastException`이 발생합니다.
     *
     * `new ParameterizedTypeReference<PageResponse<T>>() {}` 와 같이 익명 클래스를 생성하면,
     * 제네릭 타입 정보(`PageResponse<T>`)가 클래스 메타데이터에 저장되어 런타임에도 유지됩니다.
     * RestTemplate은 이 정보를 리플렉션(reflection)을 통해 읽어와 `content` 필드를 정확한 `T` 타입의 리스트로 변환할 수 있습니다.
     */
    private final ParameterizedTypeReference<PageResponse<T>> responseType;

    // API 호출 중 오류가 발생했을 때 해당 오류를 무시하고 계속 진행할지 여부를 결정하는 플래그
    // - true: 오류가 발생해도 Step을 실패시키지 않고, null을 반환하여 해당 페이지만 건너뜁니다.
    // - false: 오류가 발생하면 예외를 던져 Step을 즉시 실패시킵니다. (기본값)
    private final boolean ignoreErrors;

    // 현재 페이지에서 가져온 아이템들을 임시로 저장하는 리스트 (버퍼 역할)
    private List<T> items;

    // 요청할 페이지 번호 (0부터 시작)
    private int page = 0;

    // `items` 리스트에서 현재 읽어야 할 아이템의 인덱스
    private int currentItemIndex = 0;

    // API 응답에서 마지막 페이지인지 여부를 저장하는 플래그
    private boolean lastPage = false;

    /**
     * 빌더 패턴을 통해 HttpPageItemReader의 인스턴스를 생성합니다.
     * 필수 파라미터들이 빌더를 통해 안전하게 주입됩니다.
     *
     * @param builder 필요한 모든 설정값이 담긴 HttpPageItemReaderBuilder 객체
     */
    HttpPageItemReader(HttpPageItemReaderBuilder<T> builder) {
        this.baseUrl = builder.baseUrl;
        this.restTemplate = builder.restTemplate;
        this.size = builder.size;
        this.responseType = builder.responseType;
        this.ignoreErrors = builder.ignoreErrors;
    }

    /**
     * Spring Batch가 다음 아이템을 요청할 때 호출하는 핵심 메서드입니다.
     * 한 번에 하나의 아이템을 반환해야 합니다.
     *
     * @return 다음 아이템 객체. 더 이상 읽을 아이템이 없으면 null을 반환합니다.
     * @throws Exception 아이템을 읽는 도중 발생하는 모든 예외
     */
    @Override
    protected T doRead() throws Exception {
        // 아이템 버퍼가 비어있으면 새로운 페이지를 가져온다.
        // 이 로직을 반복문으로 감싸서, 오류 발생 시 다음 페이지를 계속 시도할 수 있도록 한다.
        while (items == null || currentItemIndex >= items.size()) {
            // 이미 마지막 페이지까지 모두 처리했다면, 더 이상 읽을 데이터가 없으므로 null을 반환한다.
            if (lastPage) {
                return null;
            }

            // API 요청을 위한 URI를 생성한다. (e.g., http://localhost:8080/api/members?page=0&size=10)
            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(baseUrl)
                    .queryParam("page", page)
                    .queryParam("size", size);

            PageResponse<T> pageResponse;
            try {
                // 3. RestTemplate을 사용하여 API를 호출한다.
                ResponseEntity<PageResponse<T>> response = restTemplate.exchange(
                        uriBuilder.toUriString(),
                        HttpMethod.GET,
                        null, // 요청 본문(body)은 없음
                        responseType
                );
                pageResponse = response.getBody();

            } catch (Exception ex) {
                // API 호출 중 예외가 발생했을 때의 처리
                if (ignoreErrors) {
                    // 오류 무시 옵션이 켜져 있으면, 경고 로그를 남기고 현재 페이지를 건너뛴다.
                    log.error("API call for page {} failed and will be skipped. Reason: {}", page, ex.getMessage());
                    this.page++; // 다음 페이지 번호로 이동
                    continue;    // while문의 다음 반복을 실행하여 다음 페이지를 가져오도록 시도
                } else {
                    // 오류 무시 옵션이 꺼져 있으면, 예외를 던져 Job을 즉시 실패시킨다.
                    log.error("API call for page {} failed.", page, ex);
                    throw new RuntimeException("Failed to fetch page " + page, ex);
                }
            }

            // 정상적으로 API를 호출한 후, 다음 요청을 위해 페이지 번호를 1 증가시킨다.
            this.page++;

            // 4. 응답 본문이 비어있는지 확인한다.
            if (pageResponse == null || pageResponse.getContent() == null || pageResponse.getContent().isEmpty()) {
                // 응답이 비어있으면 마지막 페이지로 간주하고 읽기를 종료한다.
                this.lastPage = true;
                // 현재 페이지가 비었으므로, while문을 다시 실행하여 lastPage 조건을 확인하고 종료하도록 한다.
                continue;
            }

            // 5. 가져온 데이터를 내부 버퍼(items)에 저장하고 상태를 업데이트한다.
            this.items = pageResponse.getContent();
            this.lastPage = pageResponse.isLast();
            this.currentItemIndex = 0;
        }

        // 6. 버퍼에서 다음 아이템을 하나씩 꺼내 반환한다.
        T nextItem = items.get(currentItemIndex);
        currentItemIndex++;
        return nextItem;
    }

    /**
     * ItemStream이 열릴 때(보통 Step 시작 시) 호출됩니다.
     * 리더의 상태를 초기화하여 재시작 시에도 일관된 동작을 보장합니다.
     */
    @Override
    protected void doOpen() {
        this.page = 0;
        this.currentItemIndex = 0;
        this.items = null;
        this.lastPage = false;
        // ExecutionContext에 저장될 이 스트림의 이름을 설정합니다.
        // Spring Batch가 이 이름을 키로 사용하여 상태(예: 읽은 아이템 개수)를 저장합니다.
        setName(HttpPageItemReader.class.getSimpleName());
    }

    /**
     * ItemStream이 닫힐 때(보통 Step 종료 시) 호출됩니다.
     * 사용했던 리소스를 정리하고 상태를 초기화합니다.
     */
    @Override
    protected void doClose() throws Exception {
        this.items = null;
        this.page = 0;
        this.currentItemIndex = 0;
        this.lastPage = false;
    }
}
