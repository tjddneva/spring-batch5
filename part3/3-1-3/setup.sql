-- ONLY_FULL_GROUP_BY 설정
SET GLOBAL sql_mode=(SELECT REPLACE(@@sql_mode,'ONLY_FULL_GROUP_BY',''));


-- 일일 결제 통계 배치를 위한 테스트 데이터 삽입

-- 2025-01-01 데이터
-- 사업자번호 10002000은 2건의 결제로 총 400원이 집계되어야 함
INSERT INTO payment_source (payment_date_time, corp_name, business_registration_number, amount, created_at, updated_at) VALUES ('2025-01-01 00:01:02', '사업자1', '10002000', 100, NOW(), now());
INSERT INTO payment_source (payment_date_time, corp_name, business_registration_number, amount, created_at, updated_at) VALUES ('2025-01-01 23:01:02', '사업자1', '10002000', 300, NOW(), now());

-- 사업자번호 2002231는 1건의 결제로 총 1000원이 집계되어야 함
INSERT INTO payment_source (payment_date_time, corp_name, business_registration_number, amount, created_at, updated_at) VALUES ('2025-01-01 00:01:02', '사업자2', '2002231', 1000, NOW(), now());

-- 2025-01-02 데이터 (배치 실행 시 이 데이터는 포함되지 않아야 함)
INSERT INTO payment_source (payment_date_time, corp_name, business_registration_number, amount, created_at, updated_at) VALUES ('2025-01-02 10:00:00', '사업자1', '10002000', 500, NOW(), now());
INSERT INTO payment_source (payment_date_time, corp_name, business_registration_number, amount, created_at, updated_at) VALUES ('2025-01-02 11:00:00', '사업자3', '3002231', 2500, NOW(), now());


-- 데이터 확인
select *from payment_source;
select *from payment_daily_statistics;

-- 데이터 삭제
delete from payment_daily_statistics where  id > 0;
delete from payment_source where  id > 0;