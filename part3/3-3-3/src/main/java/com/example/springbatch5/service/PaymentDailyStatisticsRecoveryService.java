package com.example.springbatch5.service;


import com.example.springbatch5.entity.PaymentDailyStatistics;
import com.example.springbatch5.entity.PaymentDailyStatisticsRepository;
import com.example.springbatch5.entity.PaymentDailyStatisticsUniqueKey;
import com.example.springbatch5.job.PaymentStatisticsDailySum;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentDailyStatisticsRecoveryService {

    private final PaymentDailyStatisticsRepository paymentDailyStatisticsRepository;

    /**
     * Reader가 읽어온 일일 결제 합산 데이터(targets)를 DB에 저장하거나 업데이트합니다.
     * 이 메서드는 다음과 같은 로직을 수행합니다.
     * 1. 사업자번호와 결제일자를 기준으로 데이터가 DB에 없는 경우: 신규 데이터로 판단하여 INSERT 합니다.
     * 2. 사업자번호와 결제일자를 기준으로 데이터가 DB에 있는 경우:
     *    - 금액(amount)이 일치하면: 아무 작업도 하지 않습니다.
     *    - 금액(amount)이 일치하지 않으면: 기존 데이터의 금액을 업데이트합니다.
     *
     * @param targets Reader가 조회한 일일 결제 합산 DTO 리스트
     */
    @Transactional
    public void recovery(List<PaymentStatisticsDailySum> targets) {

        // 1. Reader가 읽어온 DTO 리스트에서 고유 키(사업자번호, 결제일자) 리스트를 추출합니다.
        //    이 키들은 DB에서 기존 데이터를 조회하는 데 사용됩니다.
        List<PaymentDailyStatisticsUniqueKey> uniqueKeys = targets.stream()
                .map(PaymentStatisticsDailySum::toUniqueKey)
                .collect(Collectors.toList());

        // 2. 추출된 고유 키 리스트를 사용하여 DB에서 이미 존재하는 모든 데이터를 한 번의 쿼리로 조회합니다.
        List<PaymentDailyStatistics> existingEntities = paymentDailyStatisticsRepository.findBy(uniqueKeys);

        // 3. 조회된 기존 엔티티 리스트를 Map으로 변환합니다.
        //    Key는 고유 키(PaymentDailyStatisticsUniqueKey), Value는 엔티티(PaymentDailyStatistics)입니다.
        //    Map을 사용하면 이후 단계에서 각 DTO에 해당하는 기존 엔티티를 O(1) 시간 복잡도로 빠르게 찾을 수 있습니다.
        Map<PaymentDailyStatisticsUniqueKey, PaymentDailyStatistics> existingEntitiesMap = existingEntities.stream()
                .collect(Collectors.toMap(
                        PaymentDailyStatistics::toUniqueKey,
                        entity -> entity
                ));

        // 4. DB에 새로 추가해야 할 신규 데이터(DTO)를 담을 리스트를 초기화합니다.
        List<PaymentStatisticsDailySum> newTargetsDto = new ArrayList<>();

        // 5. Reader가 읽어온 모든 DTO(targets)를 하나씩 순회하며 신규/업데이트 대상을 판별합니다.
        for (PaymentStatisticsDailySum target : targets) {
            PaymentDailyStatisticsUniqueKey uniqueKey = target.toUniqueKey();
            // 3번에서 만든 Map을 사용하여 현재 DTO에 해당하는 기존 엔티티가 있는지 확인합니다.
            PaymentDailyStatistics existingEntity = existingEntitiesMap.get(uniqueKey);

            if (existingEntity != null) {
                // 5-1. [업데이트] 기존 엔티티가 존재하는 경우
                //      DB에 저장된 금액과 새로 계산된 금액이 다른지 비교합니다.
                if (!existingEntity.getAmount().equals(target.getTotalAmount())) {
                    // 금액이 다르면, 기존 엔티티의 금액을 새로운 값으로 업데이트합니다.
                    // 이 변경 사항은 트랜잭션이 커밋될 때 JPA의 Dirty Checking에 의해 자동으로 UPDATE 쿼리가 실행됩니다.
                    System.out.println("기존 데이터와 amount 불일치(변경 대상): 사업자번호=" + target.getBusinessRegistrationNumber() +
                            ", 결제일자=" + target.getPaymentDate() +
                            ", 기존 amount=" + existingEntity.getAmount() +
                            ", 새 amount=" + target.getTotalAmount());
                    existingEntity.updateAmount(target.getTotalAmount());
                } else {
                    // 금액이 같으면, 아무 작업도 하지 않습니다.
                    System.out.println("기존 데이터와 amount 일치 (변경 없음): 사업자번호=" + target.getBusinessRegistrationNumber() +
                            ", 결제일자=" + target.getPaymentDate() +
                            ", amount=" + target.getTotalAmount());
                }
            } else {
                // 5-2. [신규] 기존 엔티티가 존재하지 않는 경우
                //      이 DTO는 신규 데이터이므로, 4번에서 만든 신규 대상 리스트에 추가합니다.
                newTargetsDto.add(target);
            }
        }

        // 6. 신규 대상 리스트에 데이터가 있는 경우, 이들을 엔티티로 변환하여 DB에 일괄 저장(Bulk Insert)합니다.
        if (!newTargetsDto.isEmpty()) {
            System.out.println("새로운 대상 저장: " + newTargetsDto.size() + "건");
            List<PaymentDailyStatistics> newEntities = newTargetsDto.stream()
                    .map(PaymentStatisticsDailySum::toEntity)
                    .collect(Collectors.toList());
            paymentDailyStatisticsRepository.saveAll(newEntities);
        }
    }
}
