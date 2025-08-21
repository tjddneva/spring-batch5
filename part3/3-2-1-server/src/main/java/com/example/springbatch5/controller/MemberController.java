package com.example.springbatch5.controller;

import com.example.springbatch5.entity.Member;
import com.example.springbatch5.entity.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberRepository memberRepository;

    @GetMapping
    public Page<Member> getMembers(@PageableDefault(page = 0, size = 10) Pageable pageable) {
//        if (pageable.getPageNumber() == 4) {
//            throw new RuntimeException("예외 발생");
//        }
        return memberRepository.findAll(pageable);
    }
}
