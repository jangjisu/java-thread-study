package study.thread.step01_basics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import study.thread.support.Sleeper;
import study.thread.support.Timeline;

/**
 * step01 — 스레드를 만드는 방법과, {@code start()}가 정확히 무엇을 하는가.
 *
 * <p>확인하려는 것: 새 스레드에서 실행됐다는 사실을 어떻게 "증명"할 것인가.
 * 눈으로 보는 대신, 실제로 실행한 스레드의 이름을 붙잡아 단언한다.
 */
class ThreadCreationTest {

    @BeforeEach
    void resetTimeline() {
        Timeline.reset();
    }

    @Test
    @DisplayName("Thread를 상속하면 run()을 재정의해 할 일을 적는다")
    void extendThread() throws InterruptedException {
        AtomicReference<String> ranOn = new AtomicReference<>();

        Thread thread = new Thread("extends-thread") {
            @Override
            public void run() {
                Timeline.log("Thread를 상속한 run() 실행");
                ranOn.set(Thread.currentThread().getName());
            }
        };

        thread.start();
        thread.join();

        // 내가 만든 그 스레드가 실행했다 — 호출한 쪽(main)이 아니라.
        assertThat(ranOn.get()).isEqualTo("extends-thread");
    }

    @Test
    @DisplayName("Runnable로 '할 일'을 분리하면 스레드와 작업이 따로 논다")
    void implementRunnable() throws InterruptedException {
        AtomicReference<String> ranOn = new AtomicReference<>();

        // 작업(Runnable)은 '무엇을 할지'만 안다. '누가 실행할지'는 모른다.
        Runnable task = () -> {
            Timeline.log("Runnable 실행");
            ranOn.set(Thread.currentThread().getName());
        };

        Thread thread = new Thread(task, "runnable-thread");
        thread.start();
        thread.join();

        assertThat(ranOn.get()).isEqualTo("runnable-thread");

        // 같은 작업을 다른 스레드가 실행할 수도, 심지어 그냥 호출할 수도 있다.
        // 작업과 실행 주체가 분리됐기 때문이다. step05의 스레드 풀이 여기서 출발한다.
        task.run();
        assertThat(ranOn.get()).isEqualTo(Thread.currentThread().getName());
    }

    @Test
    @DisplayName("run()은 그냥 메서드 호출이다 — 스레드는 시작조차 하지 않는다")
    void runDoesNotStartAThread() {
        AtomicReference<String> ranOn = new AtomicReference<>();
        Thread thread = new Thread(() -> ranOn.set(Thread.currentThread().getName()), "never-started");

        thread.run(); // start()가 아니다

        // 실행한 것은 나(main)다. 새 스레드가 아니다.
        assertThat(ranOn.get()).isEqualTo(Thread.currentThread().getName());
        // 그리고 이 스레드는 아직 태어나지도 않은 상태다.
        assertThat(thread.getState()).isEqualTo(Thread.State.NEW);
    }

    @Test
    @DisplayName("start()는 한 번만 호출할 수 있다")
    void startCanBeCalledOnlyOnce() throws InterruptedException {
        Thread thread = new Thread(() -> Timeline.log("한 번 실행"), "once");
        thread.start();
        thread.join();

        // 끝난 스레드는 되살릴 수 없다. 스레드는 재사용 대상이 아니다.
        // "그럼 매번 새로 만들어야 하나?"에 대한 답이 step05의 스레드 풀이다.
        assertThat(thread.getState()).isEqualTo(Thread.State.TERMINATED);
        assertThatThrownBy(thread::start).isInstanceOf(IllegalThreadStateException.class);
    }

    @Test
    @DisplayName("join()은 그 스레드가 끝날 때까지 기다린다")
    void joinWaitsForCompletion() throws InterruptedException {
        List<String> done = new CopyOnWriteArrayList<>();

        Thread worker = new Thread(() -> {
            Timeline.log("작업 시작");
            Sleeper.millis(100);
            done.add("worker");
            Timeline.log("작업 끝");
        }, "worker");

        worker.start();
        Timeline.log("worker를 기다린다");
        worker.join();
        Timeline.log("worker가 끝난 것을 확인했다");

        // join()이 돌아왔다는 것은 worker가 확실히 끝났다는 뜻이다.
        assertThat(done).containsExactly("worker");
        assertThat(worker.getState()).isEqualTo(Thread.State.TERMINATED);
    }

    @Test
    @DisplayName("데몬 스레드는 JVM이 끝나기를 붙잡지 않는다")
    void daemonThread() {
        Thread daemon = new Thread(() -> Sleeper.millis(10_000), "daemon");
        daemon.setDaemon(true); // start() 전에만 설정할 수 있다

        daemon.start();

        assertThat(daemon.isDaemon()).isTrue();
        // 일반 스레드였다면 JVM은 이 스레드가 끝날 때까지(10초) 종료되지 못한다.
        // 데몬이면 남은 스레드가 데몬뿐일 때 JVM이 그냥 종료된다.
    }

    @Test
    @DisplayName("[관찰] 여러 스레드의 실행 순서는 보장되지 않는다")
    @Tag("observation")
    void executionOrderIsNotGuaranteed() {
        List<String> finishOrder = new CopyOnWriteArrayList<>();

        List<Thread> threads = IntStream.rangeClosed(1, 5)
                .mapToObj(i -> new Thread(() -> {
                    Timeline.log("실행");
                    finishOrder.add("t" + i);
                }, "t" + i))
                .toList();

        threads.forEach(Thread::start);
        threads.forEach(ThreadCreationTest::joinQuietly);

        // 단언하지 않는다. 순서가 뒤바뀌는 것이 정상이고, 뒤바뀌지 않는 것도 정상이다.
        // 여기에 assert를 걸면 그 테스트는 거짓말쟁이가 된다.
        Timeline.log("시작 순서: [t1, t2, t3, t4, t5]");
        Timeline.log("끝난 순서: " + finishOrder);
    }

    private static void joinQuietly(Thread thread) {
        try {
            thread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("기다리는 중 인터럽트됨", e);
        }
    }
}
