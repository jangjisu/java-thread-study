package study.thread.step03;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import study.thread.support.Timeline;

/**
 * step03 미션 — 문제 상태.
 *
 * <p>비어 있는 두 곳을 직접 채워서 빨간불을 초록불로 만든다.
 * 푼 것은 {@link Step03MissionSolvedTest} 에 있다.
 *
 * <p>{@code Thread.sleep} 을 직접 부르고 {@code try-catch} 도 직접 쓴다.
 * 그 예외를 어떻게 다루느냐가 이 단계의 주제다.
 */
class Step03MissionTest {

    private static final int STOCK_MILLIS = 500;
    private static final int PRICE_MILLIS = 500;
    private static final int WAIT_LIMIT_MILLIS = 200;

    private static final int ORDER_COUNT = 100;
    private static final int ORDER_MILLIS = 10;

    @BeforeEach
    void resetTimeline() {
        Timeline.reset();
    }

    /*
     * 미션 1. 제한 시간 안에 안 끝나면 인터럽트로 중단하고, 이후 작업까지 멈춘다.
     *
     * 실무 상황:
     *   상품 화면을 그리려면 재고 API 를 부르고, 이어서 가격 API 를 불러야 한다. 각각 500ms.
     *   그런데 화면은 200ms 안에 응답해야 한다.
     *   step02 에서는 join(200) 으로 기다리기만 포기했더니 스레드가 계속 실행되어
     *   결국 결과를 만들어냈다. 이번에는 실제로 중단시켜야 한다.
     *
     * 만들 것:
     *   - 스레드 안에서 두 API 를 차례로 부른다
     *       재고: Thread.sleep(STOCK_MILLIS) 후 results 에 "재고-결과" 를 담는다
     *       가격: Thread.sleep(PRICE_MILLIS) 후 results 에 "가격-결과" 를 담는다
     *       각각 InterruptedException 을 잡아야 한다
     *   - 재고 호출이 인터럽트되면 Thread.currentThread().interrupt() 로 인터럽트 상태를 복원한다
     *   - 바깥에서는 start() 하고 join(WAIT_LIMIT_MILLIS) 로 200ms 만 기다린다
     *   - 그래도 안 끝났으면(isAlive) interrupt() 를 호출한다
     *   - join() 으로 종료될 때까지 기다린다
     *
     * 다 만들면 확인해볼 것:
     *   인터럽트 상태를 복원하는 줄을 주석 처리하고 다시 실행해본다.
     *   재고 호출은 중단됐는데 가격 호출은 왜 끝까지 실행되는지 보인다.
     */
    @Test
    @DisplayName("미션1 - join(timeout) 후 interrupt() 와 상태 복원으로 이후 작업까지 중단한다")
    void cancellingNeedsBothInterruptAndRestore() throws InterruptedException {
        List<String> results = new CopyOnWriteArrayList<>();

        long start = System.currentTimeMillis();

        // 여기부터 직접 만든다
        Thread worker = null;

        long elapsed = System.currentTimeMillis() - start;

        System.out.println("받은 결과   : " + results);
        System.out.println("걸린 시간   : " + elapsed + "ms");
        System.out.println("스레드 상태 : " + (worker == null ? "아직 만들지 않음" : worker.getState()));

        assertThat(results)
                .as("재고 호출이 인터럽트되고 상태를 복원했으므로 가격 호출도 진입 즉시 중단되어야 한다")
                .isEmpty();

        assertThat(elapsed)
                .as("200ms 는 기다려야 하고, 재고 500ms 를 다 기다리면 안 된다")
                .isGreaterThanOrEqualTo(WAIT_LIMIT_MILLIS)
                .isLessThan(STOCK_MILLIS);

        assertThat(worker)
                .as("스레드를 만들어서 실행했어야 한다")
                .isNotNull();
        assertThat(worker.getState())
                .as("인터럽트를 받고 스스로 정리한 뒤 종료되어야 한다")
                .isEqualTo(Thread.State.TERMINATED);
    }

    /*
     * 미션 2. 오래 실행되는 반복 작업을 중간에 중단한다.
     *
     * 실무 상황:
     *   주문 100건을 한 건씩 처리하는 배치가 실행된다. 한 건에 10ms, 전체 1초다.
     *   중간에 중단 신호가 오면 남은 건은 처리하지 않고 빠져나와야 한다.
     *
     * 주의:
     *   이 작업은 자지 않고 계산만 한다.
     *   그래서 InterruptedException 은 발생하지 않는다. (ThreadTest03 의 1번)
     *   중단하려면 작업이 직접 인터럽트 상태를 확인해야 한다. (ThreadTest03 의 5번)
     *
     * 만들 것:
     *   - ORDER_COUNT 건을 처리하는 스레드를 만든다
     *       한 건 처리 = burnCpu(ORDER_MILLIS) 를 부르고 processedCount 를 1 늘린다
     *       한 건을 처리하기 전에 인터럽트 상태를 확인하고, 설정돼 있으면 반환한다
     *   - start() 하고 WAIT_LIMIT_MILLIS 뒤에 interrupt() 를 호출한다
     *   - join() 으로 기다린다
     *
     * 다 만들면 확인해볼 것:
     *   인터럽트 상태를 확인하는 코드를 지우고 다시 실행해본다.
     */
    @Test
    @DisplayName("미션2 - CPU 연산은 인터럽트 상태를 직접 확인해야 중단된다")
    void cooperativeCancellationStopsTheLoop() throws InterruptedException {
        AtomicInteger processedCount = new AtomicInteger();

        long start = System.currentTimeMillis();

        // 여기부터 직접 만든다

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
