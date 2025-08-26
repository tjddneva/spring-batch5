package com.example.springbatch5.reader;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class MemberResponse {
    private final Long id;
    private final String name;
    private final String email;
}
