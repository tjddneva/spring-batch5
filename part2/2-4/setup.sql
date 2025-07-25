-- 기존 테이블이 있다면 삭제
DROP TABLE IF EXISTS users;

-- users 테이블 생성
CREATE TABLE users
(
    id    BIGINT AUTO_INCREMENT PRIMARY KEY,
    grade VARCHAR(50) NOT NULL
);

-- grade 컬럼에 대한 인덱스 생성 (조회 성능 향상)
CREATE INDEX idx_grade ON users (grade);

-- SQL 클라이언트가 세미콜론(;)을 만나도 프로시저 정의가 끝나지 않도록 구분자를 변경합니다.
DELIMITER $$

-- 만약 이전에 생성된 프로시저가 있다면 삭제합니다.
DROP PROCEDURE IF EXISTS sp_insert_user_batch;

-- 대량의 테스트 데이터를 배치(batch) 형태로 삽입하는 저장 프로시저를 생성합니다.
CREATE PROCEDURE sp_insert_user_batch(IN p_row_count INT)
BEGIN
    -- 루프 및 배치 관련 변수 선언
    DECLARE i INT DEFAULT 1;
    DECLARE v_batch_size INT DEFAULT 1000; -- 한 번에 INSERT할 데이터 묶음 크기
    DECLARE v_sql_values LONGTEXT DEFAULT '';
    -- INSERT할 값들을 누적할 문자열 변수

    -- 데이터 생성을 위한 변수 선언
    DECLARE v_grade VARCHAR(50);

    -- 요청된 row 개수만큼 루프를 실행합니다.
    WHILE i <= p_row_count
        DO
            -- 등급은 초기 등급 INIT 으로 설정
            SET v_grade = 'INIT';

            -- 생성된 데이터를 VALUES 구문 형식의 문자열로 v_sql_values에 추가합니다.
            SET v_sql_values = CONCAT(v_sql_values,
                                      '(',
                                      QUOTE(v_grade),
                                      '),'
                               );

            -- 배치 크기(1000개)에 도달했을 경우, 모아둔 데이터를 한 번에 INSERT 합니다.
            IF (i % v_batch_size = 0) THEN
                -- TRIM 함수를 사용하여 마지막에 붙은 불필요한 쉼표(,)를 안전하게 제거합니다.
                SET @sql_query = CONCAT(
                        'INSERT INTO users (grade) VALUES ',
                        TRIM(TRAILING ',' FROM v_sql_values)
                                 );

                -- 동적 SQL을 준비하고 실행합니다.
                PREPARE stmt FROM @sql_query;
                EXECUTE stmt;
                DEALLOCATE PREPARE stmt;

                -- 다음 배치를 위해 VALUES 문자열을 초기화합니다.
                SET v_sql_values = '';
            END IF;

            SET i = i + 1;
        END WHILE;

    -- 루프가 끝난 후, 처리되지 않고 남아있는 데이터가 있다면 마지막으로 INSERT 합니다.
    IF v_sql_values != '' THEN
        SET @sql_query = CONCAT(
                'INSERT INTO users (grade) VALUES ',
                TRIM(TRAILING ',' FROM v_sql_values)
                         );

        PREPARE stmt FROM @sql_query;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;

END$$

-- 구분자를 다시 기본값인 세미콜론(;)으로 변경합니다.
DELIMITER ;

select * from users;

-- 1,000건 데이터 삽입
CALL sp_insert_user_batch(1000);

-- 5,000건 데이터 삽입
CALL sp_insert_user_batch(5000);

-- 10,000건 데이터 삽입
CALL sp_insert_user_batch(10000);

-- 50,000건 데이터 삽입
CALL sp_insert_user_batch(50000);

-- 100,000건 데이터 삽입
CALL sp_insert_user_batch(100000);

-- 500,000건 데이터 삽입
CALL sp_insert_user_batch(500000);