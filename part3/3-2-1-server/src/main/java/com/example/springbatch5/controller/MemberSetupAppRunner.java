package com.example.springbatch5.controller;

import com.example.springbatch5.entity.Member;
import com.example.springbatch5.entity.MemberRepository;
import lombok.AllArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@AllArgsConstructor
public class MemberSetupAppRunner implements ApplicationRunner {

    private final MemberRepository memberRepository;

    @Override
    public void run(ApplicationArguments args) {
        // 애플리케이션 시작 시 회원이 한 명도 없으면 100명을 생성합니다.
        if (memberRepository.count() == 0) {
            List<Member> members = IntStream.rangeClosed(1, 100)
                    .mapToObj(i -> new Member(null, "user" + i, "user" + i + "@example.com"))
                    .collect(Collectors.toList());
            memberRepository.saveAll(members);
        }
    }
}
