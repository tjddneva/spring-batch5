package com.example.springbatch5.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

import java.util.List;
import java.util.stream.Collectors;

@SpringBootTest(properties = {"spring.batch.job.enabled=false"})
public class UserRepositoryPerformanceTest {

    @Autowired
    private UserRepository userRepository;


    @Test
    @DisplayName("단일 방식")
    void test_1() {
        // 테스트를 위해 모든 사용자를 조회합니다.
        List<User> users = userRepository.findAll();
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        for (User user : users) {
            long userId = user.getId();
            if (userId % 3 == 0) {
                user.setGrade(Grade.VIP);
            } else if (userId % 2 == 0) {
                user.setGrade(Grade.PREMIUM);
            } else {
                user.setGrade(Grade.BASIC);
            }
        }
        userRepository.saveAll(users);
        stopWatch.stop();
        System.out.println("========================================");
        System.out.println("업데이트 소요 시간: " + stopWatch.getTotalTimeMillis() + "ms");
        System.out.println("========================================");
    }

    @Test
    @DisplayName("update in 방식")
    void test_2() {
        // 테스트를 위해 모든 사용자를 조회합니다.
        List<User> users = userRepository.findAll();
        List<Long> ids = users.stream().map(User::getId).collect(Collectors.toList());
        // 사용자 ID를 조건에 따라 그룹화하여 등급을 업데이트합니다.
        List<Long> vipIds = ids.stream().filter(id -> id % 3 == 0).collect(Collectors.toList());
        List<Long> premiumIds = ids.stream().filter(id -> id % 3 != 0 && id % 2 == 0).collect(Collectors.toList());
        List<Long> basicIds = ids.stream().filter(id -> id % 3 != 0 && id % 2 != 0).collect(Collectors.toList());
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        if (!vipIds.isEmpty()) {
            userRepository.updateGrade(Grade.VIP, vipIds);
        }
        if (!premiumIds.isEmpty()) {
            userRepository.updateGrade(Grade.PREMIUM, premiumIds);
        }
        if (!basicIds.isEmpty()) {
            userRepository.updateGrade(Grade.BASIC, basicIds);
        }
        stopWatch.stop();
        System.out.println("========================================");
        System.out.println("업데이트 소요 시간: " + stopWatch.getTotalTimeMillis() + "ms");
        System.out.println("========================================");
    }
}
