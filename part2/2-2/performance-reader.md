# Reader Performance 측정

## No Offset 성능 비교

| rows      | Limit Offset | Cursor    |
|-----------|--------------|-----------|
| 10,000    | 471 ms       | 472 ms    |
| 50,000    | 2,102 ms     | 1,239 ms  |
| 100,000   | 5,893 ms     | 2,108 ms  |
| 500,000   | 13,779 ms    | 9,840 ms  |
| 1,000,000 | 21,953 ms    | 23,104 ms |
| 5,000,000 | 97,587 ms    | 86,381 ms |

데이터가 적을 때는 큰 차이가 없지만, 50만건 이상으로 데이터가 많아질수록 No Offset 방식이 Limit Offset 방식보다 훨씬 빠른 성능을 보입니다.
**특히 100만건의 데이터에서는 약 26배 더 빠른 성능을 보여줍니다.**

## No Offset 1,000,000 rows Chunk 별 성능 측정

<details>
<summary>rows 1,000,000, 전체 소요 시간 21,953 ms</summary>

* Chunk #1 duration: 73ms
* Chunk #2 duration: 15ms
* Chunk #3 duration: 18ms
* ...
* Chunk #500 duration: 13ms
* Chunk #501 duration: 25ms
* Chunk #502 duration: 14ms
* ...
* Chunk #999 duration: 16ms
* Chunk #1000 duration: 14ms
* Chunk #1001 duration: 6ms

</details>

No Offset 방식은 조회 시작부터 끝까지 일정한 성능을 보입니다.
**이는 초반, 중간, 후반 청크 모두 균등하게 빠른 응답 속도를 내는 것을 의미합니다.**