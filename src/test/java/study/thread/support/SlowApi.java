package study.thread.support;

/**
 * 호출에 시간이 걸리는 외부 API. 모든 step 에서 공용으로 쓴다.
 *
 * <p>{@link #call} 은 아무것도 출력하지 않는다. 스레드 상태를 관찰하는 테스트에서는
 * 출력이 락 경합을 만들어 상태가 BLOCKED 로 보이기 때문이다.
 * 타임라인이 필요하면 {@link #callWithLog} 를 쓴다.
 */
public final class SlowApi {

    /** 외부 API 한 번 호출에 걸리는 기본 시간. */
    public static final int CALL_MILLIS = 500;

    private SlowApi() {
    }

    /** 기본 시간만큼 걸리는 호출. 조용히 실행된다. */
    public static String call(String name) {
        return call(name, CALL_MILLIS);
    }

    /** 걸리는 시간을 지정하는 호출. 조용히 실행된다. */
    public static String call(String name, int millis) {
        Sleeper.millis(millis);
        return name + "-결과";
    }

    /** 기본 시간만큼 걸리는 호출. 시작과 응답 시각을 타임라인에 찍는다. */
    public static String callWithLog(String name) {
        return callWithLog(name, CALL_MILLIS);
    }

    /** 걸리는 시간을 지정하는 호출. 시작과 응답 시각을 타임라인에 찍는다. */
    public static String callWithLog(String name, int millis) {
        Timeline.log(name + " 호출 시작 (" + millis + "ms 예상)");
        String result = call(name, millis);
        Timeline.log(name + " 응답 받음");
        return result;
    }
}
