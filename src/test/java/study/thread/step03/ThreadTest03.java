package study.thread.step03;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import study.thread.support.Sleeper;

/**
 * step03 정리 — 실행 중인 스레드를 어떻게 멈추나.
 *
 * <p>step02 에서 {@code join(timeout)} 은 기다림만 포기할 뿐 스레드를 멈추지 못한다는 것을 봤다.
 * 그럼 진짜로 멈추게 하려면 어떻게 해야 하는가.
 *
 * <p>이 파일에서는 {@code Thread.sleep} 을 직접 부르고 {@code try-catch} 도 직접 쓴다.
 * 그 예외를 어떻게 다루느냐가 이 단계의 주제이기 때문이다.
 */
class ThreadTest03 {

    private static final int WORK_MILLIS = 500;
    private static final int INTERRUPT_AFTER_MILLIS = 100;

    @Test
    @DisplayName("1. interrupt() 는 인터럽트 상태만 설정한다")
    void interruptOnlyMarksTheThread() throws InterruptedException {
        AtomicBoolean finishedAllWork = new AtomicBoolean(false);
        AtomicBoolean markedAtEnd = new AtomicBoolean(false);

        // 자지 않고 계속 계산만 하는 작업. 인터럽트를 확인하지도 않는다.
        Thread worker = new Thread(() -> {
            burnCpu(WORK_MILLIS);
            finishedAllWork.set(true);
            markedAtEnd.set(Thread.currentThread().isInterrupted());
        }, "worker");

        worker.start();
        Sleeper.millis(INTERRUPT_AFTER_MILLIS);
        worker.interrupt(); // 멈추라고 요청한다
        worker.join();

        System.out.println("일을 끝까지 다 했나  : " + finishedAllWork.get());
        System.out.println("인터럽트 상태        : " + markedAtEnd.get());

        // 인터럽트를 요청했지만 스레드는 작업을 끝까지 수행했다.
        // interrupt() 는 실행을 중단시키지 않고 인터럽트 상태만 설정한다.
        assertThat(finishedAllWork.get()).isTrue();
        assertThat(markedAtEnd.get()).isTrue();
    }

    @Test
    @DisplayName("2. sleep() 중 인터럽트되면 InterruptedException 이 발생한다")
    void sleepingThreadWakesUpWithException() throws InterruptedException {
        AtomicBoolean caughtException = new AtomicBoolean(false);
        AtomicBoolean finishedSleeping = new AtomicBoolean(false);

        Thread worker = new Thread(() -> {
            try {
                Thread.sleep(WORK_MILLIS);
                finishedSleeping.set(true);
            } catch (InterruptedException e) {
                caughtException.set(true);
            }
        }, "worker");

        long start = System.currentTimeMillis();
        worker.start();
        Sleeper.millis(INTERRUPT_AFTER_MILLIS);
        worker.interrupt();
        worker.join();
        long elapsed = System.currentTimeMillis() - start;

        System.out.println("예외를 받았나      : " + caughtException.get());
        System.out.println("500ms 를 다 잤나   : " + finishedSleeping.get());
        System.out.println("실제 걸린 시간     : " + elapsed + "ms");

        // 500ms 를 자려 했지만 100ms 만에 깨어났다.
        // 기다리는 메서드(sleep, join, wait)만이 인터럽트에 이렇게 즉시 반응한다.
        assertThat(caughtException.get()).isTrue();
        assertThat(finishedSleeping.get()).isFalse();
        assertThat(elapsed).isLessThan(WORK_MILLIS);
    }

    @Test
    @DisplayName("3. InterruptedException 이 발생하면 인터럽트 상태는 해제된다")
    void exceptionClearsTheMark() throws InterruptedException {
        AtomicBoolean markedInsideCatch = new AtomicBoolean(true);

        Thread worker = new Thread(() -> {
            try {
                Thread.sleep(WORK_MILLIS);
            } catch (InterruptedException _) {
                // 인터럽트되어 진입한 catch 블록이지만 인터럽트 상태는 이미 해제되어 있다.
                markedInsideCatch.set(Thread.currentThread().isInterrupted());
            }
        }, "worker");

        worker.start();
        Sleeper.millis(INTERRUPT_AFTER_MILLIS);
        worker.interrupt();
        worker.join();

        System.out.println("catch 블록에서 읽은 인터럽트 상태 : " + markedInsideCatch.get());

        // JDK 문서 그대로다.
        // "sleep/join/wait 로 블록된 스레드를 인터럽트하면
        //  인터럽트 상태가 지워지고(cleared) InterruptedException 을 받는다"
        //
        // 따라서 예외를 잡은 쪽이 인터럽트 상태를 복원하지 않으면
        // 호출자는 이 스레드가 인터럽트됐다는 사실을 알 수 없다.
        assertThat(markedInsideCatch.get()).isFalse();
    }

    @Test
    @DisplayName("4. InterruptedException 을 무시하면 이후 작업이 그대로 실행된다")
    void swallowingTheExceptionIgnoresCancellation() throws InterruptedException {
        List<String> doneSteps = new CopyOnWriteArrayList<>();

        Thread worker = new Thread(() -> {
            try {
                Thread.sleep(WORK_MILLIS);
                doneSteps.add("1단계");
            } catch (InterruptedException e) {
                // 인터럽트 상태를 복원하지 않고 그대로 넘어간다.
            }

            // 인터럽트 상태가 해제된 뒤이므로 이 sleep() 은 정상적으로 대기한다.
            try {
                Thread.sleep(WORK_MILLIS);
                doneSteps.add("2단계");
            } catch (InterruptedException e) {
                doneSteps.add("2단계-취소됨");
            }
        }, "worker");

        long start = System.currentTimeMillis();
        worker.start();

        worker.join(INTERRUPT_AFTER_MILLIS); // 100ms 만 기다려보고
        if (worker.isAlive()) {
            worker.interrupt(); // 아직이면 취소를 요청한다
        }
        worker.join(); // 정리될 때까지 기다린다

        long elapsed = System.currentTimeMillis() - start;

        System.out.println("끝난 단계  : " + doneSteps);
        System.out.println("걸린 시간  : " + elapsed + "ms   (2단계를 끝까지 다 잤다)");

        // 1단계는 취소됐지만 2단계는 멀쩡히 끝났다. 취소 요청이 증발한 것이다.
        // 빈 catch 블록이 위험한 이유가 이것이다. 6번에서 한 줄만 고쳐 해결한다.
        assertThat(doneSteps).containsExactly("2단계");
        assertThat(elapsed).isGreaterThanOrEqualTo(WORK_MILLIS);
    }

    @Test
    @DisplayName("5. CPU 연산은 인터럽트 상태를 직접 확인해야 중단된다")
    void cancellationIsCooperative() throws InterruptedException {
        AtomicInteger processedCount = new AtomicInteger();

        // 100건을 처리하는 작업. 한 건 처리할 때마다 취소됐는지 확인한다.
        Thread worker = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                if (Thread.currentThread().isInterrupted()) {
                    return; // 확인했으니 스스로 멈춘다
                }
                burnCpu(10);
                processedCount.incrementAndGet();
            }
        }, "worker");

        long start = System.currentTimeMillis();
        worker.start();
        Sleeper.millis(INTERRUPT_AFTER_MILLIS);
        worker.interrupt();
        worker.join();
        long elapsed = System.currentTimeMillis() - start;

        System.out.println("100건 중 처리한 건수 : " + processedCount.get());
        System.out.println("걸린 시간            : " + elapsed + "ms   (끝까지 갔다면 1000ms)");

        // interrupt() 가 실행을 중단시킨 것이 아니라,
        // 작업이 인터럽트 상태를 확인하고 스스로 반환한 것이다.
        // 확인하는 코드가 없으면 1번 테스트처럼 끝까지 실행된다.
        assertThat(processedCount.get()).isLessThan(100);
        assertThat(elapsed).isLessThan(100 * 10);
    }

    @Test
    @DisplayName("6. catch 에서 인터럽트 상태를 복원하면 이후 작업도 중단된다")
    void restoringTheMarkPropagatesCancellation() throws InterruptedException {
        List<String> doneSteps = new CopyOnWriteArrayList<>();

        Thread worker = new Thread(() -> {
            try {
                Thread.sleep(WORK_MILLIS);
                doneSteps.add("1단계");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // ★ 4번과 다른 단 한 줄
            }

            // 인터럽트 상태가 유지되므로 이 sleep() 은 대기하지 않고 즉시 예외로 끝난다.
            try {
                Thread.sleep(WORK_MILLIS);
                doneSteps.add("2단계");
            } catch (InterruptedException e) {
                doneSteps.add("2단계-취소됨");
            }
        }, "worker");

        long start = System.currentTimeMillis();
        worker.start();

        worker.join(INTERRUPT_AFTER_MILLIS); // 100ms 만 기다려보고
        if (worker.isAlive()) {
            worker.interrupt(); // 아직이면 취소를 요청한다
        }
        worker.join(); // 정리될 때까지 기다린다

        long elapsed = System.currentTimeMillis() - start;

        System.out.println("끝난 단계  : " + doneSteps);
        System.out.println("걸린 시간  : " + elapsed + "ms   (4번은 600ms 가 걸렸다)");

        //여기서는 2단계도 즉시 끊겼다.
        // 인터럽트를 직접 처리하지 못하는 위치라면 최소한 상태를 복원해야 하는 이유다.
        assertThat(doneSteps).containsExactly("2단계-취소됨");
        assertThat(elapsed).isLessThan(WORK_MILLIS);
    }

    @Test
    @DisplayName("7. CPU 연산 완료 후 sleep() 에 진입하면 InterruptedException 이 발생한다")
    void interruptDuringCpuWorkThrowsAtTheNextSleep() throws InterruptedException {
        AtomicBoolean caughtException = new AtomicBoolean(false);
        AtomicLong cpuFinishedAt = new AtomicLong();
        AtomicLong sleepEndedAt = new AtomicLong();

        long start = System.currentTimeMillis();

        Thread worker = new Thread(() -> {
            // CPU 연산 중에는 인터럽트가 와도 중단되지 않는다. 인터럽트 상태만 설정된다.
            burnCpu(WORK_MILLIS);
            cpuFinishedAt.set(System.currentTimeMillis() - start);

            // 인터럽트 상태가 남아 있으므로 sleep() 은 대기하지 않고
            // 즉시 InterruptedException 을 던진다.
            try {
                Thread.sleep(WORK_MILLIS);
            } catch (InterruptedException e) {
                caughtException.set(true);
            }
            sleepEndedAt.set(System.currentTimeMillis() - start);
        }, "worker");

        worker.start();
        Sleeper.millis(INTERRUPT_AFTER_MILLIS); // 100ms 시점, 아직 계산 중이다
        worker.interrupt();
        worker.join();

        System.out.println("계산이 끝난 시각   : " + cpuFinishedAt.get() + "ms   (100ms 에 인터럽트했는데 끝까지 갔다)");
        System.out.println("sleep 이 끝난 시각 : " + sleepEndedAt.get() + "ms   (500ms 를 자려 했다)");
        System.out.println("예외를 받았나      : " + caughtException.get());

        // 인터럽트는 계산을 끊지 못했다. 계산은 500ms 를 다 채웠다.
        assertThat(cpuFinishedAt.get()).isGreaterThanOrEqualTo(WORK_MILLIS);

        // 그러나 인터럽트 상태는 유지되었고,
        // 그 결과 이어지는 sleep() 이 진입 즉시 예외로 끝났다.
        assertThat(caughtException.get()).isTrue();
        assertThat(sleepEndedAt.get() - cpuFinishedAt.get()).isLessThan(INTERRUPT_AFTER_MILLIS);

        // 인터럽트 상태는 해제되기 전까지 유지된다.
        // CPU 연산 중에는 아무 영향이 없다가, sleep/join/wait 에 진입하는 시점에
        // InterruptedException 으로 나타난다.
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
