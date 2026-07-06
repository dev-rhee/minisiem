package com.minisiem.collector;

import com.minisiem.config.LogSourceConfig;
import com.minisiem.domain.LogEvent;
import com.minisiem.domain.LogEventRepository;
import com.minisiem.parser.LogParser;
import com.minisiem.parser.LogParserRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class LogCollectJob {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final LogFileReader logFileReader;
    private final LogParserRegistry parserRegistry;
    private final LogEventRepository logEventRepository;
    private final LogSourceConfig logSourceConfig;

    @Bean
    public Job collectLogJob() {
        return new JobBuilder("collectLogJob", jobRepository)
                .start(collectStep())
                .build();
    }

    @Bean
    public Step collectStep() {
        return new StepBuilder("collectStep", jobRepository)
                .tasklet(collectTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Tasklet collectTasklet() {
        return (contribution, chunkContext) -> {
            List<LogSourceConfig.LogSource> sources = logSourceConfig.getSources();
            if (sources == null || sources.isEmpty()) {
                log.warn("수집할 로그 소스가 설정되지 않았습니다. application.yml을 확인하세요.");
                return RepeatStatus.FINISHED;
            }

            for (LogSourceConfig.LogSource source : sources) {
                Optional<LogParser> parser = parserRegistry.getParser(source.getType());
                if (parser.isEmpty()) continue;

                processSource(source, parser.get());
            }
            return RepeatStatus.FINISHED;
        };
    }

    private void processSource(LogSourceConfig.LogSource source, LogParser parser) {
        Path path = Paths.get(source.getPath());

        if (!Files.exists(path)) {
            log.warn("경로가 존재하지 않습니다: {}", path);
            return;
        }

        // 디렉토리면 하위 .log 파일들을 순회, 파일이면 직접 처리
        if (Files.isDirectory(path)) {
            String pattern = source.getPattern() != null ? source.getPattern() : "*.log";
            try (Stream<Path> files = Files.list(path)) {
                files.filter(p -> matchesPattern(p.getFileName().toString(), pattern))
                        .forEach(file -> processFile(file, parser));
            } catch (IOException e) {
                log.error("디렉토리 접근 실패: {}", path, e);
            }
        } else {
            processFile(path, parser);
        }
    }

    private boolean matchesPattern(String fileName, String pattern) {
        // 간단한 와일드카드 매칭 (*.log, access_*.log 등)
        String regex = pattern.replace(".", "\\.").replace("*", ".*");
        return fileName.matches(regex);
    }

    private void processFile(Path filePath, LogParser parser) {
        List<LogEvent> events = logFileReader.readNewLines(filePath).stream()
                .map(parser::parse)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

        if (!events.isEmpty()) {
            logEventRepository.saveAll(events);
            log.info("[{}][{}] {}건 저장", parser.getSupportedType(),
                    filePath.getFileName(), events.size());
        }
    }
}