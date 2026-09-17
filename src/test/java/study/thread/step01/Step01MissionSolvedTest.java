package study.thread.step01;

import static org.assertj.core.api.Assertions.assertThat;
import static study.thread.support.SlowApi.CALL_MILLIS;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import study.thread.support.SlowApi;
import study.thread.support.Timeline;

/**
 * step01 미션 — 직접 푼 것.
 *
 * <p>문제부터 풀어보고 싶으면 {@link Step01MissionTest} 를 먼저 연다.
 */
class Step01MissionSolvedTest {

    @BeforeEach
    void resetTimeline() {
        Timeline.reset();
    }

    @Test
    @DisplayName("미션1 - run() 을 3번 호출하면 스레드가 시작되지 않고 순차 실행된다")
    void callSequentiallyWithRun() {
        Thread[] threads = createApiThreads();

        long start = System.currentTimeMillis();

        for (Thread thread : threads) {
            thread.run();
        }

        long elapsed = System.currentTimeMillis() - start;

        System.out.println("run() " + threads.length + "번 → " + elapsed + "ms");
        printStates(threads);

        assertThat(elapsed)
                .as("run() 은 순차 실행이라 500ms 씩 더해져야 한다")
                .isGreaterThanOrEqualTo((long) CALL_MILLIS * threads.length);
    }

    /*
     * 알게 된 것:
     *   start() 만 하고 join() 으로 기다리지 않으면 그 스레드가 끝났는지조차 알 수 없다.
     *   start() 셋을 먼저 부른 뒤 join() 을 모아서 부르면 500ms 안에 끝나지만,
     *   한 for 문 안에서 start(); join(); 을 짝지어 부르면 앞의 것이 끝난 뒤에
     *   다음 요청이 들어가서 결국 1500ms 가 걸린다.
     */
    @Test
    @DisplayName("미션2 - start() 3번 후 join() 3번을 호출하면 동시 실행된다")
    void callConcurrentlyWithStart() throws InterruptedException {
        Thread[] threads = createApiThreads();

        long start = System.currentTimeMillis();

        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }

        long elapsed = System.currentTimeMillis() - start;

        System.out.println("start() " + threads.length + "번 → " + elapsed + "ms");
        printStates(threads);

        assertThat(elapsed)
                .as("호출을 진짜로 했다면 최소 한 번 분량인 500ms 는 걸려야 한다")
                .isGreaterThanOrEqualTo(CALL_MILLIS)
                .as("동시에 호출했다면 1500ms 보다는 훨씬 적게 걸려야 한다")
                .isLessThan((long) CALL_MILLIS * threads.length);
    }

    private Thread[] createApiThreads() {
        String[] apis = {"결제", "배송", "재고"};
        Thread[] threads = new Thread[apis.length];

        for (int i = 0; i < apis.length; i++) {
            String apiName = apis[i];
            threads[i] = new Thread(() -> SlowApi.callWithLog(apiName), apiName + "-thread");
        }
        return threads;
    }

    private void printStates(Thread[] threads) {
        for (Thread thread : threads) {
            System.out.println("  " + thread.getName() + " 상태: " + thread.getState());
        }
    }
}
