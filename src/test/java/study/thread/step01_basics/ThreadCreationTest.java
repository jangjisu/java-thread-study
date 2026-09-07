package study.thread.step01_basics;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import study.thread.support.Timeline;

/**
 * step01 — 스레드를 만드는 방법과, {@code start()}가 정확히 무엇을 하는가.
 *
 * <p>과제를 하나씩 채워 넣는다. 전부 빨간불로 시작한다.
 * 하나 통과시킬 때마다 초록불이 하나씩 늘어난다.
 *
 * <p>쓸 수 있는 도구:
 * <ul>
 *   <li>{@link Timeline#log(String)} — 어느 스레드가 언제 무엇을 했는지 한 줄 출력
 *   <li>{@link study.thread.support.Sleeper#millis(long)} — try-catch 없는 sleep
 * </ul>
 */
class ThreadCreationTest {

    @BeforeEach
    void resetTimeline() {
        Timeline.reset();
    }

    @Test
    @DisplayName("과제1 - Thread를 상속해 만든 스레드가 그 스레드에서 실행됨을 증명한다")
    void extendThread() {
        // 1. Thread를 상속한 스레드를 만들고 이름을 "extends-thread"로 준다
        // 2. run() 안에서 자기가 어느 스레드인지 붙잡는다
        // 3. 시작시키고, 끝날 때까지 기다린 뒤, 붙잡은 이름이 "extends-thread"인지 단언한다
        //
        // 생각할 것: "새 스레드에서 실행됐다"를 눈이 아니라 값으로 증명하려면 무엇을 붙잡아야 하나?
        // 힌트: 스레드 안에서 만든 값을 밖으로 꺼내려면 AtomicReference<String>

        fail("아직 작성하지 않았다");
    }

    @Test
    @DisplayName("과제2 - Runnable로 만들면 '할 일'과 '실행 주체'가 분리된다")
    void implementRunnable() {
        // 1. Runnable 하나를 만든다 (람다로)
        // 2. 그 Runnable을 새 스레드에게 실행시킨다 → 스레드 이름이 찍히는지 단언
        // 3. 똑같은 Runnable을 이번엔 그냥 task.run()으로 직접 호출한다 → 이번엔 누구 이름이 찍히나?
        //
        // 생각할 것: 같은 작업 객체를 두 주체가 실행할 수 있다는 게 왜 중요한가?
        //           (step05 스레드 풀, step07 가상 스레드가 여기서 출발한다)

        fail("아직 작성하지 않았다");
    }

    @Test
    @DisplayName("과제3 - run()은 스레드를 시작시키지 않는다")
    void runDoesNotStartAThread() {
        // 1. 스레드를 만들되 start()가 아니라 run()을 호출한다
        // 2. 실제로 실행한 게 누구인지 단언한다
        // 3. 그 스레드의 getState()가 무엇인지 단언한다
        //
        // 생각할 것: 3번의 상태값이 이 과제의 핵심 증거다. 왜 그런가?

        fail("아직 작성하지 않았다");
    }

    @Test
    @DisplayName("과제4 - 끝난 스레드는 다시 시작할 수 없다")
    void startCanBeCalledOnlyOnce() {
        // 1. 스레드를 start() 하고 join()으로 끝날 때까지 기다린다
        // 2. 끝난 뒤의 getState()를 단언한다
        // 3. 그 스레드를 다시 start() 하면 어떻게 되는지 확인하고 단언한다
        //
        // 힌트: assertThatThrownBy(...).isInstanceOf(...)
        // 생각할 것: 스레드를 재사용할 수 없다면, 요청이 초당 1000개 오면 어떻게 되나?

        fail("아직 작성하지 않았다");
    }

    @Test
    @DisplayName("과제5 - join()은 그 스레드가 끝날 때까지 기다린다")
    void joinWaitsForCompletion() {
        // 1. 100ms쯤 걸리는 작업을 하는 스레드를 만든다 (Sleeper.millis 사용)
        // 2. 작업 결과를 리스트에 담게 한다
        // 3. start() → join() 후, 결과가 리스트에 들어있는지 단언한다
        // 4. Timeline.log()를 곳곳에 심어서 main이 언제부터 언제까지 멈춰 있었는지 눈으로 확인한다
        //
        // 생각할 것: join() 하는 동안 main은 뭘 하고 있었나? 그게 낭비인가?

        fail("아직 작성하지 않았다");
    }

    @Test
    @DisplayName("과제6 - [관찰] 여러 스레드의 실행 순서는 보장되지 않는다")
    @Tag("observation")
    void executionOrderIsNotGuaranteed() {
        // 1. t1~t5 이름의 스레드 5개를 만들어 순서대로 start() 한다
        // 2. 각자 끝난 순서를 기록하게 한다 (여러 스레드가 같이 쓰므로 CopyOnWriteArrayList)
        // 3. 전부 join() 한 뒤 시작 순서와 끝난 순서를 Timeline.log()로 찍는다
        //
        // 주의: 이 테스트에는 단언을 걸지 않는다.
        //       뒤바뀌는 것도 정상이고 안 뒤바뀌는 것도 정상이라, 어느 쪽을 단언해도 거짓말이 된다.
        //       그래서 @Tag("observation")이 붙어 있다.
        //
        // 다 만들었으면 fail을 지우고 여러 번 돌려본다. 매번 같은 결과가 나오나?

        fail("아직 작성하지 않았다");
    }
}
