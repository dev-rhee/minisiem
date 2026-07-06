package com.minisiem.parser;

import com.minisiem.domain.LogEvent;

import java.util.Optional;

public interface LogParser {

    /**
     * 이 파서가 처리할 수 있는 로그 타입 이름을 반환한다.
     * application.yml의 sources[].type 값과 매핑된다.
     */
    String getSupportedType();

    /**
     * 로그 한 줄을 파싱해서 LogEvent로 변환한다.
     * 파싱 실패 시 Optional.empty()를 반환한다.
     */
    Optional<LogEvent> parse(String line);
}