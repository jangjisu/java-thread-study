package study.thread.step01;

import static org.assertj.core.api.Assertions.assertThat;
import static study.thread.support.SlowApi.CALL_MILLIS;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import study.thread.support.SlowApi;
import study.thread.support.Timeline;

/**
 * step01 미션 — 문제 상태.
 *
 * <p>비어 있는 두 곳을 직접 채워서 빨간불을 초록불로 만든다.
 * 푼 것은 {@link Step01MissionSolvedTest} 에 있다.
 *
 * <p>같은 스레드 3개를 run() 으로 부를 때와 start() 로 부를 때가 어떻게 다른지 확인한다.
 */
class Step01MissionTest {

    @BeforeEach
    void resetTimeline() {
        Timeline.reset();
    }

    /*
     * 미션 1. threads 를 for 문으로 돌면서 run() 을 부른다.
     *
     * 다 만들면 알게 되는 것:
     *   Thread 를 3개나 만들었는데도 스레드는 하나도 시작되지 않는다.
     *   타임라인의 "호출 시작" 세 줄이 500ms 씩 밀려서 찍힌다.
     */
    @Test
    @DisplayName("미션1 - run() 으로 부르면 스레드가 하나도 시작되지 않아 순차 실행된다")
    void callSequentiallyWithRun() {
        Thread[] threads = createApiThreads();

        long start = System.currentTimeMillis();

        // 여기부터 직접 만든다

        long elapsed = System.currentTimeMillis() - start;

        System.out.println("run() " + threads.length + "번 → " + elapsed + "ms");
        printStates(threads);

        assertThat(elapsed)
                .as("run() 은 순차 실행이라 500ms 씩 더해져야 한다")
                .isGreaterThanOrEqualTo((long) CALL_MILLIS * threads.length);
    }

    /*
     * 미션 2. threads 를 for 문으로 돌면서 start() 를 부르고, 그 다음 for 문으로 join() 을 부른다.
     *
     * 막히면 떠올릴 것:
     *   start() 와 join() 을 한 for 문 안에서 짝지어 부르면 어떻게 될까? 직접 해보면 안다.
     */
    @Test
    @DisplayName("미션2 - start() 로 부르면 셋이 동시에 실행돼 500ms 로 끝난다")
    void callConcurrentlyWithStart() throws InterruptedException {
        Thread[] threads = createApiThreads();

        long start = System.currentTimeMillis();

        // 여기부터 직접 만든다

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
