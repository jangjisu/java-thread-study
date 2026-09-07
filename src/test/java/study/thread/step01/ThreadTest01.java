package study.thread.step01;

import static org.assertj.core.api.Assertions.assertThat;
import static study.thread.support.SlowApi.CALL_MILLIS;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import study.thread.support.Sleeper;
import study.thread.support.SlowApi;

/** step01 정리 — 테스트를 위에서부터 읽으면 Thread 사용법이 순서대로 나온다. */
class ThreadTest01 {

    @Test
    @DisplayName("1. 지금 나를 실행하는 것은 플랫폼 스레드다 - 가상 스레드도 데몬도 아니다")
    void currentThreadIsPlatformThread() {
        Thread current = Thread.currentThread();

        System.out.println("이름        : " + current.getName());
        System.out.println("id          : " + current.threadId() + "   (생성 시 자동 부여, 변경 불가)");
        System.out.println("상태        : " + current.getState());
        System.out.println("가상 스레드 : " + current.isVirtual() + "   (false = 플랫폼 스레드, OS 커널 스레드와 1:1)");
        System.out.println("데몬        : " + current.isDaemon() + "   (false = 이 스레드가 살아 있으면 JVM은 종료되지 않는다)");

        assertThat(current.isVirtual()).isFalse();
        assertThat(current.isDaemon()).isFalse();
        assertThat(current.getState()).isEqualTo(Thread.State.RUNNABLE);
    }

    @Test
    @DisplayName("2. 외부 API 응답을 기다리며 자는 중에는 TIMED_WAITING 이다")
    void sleepingThreadIsTimedWaiting() throws InterruptedException {
        Thread sleeping = new Thread(() -> SlowApi.call("결제"), "sleeping");

        Thread.State beforeStart = sleeping.getState();

        sleeping.start();
        Sleeper.millis(100);
        Thread.State whileRunning = sleeping.getState();

        sleeping.join();
        Thread.State afterJoin = sleeping.getState();

        // 출력은 관찰이 끝난 뒤에 몰아서 한다.
        // System.out.println 은 내부가 synchronized 라, 관찰 도중에 찍으면
        // 스레드끼리 락을 기다리느라 상태가 BLOCKED 로 보인다.
        System.out.println("자는 스레드 (Thread.sleep 으로 대기 중)");
        System.out.println("  start() 전 : " + beforeStart);
        System.out.println("  실행 중    : " + whileRunning + "   (시간 제한을 두고 기다리는 중)");
        System.out.println("  join() 후  : " + afterJoin);

        assertThat(beforeStart).isEqualTo(Thread.State.NEW);
        assertThat(whileRunning).isEqualTo(Thread.State.TIMED_WAITING);
        assertThat(afterJoin).isEqualTo(Thread.State.TERMINATED);
    }

    @Test
    @DisplayName("3. CPU를 쓰며 계산하는 중에는 RUNNABLE 이다")
    void busyThreadIsRunnable() throws InterruptedException {
        Thread busy = new Thread(this::burnCpu, "busy");

        Thread.State beforeStart = busy.getState();

        busy.start();
        Sleeper.millis(100);
        Thread.State whileRunning = busy.getState();

        busy.join();
        Thread.State afterJoin = busy.getState();

        System.out.println("바쁜 스레드 (CPU 계산 중)");
        System.out.println("  start() 전 : " + beforeStart);
        System.out.println("  실행 중    : " + whileRunning + "   (실행 중이거나 CPU를 기다리는 중, 둘을 구분하지 않는다)");
        System.out.println("  join() 후  : " + afterJoin);

        assertThat(beforeStart).isEqualTo(Thread.State.NEW);
        assertThat(whileRunning).isEqualTo(Thread.State.RUNNABLE);
        assertThat(afterJoin).isEqualTo(Thread.State.TERMINATED);
    }

    @Test
    @DisplayName("4. run() 을 부르면 새 스레드가 아니라 부른 쪽에서 실행된다")
    void runExecutesOnCallerThread() {
        AtomicReference<String> executedBy = new AtomicReference<>();
        Thread worker = new Thread(() -> executedBy.set(Thread.currentThread().getName()), "worker");

        worker.run();

        System.out.println("worker 라고 이름 붙였지만 실제 실행한 스레드 : " + executedBy.get());
        System.out.println("지금 이 테스트를 실행 중인 스레드            : " + Thread.currentThread().getName());
        System.out.println("worker 의 상태                              : " + worker.getState() + "   (시작조차 하지 않았다)");

        assertThat(executedBy.get()).isEqualTo(Thread.currentThread().getName());
        assertThat(worker.getState()).isEqualTo(Thread.State.NEW);
    }

    @Test
    @DisplayName("5. run() 3번 - 스레드를 3개 만들어도 하나도 시작되지 않아 순차 실행된다")
    void runIsSequential() {
        Thread[] threads = createApiThreads();

        long start = System.currentTimeMillis();
        for (Thread thread : threads) {
            thread.run();
        }
        long elapsed = System.currentTimeMillis() - start;

        System.out.println("run() " + threads.length + "번 → " + elapsed + "ms");
        printStates(threads);

        assertThat(elapsed).isGreaterThanOrEqualTo((long) CALL_MILLIS * threads.length);
        for (Thread thread : threads) {
            assertThat(thread.getState()).isEqualTo(Thread.State.NEW);
        }
    }

    @Test
    @DisplayName("6. start() 3번 - start() 를 먼저 다 부르고 join() 을 모아서 부르면 동시에 실행된다")
    void startIsConcurrent() throws InterruptedException {
        Thread[] threads = createApiThreads();

        long start = System.currentTimeMillis();
        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        long elapsed = System.currentTimeMillis() - start;

        long apiCount = threads.length;

        System.out.println("start() " + apiCount + "번 → " + elapsed + "ms");
        printStates(threads);

        assertThat(elapsed).isLessThan(CALL_MILLIS * apiCount);
        for (Thread thread : threads) {
            assertThat(thread.getState()).isEqualTo(Thread.State.TERMINATED);
        }
    }

    private Thread[] createApiThreads() {
        Thread[] threads = new Thread[3];
        String[] apis = {"결제", "배송", "재고"};

        for (int i = 0; i < apis.length; i++) {
            String apiName = apis[i];
            threads[i] = new Thread(() -> SlowApi.call(apiName), apiName + "-thread");
        }
        return threads;
    }

    private void printStates(Thread[] threads) {
        for (Thread thread : threads) {
            System.out.println("  " + thread.getName() + " 상태: " + thread.getState());
        }
    }

    /** CPU를 쓰며 계산하는 작업. 자는 것과 달리 RUNNABLE 상태가 된다. */
    private void burnCpu() {
        long deadline = System.currentTimeMillis() + CALL_MILLIS;
        long counter = 0;
        while (System.currentTimeMillis() < deadline) {
            counter++;
        }
        if (counter < 0) {
            throw new IllegalStateException("최적화로 루프가 사라지는 것을 막기 위한 코드");
        }
    }
}
