package study.thread.step01_basics;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import study.thread.support.Timeline;

/**
 * step01 문제 — Thread와 Runnable.
 *
 * <p>먼저 {@code docs/step01/이론.md}를 읽는다. 거기 나온 것만으로 전부 풀 수 있다.
 *
 * <p>각 문제는 "무엇을 확인하려는 문제인지"를 먼저 말하고, 그 다음에 할 일을 준다.
 * 전부 빨간불로 시작한다. 하나 통과시킬 때마다 초록불이 하나 늘어난다.
 */
class ThreadCreationTest {

    @BeforeEach
    void resetTimeline() {
        Timeline.reset();
    }

    /*
     * 문제 1.
     *
     * 확인하려는 것:
     *   Thread를 상속해서 run()을 재정의하면, 그 코드는 정말 "새 스레드"에서 실행되는가?
     *
     * 그런데 "새 스레드에서 실행됐다"를 어떻게 확인하지? 눈으로 보는 건 증명이 아니다.
     * 이론 4장의 Thread.currentThread().getName()을 쓰면
     * 그 코드를 실행 중인 스레드가 누구인지 이름으로 알 수 있다.
     *
     * 할 일:
     *   1) Thread를 상속한 스레드를 만든다. 이름은 "extends-thread" (생성자로 넘긴다)
     *   2) 재정의한 run() 안에서 Thread.currentThread().getName()을 읽어 바깥으로 꺼낸다
     *      (꺼내는 방법은 이론 5장)
     *   3) start() 하고 join() 한다
     *   4) 꺼낸 이름이 "extends-thread"인지 assertThat으로 확인한다
     *
     * 통과하면 이런 뜻이다:
     *   "run() 안의 코드는 main이 아니라 내가 만든 그 스레드가 실행했다"
     */
    @Test
    @DisplayName("문제1 - 상속해서 만든 스레드가 실제로 그 스레드에서 실행됨을 증명한다")
    void extendThread() {
        // 아래는 작성 중인 코드다.
        // 힌트: 이론 3장에서 Thread.run()의 실제 구현을 다시 본다.
        //       생성자에 Runnable을 안 넘겼고 run()도 재정의하지 않았다면, run()은 무엇을 실행하나?
        Thread thread = new Thread("extends-thread");

        thread.run();

        fail("아직 풀지 않았다");
    }

    /*
     * 문제 2.
     *
     * 확인하려는 것:
     *   Runnable은 "할 일"일 뿐이고 "누가 실행할지"는 정해져 있지 않다.
     *   정말 그런지, 같은 Runnable을 두 주체가 실행해서 확인한다.
     *
     * 할 일:
     *   1) Runnable 하나를 람다로 만든다. 하는 일은 문제1과 같다
     *      (실행 중인 스레드 이름을 바깥 상자에 담기)
     *   2) 그 Runnable을 이름 "runnable-thread"인 새 스레드에게 실행시킨다
     *      → 상자에 담긴 이름을 확인한다
     *   3) 똑같은 Runnable 객체를 이번엔 task.run() 으로 직접 호출한다
     *      → 상자에 담긴 이름을 다시 확인한다. 이번엔 누구 이름이 들어 있나?
     *
     * 통과하면 이런 뜻이다:
     *   "같은 할 일을 누구에게 시키느냐는 나중에 정할 수 있다"
     *   (이 성질 덕분에 step05의 스레드 풀과 step07의 가상 스레드가 가능해진다)
     */
    @Test
    @DisplayName("문제2 - 같은 Runnable을 두 주체가 실행할 수 있다")
    void implementRunnable() {
        fail("아직 풀지 않았다");
    }

    /*
     * 문제 3. ★ step01에서 가장 중요한 문제
     *
     * 확인하려는 것:
     *   start()와 run()은 뭐가 다른가?
     *
     * 흔한 오해: "run()을 부르면 스레드가 돈다"
     * 실제: run()은 그냥 평범한 메서드 호출이다. 새 스레드는 생기지 않는다.
     *
     * 할 일:
     *   1) 스레드를 하나 만든다 (이름은 아무거나, 예: "never-started")
     *      run()이 실행되면 실행 중인 스레드 이름을 상자에 담게 한다
     *   2) start()가 아니라 run()을 호출한다
     *   3) 상자에 담긴 이름이 누구인지 확인한다
     *      → 힌트: 지금 이 테스트 메서드를 실행 중인 스레드 이름과 비교해본다
     *   4) 그 스레드의 getState()를 확인한다
     *      → Thread.State의 6개 값 중 어느 것일지 이론 4장을 보고 예상한 뒤 단언한다
     *
     * 통과하면 이런 뜻이다:
     *   "run()을 불러도 스레드는 시작조차 하지 않았다"
     *   4)의 상태값이 그 결정적 증거다.
     */
    @Test
    @DisplayName("문제3 - run()은 스레드를 시작시키지 않는다")
    void runDoesNotStartAThread() {
        fail("아직 풀지 않았다");
    }

    /*
     * 문제 4.
     *
     * 확인하려는 것:
     *   한 번 끝난 스레드를 다시 쓸 수 있는가?
     *
     * 할 일:
     *   1) 스레드를 만들어 start() 하고 join()으로 끝날 때까지 기다린다
     *   2) 끝난 뒤의 getState()를 확인한다 (6개 값 중 어느 것일까?)
     *   3) 그 스레드를 다시 start() 하면 어떻게 되는지 확인한다
     *      → 이론 4장에 어떤 예외가 나는지 적혀 있다
     *      → 힌트: assertThatThrownBy(t::start).isInstanceOf(???.class)
     *
     * 통과하면 이런 뜻이다:
     *   "스레드는 일회용이다. 재사용할 수 없다."
     *
     * 생각해볼 것 (코드로 쓸 필요는 없다):
     *   그렇다면 초당 1000개의 요청이 들어오는 서버는 스레드를 어떻게 다뤄야 할까?
     *   이 질문의 답이 step05다.
     */
    @Test
    @DisplayName("문제4 - 끝난 스레드는 다시 시작할 수 없다")
    void startCanBeCalledOnlyOnce() {
        fail("아직 풀지 않았다");
    }

    /*
     * 문제 5.
     *
     * 확인하려는 것:
     *   join()은 정확히 무엇을 보장하나? 그리고 그동안 누가 멈춰 있나?
     *
     * 할 일:
     *   1) 100ms쯤 걸리는 일을 하는 스레드를 만든다 (Sleeper.millis(100) 사용)
     *      일이 끝나면 결과를 바깥 리스트에 담게 한다
     *   2) start() 하고, join()으로 기다린 다음, 리스트에 결과가 들어있는지 확인한다
     *   3) Timeline.log()를 최소 네 군데 심는다:
     *      - 작업 스레드가 일을 시작할 때 / 끝낼 때
     *      - main이 기다리기 시작할 때 / 기다림이 끝났을 때
     *   4) 테스트를 돌리고 콘솔에 찍힌 시각을 본다
     *
     * 통과하면 이런 뜻이다:
     *   "join()이 돌아왔다는 것은 그 스레드가 확실히 끝났다는 뜻이다"
     *
     * 콘솔을 보고 답해볼 것:
     *   main은 몇 ms 동안 아무것도 못 하고 서 있었나? 그 시간은 낭비인가?
     *   이 질문이 step06과 step07로 이어진다.
     */
    @Test
    @DisplayName("문제5 - join()은 그 스레드가 끝날 때까지 기다린다")
    void joinWaitsForCompletion() {
        fail("아직 풀지 않았다");
    }

    /*
     * 문제 6. [관찰 문제 — 단언하지 않는다]
     *
     * 확인하려는 것:
     *   start()를 부른 순서대로 스레드가 실행되는가?
     *
     * 할 일:
     *   1) 이름이 "t1"~"t5"인 스레드 5개를 만들어 순서대로 start() 한다
     *      각자 Timeline.log()를 찍고, 자기 이름을 공용 리스트에 넣는다
     *      (여러 스레드가 동시에 넣으므로 java.util.concurrent.CopyOnWriteArrayList 를 쓴다)
     *   2) 전부 join()으로 기다린다
     *   3) 끝난 순서를 Timeline.log()로 찍는다
     *   4) fail을 지우고 여러 번 돌려본다. 매번 같은 결과가 나오나?
     *
     * 여기엔 assert를 쓰지 않는다:
     *   순서가 뒤바뀌는 것도 정상이고 안 뒤바뀌는 것도 정상이다.
     *   어느 쪽을 단언해도 그 테스트는 언젠가 거짓말을 하게 된다.
     *   그래서 @Tag("observation")을 붙여 검증 테스트와 구분해 둔다.
     */
    @Test
    @DisplayName("문제6 - [관찰] 실행 순서는 보장되지 않는다")
    @Tag("observation")
    void executionOrderIsNotGuaranteed() {
        fail("아직 풀지 않았다");
    }
}
