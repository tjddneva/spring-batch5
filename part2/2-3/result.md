# JpaCursorItemReader와 스트리밍 데이터 읽기 (MySQL Connector/J 기준)

`JpaCursorItemReader`는 Spring Batch에서 대용량의 데이터베이스 레코드를 효과적으로 읽기 위한 ItemReader 구현체입니다. JPA(Java Persistence API)를 사용하여 커서(Cursor) 기반의 스트리밍 방식으로 데이터를 읽어오므로, 전체 조회 결과를 애플리케이션 메모리에 로드하지 않아 `OutOfMemoryError`를 방지할 수 있습니다.

이 문서는 MySQL 8 버전과 MySQL Connector/J를 사용하는 환경을 기준으로 `JpaCursorItemReader`의 핵심 동작 원리인 커서 스트리밍에 대해 설명합니다.

## 커서 스트리밍이란?

커서 스트리밍의 핵심을 이해하기 위해서는 먼저 JDBC가 대량의 데이터를 가져오는 방식에 대해 알아야 합니다.

### 1. 클라이언트 사이드 커서 (Client-Side Cursor) - 기본 방식

JDBC 표준에는 `Statement.setFetchSize()` 메서드가 있어, 서버로부터 한 번에 가져올 레코드 수를 설정할 수 있습니다. 하지만 **MySQL Connector/J는 이 표준을 그대로 지원하지 않습니다.**

`setFetchSize`를 별도로 설정하지 않고 일반적인 조회 쿼리(`Statement.executeQuery()`)를 실행하면, Connector/J는 다음과 같이 동작합니다.

1. MySQL 서버에 쿼리를 전송하고, **조회된 모든 결과를 한 번에 다운로드**합니다.
2. 다운로드한 모든 데이터를 클라이언트(애플리케이션)의 **메모리에 캐시**합니다.
3. 모든 데이터가 다운로드될 때까지 애플리케이션은 Blocking 상태가 됩니다.
4. 이후 `ResultSet.next()`를 호출하면, 실제 DB 서버와 통신하는 것이 아니라 메모리에 캐시된 데이터에서 값을 가져오므로 매우 빠릅니다.

![](https://github.com/cheese10yun/TIL/raw/master/assets/rea-mysql-flow-1.png)

* **장점**:
  * 일단 모든 데이터를 가져온 후에는 메모리에서 직접 읽으므로 레코드별 조회 속도가 매우 빠릅니다.
* **단점**:
  * **대용량 데이터 조회 시 애플리케이션의 메모리 사용량이 급증**하여 `OutOfMemoryError`가 발생할 위험이 큽니다.
  * 모든 결과를 다운로드할 때까지 오랜 시간이 소요될 수 있습니다.

### 2. 서버 사이드 커서 (Server-Side Cursor) - 스트리밍 방식

이러한 메모리 문제를 해결하기 위해 MySQL Connector/J는 "ResultSet Streaming"이라는 방식을 지원합니다.

`Statement.setFetchSize(Integer.MIN_VALUE)`라는 특수한 설정을 적용하면, Connector/J는 조회 결과를 한 번에 모두 가져오는 대신, **데이터베이스 서버로부터 한 건씩 데이터를 스트리밍**해옵니다.

1. `ResultSet.next()`를 호출할 때마다 네트워크 통신을 통해 DB 서버로부터 다음 레코드 한 건을 가져옵니다.
2. 데이터를 클라이언트 메모리에 쌓아두지 않으므로, 아무리 많은 데이터를 조회해도 메모리 사용량은 거의 일정하게 유지됩니다.

![](https://github.com/cheese10yun/TIL/raw/master/assets/real_mysql_2222.png)

* **장점**:
  * **메모리 사용량이 매우 낮고 일정**하여 대용량 데이터 처리에 안정적입니다.
* **단점**:
  * 각 레코드를 가져올 때마다 네트워크 통신이 발생하므로 클라이언트 사이드 커서 방식보다 처리 속도가 느릴 수 있습니다.
  * 배치 처리 시간 동안 데이터베이스 커넥션을 계속 열어두어야 하므로, DB 타임아웃 설정에 유의해야 합니다.

## Spring Batch 5의 JpaCursorItemReader는 어떤 방식을 사용할까?

결론적으로, **`JpaCursorItemReader`는 내부적으로 서버 사이드 커서(ResultSet Streaming) 방식을 사용합니다.**

`JpaCursorItemReader`는 Hibernate와 같은 JPA 구현체를 통해 동작하는데, Hibernate는 커서 기반의 조회를 요청할 때 내부적으로 위에서 설명한 JDBC의 스트리밍 방식(`setFetchSize(Integer.MIN_VALUE)`)을 활용합니다.

따라서 `JpaCursorItemReader`를 사용하면 수백만, 수천만 건의 데이터라도 OOM 걱정 없이 안정적으로 처리할 수 있습니다.

### `fetchSize` 옵션에 대한 오해

`JpaCursorItemReaderBuilder`나 `HibernateCursorItemReaderBuilder`에서 `fetchSize`라는 옵션을 볼 수 있습니다.

```java
.fetchSize(100) // 이 옵션은 JDBC의 setFetchSize와 다르다!
```

여기서의 `fetchSize`는 Spring Batch의 **청크(Chunk) 사이즈와 유사한 개념**입니다. Reader가 내부적으로 스트리밍을 통해 데이터를 한 건씩 읽어들여, `fetchSize` 만큼의 개수가 모이면 그 묶음을 `ItemProcessor`나 `ItemWriter`에게 전달하는 역할을 합니다. 이는 JDBC의 `setFetchSize`와는 직접적인 관련이 없으므로 혼동하지 않도록 주의해야 합니다.

## 주의 사항

서버 사이드 커서 방식은 장시간의 데이터베이스 커넥션을 요구합니다. 배치가 실행되는 동안 커넥션을 반납하지 않고 계속 사용하기 때문에, 처리할 데이터가 매우 많아지면 DB 서버의 `wait_timeout` 설정보다 배치 처리 시간이 길어져 커넥션이 끊기는 문제가 발생할 수 있습니다.

따라서 `JpaCursorItemReader`를 사용할 때는 DB 타임아웃 설정을 충분히 길게 가져가거나, 배치가 타임아웃 시간 내에 완료될 수 있도록 튜닝해야 합니다.

## AbstractSelectionQuery.stream() 코드 분석

```java

@Override
public Stream stream() {
    final ScrollableResultsImplementor scrollableResults = scroll(ScrollMode.FORWARD_ONLY);
    final ScrollableResultsIterator iterator = new ScrollableResultsIterator<>(scrollableResults);
    final Spliterator spliterator = spliteratorUnknownSize(iterator, Spliterator.NONNULL);

    final Stream stream = StreamSupport.stream(spliterator, false);
    return (Stream) stream.onClose(scrollableResults::close);
}
```

이 코드는 Hibernate/JPA와 같은 현대적인 데이터 액세스 프레임워크가 대용량 데이터셋을 효율적으로 처리하는 방식의 핵심을 보여줍니다. 코드는 전체적으로 **클라이언트 사이드(Java 애플리케이션)**에서 실행되지만, 그 동작은 클라이언트와 **서버 사이드(데이터베이스)** 간의 정교한 협력을 통해 이루어집니다.

### 서버 사이드 (데이터베이스) 동작

1. **쿼리 실행**: `scroll(ScrollMode.FORWARD_ONLY)`가 호출되면, 데이터베이스는 SQL 쿼리를 받아 실행합니다.
2. **커서 생성**: 모든 결과를 한 번에 모아 전송하는 대신, 데이터베이스는 서버에 **커서(Cursor)**를 생성합니다. 커서는 결과셋에 대한 포인터와 같으며, 실제 데이터는 여전히 데이터베이스 서버의 메모리나 임시 디스크 공간에 남아있습니다.
3. **데이터 요청 대기**: 데이터베이스는 클라이언트(애플리케이션)가 결과셋에서 데이터를 요청하기를 기다립니다. 클라이언트가 요청할 때만 데이터를 전송합니다.

### 클라이언트 사이드 (Java 애플리케이션) 동작

1. **스트림의 지연 생성 (Lazy Creation)**:
  * `stream()` 메서드가 호출되면 `ScrollableResults` 객체를 얻습니다. 이 객체는 서버 사이드 커서에 대한 가벼운 핸들(참조)입니다.
  * 이 객체를 `Iterator`로 감싸고, 최종적으로 Java `Stream`을 생성합니다.
  * **이 시점에서는 데이터베이스로부터 거의 아무런 데이터도 전송되지 않았습니다.** `Stream`은 "지연(lazy)" 방식으로 동작하므로, 아직 도착하지 않은 데이터를 처리하기 위한 계획일 뿐입니다.

2. **요청 기반 데이터 패치 (Fetch-on-Demand)**:
  * 실제 작업은 스트림의 **최종 연산(terminal operation)**(예: `forEach`, `collect`, `findFirst`)이 호출될 때 시작됩니다.
  * 스트림 처리가 시작되면, 내부적으로 `Iterator`에게 다음 아이템을 요청합니다 (`iterator.next()`).
  * 이 요청은 `ScrollableResults` 객체를 통해 데이터베이스에 "다음 레코드를 줘" 라는 네트워크 요청을 보냅니다. (실제로는 JDBC의 `fetchSize` 파라미터에 따라 작은 배치 단위로 요청할 수 있습니다.)

3. **레코드 단위 처리**:
  * 데이터베이스는 요청받은 레코드(들)를 네트워크를 통해 전송합니다.
  * 클라이언트는 데이터를 받아 Java 엔티티 객체로 변환합니다.
  * 이 단일 객체는 스트림 파이프라인(`filter`, `map` 등)을 통해 처리됩니다.
  * 이 과정이 반복됩니다: 스트림이 아이템을 요청하고, 클라이언트가 서버로부터 데이터를 가져와서 처리합니다.

4. **자원 정리**:
  * `stream.onClose(scrollableResults::close)` 부분은 매우 중요합니다. 스트림이 모든 요소를 소진했거나 `try-with-resources` 구문으로 인해 닫힐 때, `ScrollableResults`의 `close()` 메서드가 자동으로 호출됩니다.
  * 이 호출은 데이터베이스 서버에 커서를 닫고 관련 자원을 해제하라는 마지막 메시지를 보냅니다.

### 클라이언트-서버 동작 요약

| 기능          | 클라이언트 사이드 (Java 앱) 동작                           | 서버 사이드 (데이터베이스) 동작                      |
|:------------|:------------------------------------------------|:----------------------------------------|
| **초기화**     | `query.stream()` 호출                             | SQL 수신, 실행 후 결과셋 준비                     |
| **데이터 저장**  | 현재 처리 중인 **단일** 객체만 메모리에 유지                     | **전체** 결과셋을 커서를 통해 관리                   |
| **데이터 전송**  | 필요할 때마다 서버로부터 레코드를 하나씩 (또는 작은 배치로) 가져옴          | 요청받을 때마다 클라이언트로 레코드를 하나씩 (또는 작은 배치로) 보냄 |
| **처리**      | 각 객체가 도착할 때마다 Java 스트림 로직(`map`, `filter` 등) 실행 | SQL의 `WHERE` 절을 통해 초기 데이터 필터링 및 선택 수행   |
| **메모리 사용량** | 전체 레코드 수와 관계없이 **매우 낮고 일정함**                    | 전체 결과셋을 관리하므로 높을 수 있음                   |
| **자원 관리**   | 스트림을 닫아 서버에 커서 종료를 요청할 책임                       | 커서 종료 요청을 받으면 관련 DB 자원을 해제할 책임          |

### 핵심 결론

`query.getResultList()`와 같은 단순한 접근 방식과의 가장 큰 차이점은 **메모리 효율성**입니다.

* **`getResultList()` (단순 방식)**: 클라이언트가 "모든 데이터를 줘"라고 요청합니다. 서버는 수백만 개의 레코드를 모두 전송하고, 클라이언트는 이 모든 것을 메모리의 `List`에 담으려다 `OutOfMemoryError`를 발생시킬 수 있습니다.
* **`stream()` (스마트 방식)**: 클라이언트가 "데이터 보낼 준비를 해"라고 요청합니다. 서버는 수백만 개의 레코드를 준비만 하고 아무것도 보내지 않습니다. 이후 클라이언트는 "자, 1번 레코드 줘" -> 처리 -> "이제 2번 레코드 줘" -> 처리... 와 같이 동작합니다. 이 방식 덕분에 클라이언트의 메모리 사용량은 전체 과정 동안 낮고 일정하게 유지됩니다.