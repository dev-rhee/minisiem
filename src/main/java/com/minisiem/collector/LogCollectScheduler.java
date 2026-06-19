package com.minisiem.collector;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class LogCollectScheduler {

    private final JobLauncher jobLauncher;
    private final Job collectLogJob;

    @Scheduled(fixedDelay = 30_000) // 30초마다 실행
    public void run() {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("run.at", System.currentTimeMillis())
                    .toJobParameters();
            jobLauncher.run(collectLogJob, params);
        } catch (Exception e) {
            log.error("배치 실행 실패", e);
        }
    }
}