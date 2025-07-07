select count(1) from payment_source;
select count(1) from payment_source where payment_date = '2025-04-01';
select count(1) from payment_source where payment_date = '2025-05-02';

select * from payment_source;


select * from payment_source
where payment_date = '2025-05-02'

limit 1000 offset 0
;

select * from payment_source
where payment_date = '2025-05-02'

limit 1000 offset 1000
;

select * from payment_source
where payment_date = '2025-05-02'

limit 1000 offset 10000
;

select * from payment_source
where payment_date = '2025-05-02'

limit 1000 offset 800000
;

# 1,017,503
# 33,452