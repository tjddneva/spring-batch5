package com.example.springbatch5.job;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.scope.context.ChunkContext;

@Slf4j
public class ChunkDurationTrackerListener implements ChunkListener {

    @Override
    public void beforeChunk(ChunkContext context) {
        // StepExecution에서 현재까지 커밋된 청크 수를 가져와 현재 청크 번호를 계산합니다. (0부터 시작하므로 +1)
        context.setAttribute("startTime", System.currentTimeMillis());
    }

    @Override
    public void afterChunk(ChunkContext context) {
        long startTime = (long) context.getAttribute("startTime");
        long endTime = System.currentTimeMillis();
        long durationMillis = endTime - startTime;

        // 처리가 완료된 청크 번호를 가져옵니다.
        int chunkNumber = (int) context.getStepContext().getStepExecution().getCommitCount();

        // 밀리초 단위의 소요 시간을 시, 분, 초로 변환합니다.
        long hours = durationMillis / (1000 * 60 * 60);
        long minutes = (durationMillis % (1000 * 60 * 60)) / (1000 * 60);
        long seconds = (durationMillis % (1000 * 60)) / 1000;
        long millis = durationMillis % 1000;

        // 사람이 읽기 좋은 형태의 문자열로 포맷팅합니다.
        // 청크 처리 시간은 보통 짧으므로, 분 단위 이하에서는 초와 밀리초까지 보여주는 것이 유용합니다.
        String duration;
        if (hours > 0) {
            duration = String.format("%d시간 %d분 %d초", hours, minutes, seconds);
        } else if (minutes > 0) {
            duration = String.format("%d분 %d초", minutes, seconds);
        } else if (seconds > 0) {
            duration = String.format("%d.%03d초", seconds, millis);
        } else {
            duration = String.format("%dms", millis);
        }

        log.info("Chunk #{} duration: {}", chunkNumber, duration);
    }
}
