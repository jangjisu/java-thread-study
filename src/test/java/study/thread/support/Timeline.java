package study.thread.support;

import java.util.concurrent.TimeUnit;

/**
 * 어느 스레드가 언제 무엇을 했는지 한 줄로 찍는다.
 *
 * <p>동시성 코드는 결과만 봐서는 왜 그렇게 됐는지 알 수 없다.
 * 테스트가 통과/실패만 말하는 대신 실행 과정을 보여주게 하려고 둔 도구다.
 */
public final class Timeline {

    private static volatile long origin = System.nanoTime();

    private Timeline() {
    }

    /** 지금부터를 0ms로 삼는다. 테스트 시작 시점에 호출한다. */
    public static void reset() {
        origin = System.nanoTime();
    }

    public static void log(String message) {
        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - origin);
        Thread current = Thread.currentThread();
        System.out.printf("%6dms | %-16s | %s%n", elapsedMillis, describe(current), message);
    }

    private static String describe(Thread thread) {
        String name = thread.getName();
        if (name.isEmpty()) {
            name = "#" + thread.threadId();
        }
        return thread.isVirtual() ? name + "(V)" : name;
    }
}
