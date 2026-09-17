package study.thread.step02;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import study.thread.support.Timeline;

/**
 * step02 미션 — 문제 상태.
 *
 * <p>상황: 상품 상세 페이지 하나를 그리려면 API 세 개를 불러야 한다.
 * 각각 걸리는 시간이 다르다.
 *
 * <ul>
 *   <li>재고 조회 — 500ms
 *   <li>리뷰 조회 — 300ms
 *   <li>가격 조회 — 100ms
 * </ul>
 *
 * <p>{@code SlowApi.callWithLog(이름, 밀리초)} 로 호출한다.
 */
class Step02MissionTest {

    private static final int STOCK_MILLIS = 500;
    private static final int REVIEW_MILLIS = 300;
    private static final int PRICE_MILLIS = 100;

    @BeforeEach
    void resetTimeline() {
        Timeline.reset();
    }

    /*
     * 미션 1. 세 API 를 동시에 호출하고, 끝난 순서대로 completionOrder 에 이름을 담는다.
     *
     * 만들 것:
     *   - "재고", "리뷰", "가격" 순서로 start() 한다
     *   - 각 스레드는 호출이 끝나면 자기 이름을 completionOrder 에 담는다
     *   - 셋 다 끝날 때까지 기다린다
     *
     * 다 만들면 알게 되는 것:
     *   먼저 시킨 것이 먼저 끝나지 않는다. 끝나는 순서는 걸린 시간이 정한다.
     */
    @Test
    @DisplayName("미션1 - 완료 순서는 각 작업의 소요 시간으로 결정된다")
    void completionOrderFollowsDuration() throws InterruptedException {
        List<String> completionOrder = new CopyOnWriteArrayList<>();

        long start = System.currentTimeMillis();

        // 여기부터 직접 만든다

        long elapsed = System.currentTimeMillis() - start;

        System.out.println("start() 부른 순서 : [재고, 리뷰, 가격]");
        System.out.println("완료된 순서       : " + completionOrder);
        System.out.println("걸린 시간         : " + elapsed + "ms");

        assertThat(completionOrder)
                .as("빨리 끝나는 것부터 담긴다. 시작 순서와 정확히 반대다")
                .containsExactly("가격", "리뷰", "재고");

        assertThat(elapsed)
                .as("동시에 돌렸다면 가장 오래 걸린 재고(500ms) 만큼만 걸려야 한다")
                .isLessThan(STOCK_MILLIS + REVIEW_MILLIS);
    }

    /*
     * 미션 2. 느린 API 를 무한정 기다리지 않는다.
     *
     * 실무 상황:
     *   재고 API 가 500ms 걸리는데, 우리 화면은 200ms 안에 응답해야 한다.
     *   그렇다면 200ms 까지만 기다려보고, 아직 안 끝났으면 포기하고 넘어가야 한다.
     *
     * 만들 것:
     *   - 재고 API 를 호출해서 그 결과를 results 에 담는 스레드를 만들고 start() 한다
     *   - join(WAIT_LIMIT_MILLIS) 로 200ms 만 기다린다
     *   - 기다리기를 포기한 그 시점의 results 를 resultsAtTimeout 에 복사해 둔다
     *         resultsAtTimeout = List.copyOf(results);
     *   - 그 다음 join() 으로 끝까지 기다린다
     *
     * 다 만들면 알게 되는 것:
     *   포기한 시점에는 결과가 없는데, 끝까지 기다려 보면 결과가 들어와 있다.
     *   내가 기다리기를 포기했을 뿐 스레드는 자기 일을 계속하고 있었다는 뜻이다.
     *   join(timeout) 은 "기다림"을 끊는 것이지 "스레드"를 끊는 것이 아니다.
     *
     *   그럼 진짜로 그 스레드를 멈추게 하려면 어떻게 해야 할까? -> step03
     */
    @Test
    @DisplayName("미션2 - join(timeout) 이 반환된 뒤에도 스레드는 실행을 계속한다")
    void joinWithTimeoutDoesNotStopTheThread() throws InterruptedException {
        final int waitLimitMillis = 200;

        List<String> results = new CopyOnWriteArrayList<>();
        List<String> resultsAtTimeout = List.of();

        long start = System.currentTimeMillis();

        // 여기부터 직접 만든다

        long waited = System.currentTimeMillis() - start;

        System.out.println("기다린 시간        : " + waited + "ms");
        System.out.println("포기한 시점의 결과 : " + resultsAtTimeout);
        System.out.println("끝까지 기다린 뒤   : " + results);

        assertThat(waited)
                .as("200ms 는 기다려야 하고, 500ms 를 다 기다리면 안 된다")
                .isGreaterThanOrEqualTo(waitLimitMillis)
                .isLessThan(STOCK_MILLIS);

        assertThat(resultsAtTimeout)
                .as("포기한 시점에는 아직 작업이 안 끝나서 결과가 없어야 한다")
                .isEmpty();

        assertThat(results)
                .as("끝까지 기다려 보면 결과가 들어와 있다 - 스레드는 계속 일하고 있었다")
                .containsExactly("재고-결과");
    }
}
