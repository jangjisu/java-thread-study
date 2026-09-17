package study.thread.step03;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import study.thread.support.Sleeper;
import study.thread.support.Timeline;

/**
 * step03 미션 — 직접 푼 것.
 *
 * <p>문제부터 풀어보고 싶으면 {@link Step03MissionTest} 를 먼저 연다.
 */
class Step03MissionSolvedTest {

    private static final int STOCK_MILLIS = 500;
    private static final int PRICE_MILLIS = 500;
    private static final int WAIT_LIMIT_MILLIS = 200;

    private static final int ORDER_COUNT = 100;
    private static final int ORDER_MILLIS = 10;

    @BeforeEach
    void resetTimeline() {
        Timeline.reset();
    }

    @Test
    @DisplayName("미션1 - join(timeout) 후 interrupt() 와 상태 복원으로 이후 작업까지 중단한다")
    void cancellingNeedsBothInterruptAndRestore() throws InterruptedException {
        List<String> results = new CopyOnWriteArrayList<>();

        long start = System.currentTimeMillis();

        Thread worker = new Thread(() -> {
            try {
                Thread.sleep(STOCK_MILLIS);
                results.add("재고-결과");
            } catch (InterruptedException _) {
                Thread.currentThread().interrupt();
            }

            try {
                Thread.sleep(PRICE_MILLIS);
                results.add("가격-결과");
            } catch (InterruptedException _) {
                Thread.currentThread().interrupt();
            }
        });

        worker.start();
        Sleeper.millis(WAIT_LIMIT_MILLIS);
        worker.interrupt();
        worker.join();

        long elapsed = System.currentTimeMillis() - start;

        System.out.println("받은 결과   : " + results);
        System.out.println("걸린 시간   : " + elapsed + "ms");
        System.out.println("스레드 상태 : " + worker.getState());

        assertThat(results)
                .as("재고 호출이 인터럽트되고 상태를 복원했으므로 가격 호출도 진입 즉시 중단되어야 한다")
                .isEmpty();

        assertThat(elapsed)
                .as("200ms 는 기다려야 하고, 재고 500ms 를 다 기다리면 안 된다")
                .isGreaterThanOrEqualTo(WAIT_LIMIT_MILLIS)
                .isLessThan(STOCK_MILLIS);

        assertThat(worker.getState())
                .as("인터럽트를 받고 스스로 정리한 뒤 종료되어야 한다")
                .isEqualTo(Thread.State.TERMINATED);
    }

    @Test
    @DisplayName("미션2 - CPU 연산은 인터럽트 상태를 직접 확인해야 중단된다")
    void cooperativeCancellationStopsTheLoop() throws InterruptedException {
        AtomicInteger processedCount = new AtomicInteger();

        long start = System.currentTimeMillis();

        Thread worker = new Thread(() -> {
            for (int i = 0; i < ORDER_COUNT; i++) {
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }

                burnCpu(ORDER_MILLIS);
                processedCount.incrementAndGet();
            }
        });

        worker.start();
        Sleeper.millis(WAIT_LIMIT_MILLIS);
        worker.interrupt();
        worker.join();

        long elapsed = System.currentTimeMillis() - start;

        System.out.println(ORDER_COUNT + "건 중 처리한 건수 : " + processedCount.get());
        System.out.println("걸린 시간                : " + elapsed + "ms");

        assertThat(processedCount.get())
                .as("인터럽트 전까지는 처리가 진행됐어야 한다")
                .isPositive()
                .as("인터럽트됐으므로 전체를 다 처리하면 안 된다")
                .isLessThan(ORDER_COUNT);

        assertThat(elapsed)
                .as("끝까지 실행했다면 1000ms 가 걸렸을 것이다")
                .isLessThan(ORDER_COUNT * ORDER_MILLIS);
    }

    /** 주어진 시간 동안 CPU를 쓰며 계산한다. 자지 않으므로 인터럽트로 깨어나지 않는다. */
    private void burnCpu(long millis) {
        long deadline = System.currentTimeMillis() + millis;
        long counter = 0;
        while (System.currentTimeMillis() < deadline) {
            counter++;
        }
        if (counter < 0) {
            throw new IllegalStateException("최적화로 루프가 사라지는 것을 막기 위한 코드");
        }
    }
}
