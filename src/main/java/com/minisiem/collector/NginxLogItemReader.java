package com.minisiem.collector;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Paths;
import java.util.List;

@Component
@StepScope
@Slf4j
public class NginxLogItemReader implements ItemReader<String> {

    private final LogFileReader logFileReader;
    private final String filePath;

    private List<String> lines;
    private int index;

    @Autowired
    public NginxLogItemReader(
            LogFileReader logFileReader,
            @Value("#{jobParameters['file.path']}") String filePath) {
        this.logFileReader = logFileReader;
        this.filePath = filePath;
    }

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        this.lines = logFileReader.readNewLines(Paths.get(filePath));
        this.index = 0;
        log.info("읽어온 줄 수: {}", lines.size());
    }

    @Override
    public String read() {
        if (index >= lines.size()) return null;
        return lines.get(index++);
    }
}
