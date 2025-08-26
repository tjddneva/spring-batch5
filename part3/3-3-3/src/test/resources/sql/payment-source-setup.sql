-- 2025-01-05 데이터
-- 사업자번호 10002000은 2건의 결제로 총 400원이 집계되어야 함
INSERT INTO payment_source (payment_date_time, corp_name, business_registration_number, amount, created_at, updated_at)
VALUES ('2025-01-05 00:01:02', '사업자1', '10002000', 100, NOW(), now());
INSERT INTO payment_source (payment_date_time, corp_name, business_registration_number, amount, created_at, updated_at)
VALUES ('2025-01-05 23:01:02', '사업자1', '10002000', 300, NOW(), now());

-- 사업자번호 2002231는 1건의 결제로 총 1000원이 집계되어야 함
INSERT INTO payment_source (payment_date_time, corp_name, business_registration_number, amount, created_at, updated_at)
VALUES ('2025-01-05 00:01:02', '사업자2', '2002231', 1000, NOW(), now());


-- 2025-01-04 데이터 (배치 실행 시 이 데이터는 포함되지 않아야 함)
INSERT INTO payment_source (payment_date_time, corp_name, business_registration_number, amount, created_at, updated_at)
VALUES ('2025-01-04 10:00:00', '사업자1', '10002000', 500, NOW(), now());

-- 2025-01-03 데이터 (배치 실행 시 이 데이터는 포함되지 않아야 함)
INSERT INTO payment_source (payment_date_time, corp_name, business_registration_number, amount, created_at, updated_at)
VALUES ('2025-01-03 10:00:00', '사업자2', '2002231', 500, NOW(), now());

-- 2025-01-03 데이터 (배치 실행 시 이 데이터는 포함되지 않아야 함)
INSERT INTO payment_source (payment_date_time, corp_name, business_registration_number, amount, created_at, updated_at)
VALUES ('2025-01-03 10:00:00', '사업자3', '332231', 500, NOW(), now());
