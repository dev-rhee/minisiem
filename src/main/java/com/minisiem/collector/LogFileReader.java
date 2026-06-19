package com.minisiem.collector;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class LogFileReader {

    // 파일별 마지막으로 읽은 위치 저장
    private final Map<Path, Long> offsetMap = new ConcurrentHashMap<>();

    public List<String> readNewLines(Path filePath) {
        long offset = offsetMap.getOrDefault(filePath, 0L);
        List<String> lines = new ArrayList<>();

        try (RandomAccessFile raf = new RandomAccessFile(filePath.toFile(), "r")) {
            if (raf.length() < offset) {
                // 로그 로테이션 감지 → 처음부터 다시 읽기
                offset = 0L;
                log.info("로그 로테이션 감지: {}", filePath.getFileName());
            }
            raf.seek(offset);
            String line;
            while ((line = raf.readLine()) != null) {
                lines.add(new String(line.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8));
            }
            offsetMap.put(filePath, raf.getFilePointer());
        } catch (IOException e) {
            log.error("로그 파일 읽기 실패: {}", filePath, e);
        }
        return lines;
    }
}