-- 기존 테이블 drop
DROP TABLE IF EXISTS payment_source;

-- payment_source 테이블 생성
CREATE TABLE payment_source
(
    id                                   BIGINT AUTO_INCREMENT
        PRIMARY KEY,
    payment_date                         DATE           NOT NULL,
    discount_amount                      DECIMAL(38, 2) NOT NULL,
    final_amount                         DECIMAL(38, 2) NOT NULL,
    original_amount                      DECIMAL(38, 2) NOT NULL,
    partner_business_registration_number VARCHAR(100)   NOT NULL,
    partner_corp_name                    VARCHAR(100)   NOT NULL
);

-- payment_date 컬럼에 대한 인덱스 생성 (조회 성능 향상)
CREATE INDEX idx_payment_date
    ON payment_source (payment_date);


-- 구분자 변경
DELIMITER $$

-- (수정) 기존 프로시저: 날짜를 파라미터로 받도록 변경
DROP PROCEDURE IF EXISTS sp_insert_payment_source_batch;
CREATE PROCEDURE sp_insert_payment_source_batch(IN p_row_count INT, IN p_payment_date DATE)
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE v_batch_size INT DEFAULT 1000;
    DECLARE v_sql_values LONGTEXT DEFAULT '';

    DECLARE v_final_amount DECIMAL(38, 2);
    DECLARE v_discount_amount DECIMAL(38, 2);
    DECLARE v_original_amount DECIMAL(38, 2);
    DECLARE v_random_index INT;
    DECLARE v_partner_corp_name VARCHAR(100);
    DECLARE v_partner_biz_reg_num VARCHAR(20);

    WHILE i <= p_row_count
        DO
            -- 랜덤 데이터 생성
            SET v_final_amount = ROUND(RAND() * 1000000, 2);
            SET v_discount_amount = ROUND(RAND() * 100000, 2);
            SET v_original_amount = v_final_amount + v_discount_amount;
            SET v_random_index = FLOOR(1 + RAND() * 20);
            SET v_partner_corp_name = ELT(v_random_index, '삼성전자', 'LG전자', '현대자동차', 'SK텔레콤', '네이버', '카카오', '쿠팡', '배달의민족', '토스', '당근마켓', 'KT', '롯데그룹', '포스코', '신한금융그룹', 'KB금융그룹', '농협', '하나금융그룹', '대한항공', '아시아나항공', 'CJ그룹');
            SET v_partner_biz_reg_num = ELT(v_random_index, '000-01-00001', '000-01-00002', '000-01-00003', '000-01-00004', '000-01-00005', '000-01-00006', '000-01-00007', '000-01-00008', '000-01-00009', '000-01-00010', '000-01-00011', '000-01-00012', '000-01-00013', '000-01-00014', '000-01-00015', '000-01-00016', '000-01-00017', '000-01-00018', '000-01-00019', '000-01-00020');

            -- VALUES 구문 생성
            SET v_sql_values = CONCAT(v_sql_values,
                                      '(',
                                      v_original_amount, ',',
                                      v_discount_amount, ',',
                                      v_final_amount, ',',
                                      QUOTE(p_payment_date), ',', -- 파라미터로 받은 날짜 사용
                                      QUOTE(v_partner_corp_name), ',',
                                      QUOTE(v_partner_biz_reg_num),
                                      '),'
                               );

            -- 배치 사이즈에 도달하면 INSERT 실행
            IF (i % v_batch_size = 0) THEN
                SET @sql_query = CONCAT(
                        'INSERT INTO payment_source (original_amount, discount_amount, final_amount, payment_date, partner_corp_name, partner_business_registration_number) VALUES ',
                        TRIM(TRAILING ',' FROM v_sql_values)
                                 );
                PREPARE stmt FROM @sql_query;
                EXECUTE stmt;
                DEALLOCATE PREPARE stmt;
                SET v_sql_values = '';
            END IF;

            SET i = i + 1;
        END WHILE;

    -- 루프 후 남은 데이터 INSERT
    IF v_sql_values != '' THEN
        SET @sql_query = CONCAT(
                'INSERT INTO payment_source (original_amount, discount_amount, final_amount, payment_date, partner_corp_name, partner_business_registration_number) VALUES ',
                TRIM(TRAILING ',' FROM v_sql_values)
                         );
        PREPARE stmt FROM @sql_query;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;

END$$

-- (신규) 2025년 1월 데이터를 생성하는 프로시저
DROP PROCEDURE IF EXISTS sp_populate_january_2025;
CREATE PROCEDURE sp_populate_january_2025()
BEGIN
    DECLARE v_current_date DATE DEFAULT '2025-01-01';
    DECLARE v_end_date DATE DEFAULT '2025-01-31';

    -- 2025-01-01부터 2025-01-31까지 하루씩 증가하며 루프 실행
    WHILE v_current_date <= v_end_date
        DO
            -- 각 날짜마다 1,000건의 데이터를 삽입하는 프로시저 호출
            CALL sp_insert_payment_source_batch(1000, v_current_date);

            -- 날짜를 하루 증가
            SET v_current_date = DATE_ADD(v_current_date, INTERVAL 1 DAY);
        END WHILE;
END$$

-- 구분자를 다시 세미콜론으로 변경
DELIMITER ;

-- (최종 실행) 신규 프로시저를 호출하여 1월 한 달 치 데이터(총 31,000건) 생성
CALL sp_populate_january_2025();

select payment_date, count(1)
from payment_source

group by payment_date
order by payment_date asc;