package com.minisiem.collector;

import com.minisiem.domain.LogEvent;
import com.minisiem.domain.LogEventRepository;
import com.minisiem.parser.NginxLogParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
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
    private final NginxLogParser nginxLogParser;
    private final LogEventRepository logEventRepository;

    @Value("${siem.log.watch-dir}")
    private String watchDir;

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
            try (Stream<Path> files = Files.list(Paths.get(watchDir))) {
                files.filter(p -> p.toString().endsWith(".log"))
                        .forEach(this::processFile);
            } catch (IOException e) {
                log.error("디렉토리 접근 실패: {}", watchDir, e);
            }
            return RepeatStatus.FINISHED;
        };
    }

    private void processFile(Path path) {
        List<LogEvent> events = logFileReader.readNewLines(path).stream()
                .map(nginxLogParser::parse)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

        if (!events.isEmpty()) {
            logEventRepository.saveAll(events);
            log.info("[{}] {}건 저장 완료", path.getFileName(), events.size());
        }
    }
}