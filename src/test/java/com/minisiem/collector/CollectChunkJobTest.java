package com.minisiem.collector;

import com.minisiem.domain.LogEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class CollectChunkJobTest {

    @TestConfiguration
    static class BatchTestConfig {
        @Bean
        public JobLauncherTestUtils jobLauncherTestUtils(JobLauncher jobLauncher, JobRepository jobRepository) {
            JobLauncherTestUtils utils = new JobLauncherTestUtils();
            utils.setJobLauncher(jobLauncher);
            utils.setJobRepository(jobRepository);
            return utils;
        }

        @Bean
        public JobRepositoryTestUtils jobRepositoryTestUtils(JobRepository jobRepository) {
            return new JobRepositoryTestUtils(jobRepository);
        }
    }

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @Autowired
    private Job collectChunkJob;

    @Autowired
    private LogEventRepository logEventRepository;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        jobRepositoryTestUtils.removeJobExecutions();
        logEventRepository.deleteAll();
        jobLauncherTestUtils.setJob(collectChunkJob);
    }

    @Test
    void validLines_areSavedToDatabase() throws Exception {
        // given
        Path logFile = tempDir.resolve("valid.log");
        Files.writeString(logFile,
                "192.168.1.1 - - [15/Aug/2026:10:00:00 +0900] \"GET /index.html HTTP/1.1\" 200 1024 \"-\" \"Mozilla/5.0\"\n" +
                "10.0.0.5 - - [15/Aug/2026:10:00:01 +0900] \"POST /api/login HTTP/1.1\" 401 256 \"-\" \"curl/7.68.0\"\n" +
                "172.16.0.10 - - [15/Aug/2026:10:00:02 +0900] \"GET /admin HTTP/1.1\" 403 128 \"-\" \"Python/3.9\""
        );

        JobParameters params = new JobParametersBuilder()
                .addString("file.path", logFile.toString())
                .addLong("run.at", System.currentTimeMillis())
                .toJobParameters();

        // when
        JobExecution execution = jobLauncherTestUtils.launchJob(params);

        // then
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        StepExecution step = getStep(execution, "collectChunkStep");
        assertThat(step.getReadCount()).isEqualTo(3);
        assertThat(step.getWriteCount()).isEqualTo(3);
        assertThat(step.getSkipCount()).isEqualTo(0);
        assertThat(logEventRepository.count()).isEqualTo(3);
    }

    @Test
    void parseFailures_areCountedAsSkipped() throws Exception {
        // given
        Path logFile = tempDir.resolve("mixed.log");
        Files.writeString(logFile,
                "192.168.1.1 - - [15/Aug/2026:10:00:00 +0900] \"GET /index.html HTTP/1.1\" 200 1024 \"-\" \"Mozilla/5.0\"\n" +
                "INVALID LOG LINE\n" +
                "172.16.0.10 - - [15/Aug/2026:10:00:02 +0900] \"GET /admin HTTP/1.1\" 403 128 \"-\" \"Python/3.9\"\n" +
                "malformed entry"
        );

        JobParameters params = new JobParametersBuilder()
                .addString("file.path", logFile.toString())
                .addLong("run.at", System.currentTimeMillis())
                .toJobParameters();

        // when
        JobExecution execution = jobLauncherTestUtils.launchJob(params);

        // then
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        StepExecution step = getStep(execution, "collectChunkStep");
        assertThat(step.getReadCount()).isEqualTo(4);
        assertThat(step.getWriteCount()).isEqualTo(2);
        assertThat(step.getSkipCount()).isEqualTo(2);
    }

    private StepExecution getStep(JobExecution execution, String stepName) {
        return execution.getStepExecutions().stream()
                .filter(s -> s.getStepName().equals(stepName))
                .findFirst()
                .orElseThrow();
    }
}
