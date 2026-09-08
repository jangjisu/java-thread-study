package study.thread.step02;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import study.thread.support.SlowApi;

/** step02 정리 — 여러 스레드를 돌리면 순서는 어떻게 되고, 어떻게 기다리는가. */
class ThreadTest02 {

    private static final int FAST_MILLIS = 100;
    private static final int NORMAL_MILLIS = 300;
    private static final int SLOW_MILLIS = 500;

    @Test
    @DisplayName("1. start() 를 부른 순서와 실행되는 순서는 다르다")
    void startOrderIsNotExecutionOrder() throws InterruptedException {
        List<String> executionOrder = new CopyOnWriteArrayList<>();

        Thread[] threads = new Thread[5];
        for (int i = 0; i < threads.length; i++) {
            String name = "t" + (i + 1);
            threads[i] = new Thread(() -> executionOrder.add(name), name);
        }

        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }

        System.out.println("start() 부른 순서 : [t1, t2, t3, t4, t5]");
        System.out.println("실제 실행된 순서  : " + executionOrder);

        // 순서에는 단언을 걸지 않는다.
        // 뒤바뀌는 것도 정상이고 안 뒤바뀌는 것도 정상이라, 어느 쪽을 단언해도 언젠가 거짓이 된다.
        // 보장되는 것은 "다섯 개가 전부 실행됐다" 뿐이다.
        assertThat(executionOrder).containsExactlyInAnyOrder("t1", "t2", "t3", "t4", "t5");
    }

    @Test
    @DisplayName("2. 완료 순서는 시작 순서가 아니라 걸린 시간이 정한다")
    void completionOrderFollowsDuration() throws InterruptedException {
        List<String> completionOrder = new CopyOnWriteArrayList<>();

        // 오래 걸리는 것부터 시작시킨다.
        Thread slow = new Thread(() -> {
            SlowApi.call("느림", SLOW_MILLIS);
            completionOrder.add("느림");
        }, "slow");
        Thread normal = new Thread(() -> {
            SlowApi.call("보통", NORMAL_MILLIS);
            completionOrder.add("보통");
        }, "normal");
        Thread fast = new Thread(() -> {
            SlowApi.call("빠름", FAST_MILLIS);
            completionOrder.add("빠름");
        }, "fast");

        slow.start();
        normal.start();
        fast.start();

        slow.join();
        normal.join();
        fast.join();

        System.out.println("start() 부른 순서 : [느림, 보통, 빠름]");
        System.out.println("완료된 순서       : " + completionOrder);

        // 시작 순서와 정확히 반대다. 먼저 시켰다고 먼저 끝나지 않는다.
        assertThat(completionOrder).containsExactly("빠름", "보통", "느림");
    }

    @Test
    @DisplayName("3. 결과를 모으면 완료 순서대로 담긴다 - 그래서 순서에 기대는 코드는 깨진다")
    void resultsAreCollectedInCompletionOrder() throws InterruptedException {
        List<String> results = new CopyOnWriteArrayList<>();

        // 여러 스레드가 동시에 담으므로 평범한 ArrayList 대신 CopyOnWriteArrayList 를 쓴다.
        // 평범한 리스트를 쓰면 왜 위험한지는 step03 에서 직접 깨뜨려 본다.
        Thread[] threads = {
                new Thread(() -> results.add(SlowApi.call("느림", SLOW_MILLIS))),
                new Thread(() -> results.add(SlowApi.call("보통", NORMAL_MILLIS))),
                new Thread(() -> results.add(SlowApi.call("빠름", FAST_MILLIS)))
        };

        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }

        System.out.println("요청한 순서 : [느림, 보통, 빠름]");
        System.out.println("담긴 순서   : " + results);

        // 요청한 순서로 담기지 않는다. results.get(0) 이 "느림-결과" 일 것이라 기대하면 틀린다.
        assertThat(results).containsExactly("빠름-결과", "보통-결과", "느림-결과");
        assertThat(results.get(0)).isNotEqualTo("느림-결과");
    }

    @Test
    @DisplayName("4. join() 을 부르는 순서는 전체 시간에 영향을 주지 않는다")
    void joinOrderDoesNotChangeTotalTime() throws InterruptedException {
        Thread slow = new Thread(() -> SlowApi.call("느림", SLOW_MILLIS), "slow");
        Thread fast = new Thread(() -> SlowApi.call("빠름", FAST_MILLIS), "fast");

        long start = System.currentTimeMillis();

        slow.start();
        fast.start();

        // 빠른 것을 먼저 기다리든 늦은 것을 먼저 기다리든 결과는 같다.
        // join() 은 "기다리는 순서"를 정할 뿐, 스레드의 실행에는 관여하지 않기 때문이다.
        fast.join();
        long afterFastJoin = System.currentTimeMillis() - start;

        slow.join();
        long afterSlowJoin = System.currentTimeMillis() - start;

        System.out.println("fast.join() 이 돌아온 시점 : " + afterFastJoin + "ms");
        System.out.println("slow.join() 이 돌아온 시점 : " + afterSlowJoin + "ms");

        // 전체 시간은 가장 오래 걸리는 작업이 정한다.
        assertThat(afterSlowJoin).isGreaterThanOrEqualTo(SLOW_MILLIS);
        assertThat(afterSlowJoin).isLessThan(SLOW_MILLIS + NORMAL_MILLIS);
    }

    @Test
    @DisplayName("5. join(timeout) 은 기다림만 포기한다 - 스레드는 계속 일해서 결국 결과를 만든다")
    void joinWithTimeoutDoesNotStopTheThread() throws InterruptedException {
        List<String> results = new CopyOnWriteArrayList<>();

        Thread slow = new Thread(() -> results.add(SlowApi.call("느림", SLOW_MILLIS)), "slow");
        slow.start();

        long start = System.currentTimeMillis();
        slow.join(FAST_MILLIS); // 500ms 짜리 작업을 100ms 만 기다린다
        long waited = System.currentTimeMillis() - start;

        // 기다리기를 포기한 시점의 상황을 그대로 찍어 둔다.
        List<String> resultsAtTimeout = List.copyOf(results);
        Thread.State stateAtTimeout = slow.getState();

        slow.join(); // 이번에는 끝까지 기다린다
        List<String> resultsAfterFullJoin = List.copyOf(results);

        System.out.println("기다린 시간          : " + waited + "ms");
        System.out.println("포기한 시점의 결과   : " + resultsAtTimeout + "   상태: " + stateAtTimeout);
        System.out.println("끝까지 기다린 뒤     : " + resultsAfterFullJoin + "   상태: " + slow.getState());

        // 100ms 만 기다리고 돌아왔다. 500ms 를 다 기다리지 않았다.
        assertThat(waited).isLessThan(SLOW_MILLIS);

        // 그 시점에는 아직 결과가 없다. 작업이 안 끝났으니 당연하다.
        assertThat(resultsAtTimeout).isEmpty();
        assertThat(stateAtTimeout).isEqualTo(Thread.State.TIMED_WAITING);

        // 그런데 끝까지 기다려 보니 결과가 들어와 있다.
        // 내가 기다리기를 포기했을 뿐, 스레드는 자기 일을 계속하고 있었다는 뜻이다.
        // join(timeout) 은 "기다림"을 끊는 것이지 "스레드"를 끊는 것이 아니다.
        assertThat(resultsAfterFullJoin).containsExactly("느림-결과");
        assertThat(slow.getState()).isEqualTo(Thread.State.TERMINATED);
    }

    @Test
    @DisplayName("6. 순서를 강제하려면 동시성을 포기해야 한다")
    void forcingOrderCostsConcurrency() throws InterruptedException {
        List<String> completionOrder = new CopyOnWriteArrayList<>();

        Thread slow = new Thread(() -> {
            SlowApi.call("느림", SLOW_MILLIS);
            completionOrder.add("느림");
        }, "slow");
        Thread fast = new Thread(() -> {
            SlowApi.call("빠름", FAST_MILLIS);
            completionOrder.add("빠름");
        }, "fast");

        long start = System.currentTimeMillis();

        // start() 와 join() 을 짝지어 부르면 순서는 지켜진다. 대신 동시에 돌지 않는다.
        slow.start();
        slow.join();
        fast.start();
        fast.join();

        long elapsed = System.currentTimeMillis() - start;

        System.out.println("완료 순서 : " + completionOrder + "   (원하는 순서대로 나왔다)");
        System.out.println("걸린 시간 : " + elapsed + "ms   (동시에 돌렸다면 500ms 였다)");

        // 순서는 얻었다.
        assertThat(completionOrder).containsExactly("느림", "빠름");

        // 그 대가로 시간은 더해졌다. 순서와 동시성은 맞바꾸는 관계다.
        assertThat(elapsed).isGreaterThanOrEqualTo(SLOW_MILLIS + FAST_MILLIS);
    }
}
