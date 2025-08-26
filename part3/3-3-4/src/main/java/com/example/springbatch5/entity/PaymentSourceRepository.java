package com.example.springbatch5.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentSourceRepository extends JpaRepository<PaymentSource, Long>, PaymentSourceRepositoryCustom {
}
