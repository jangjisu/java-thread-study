package study.thread.support;

/**
 * 예제 코드에서 {@code try-catch}가 본론을 가리지 않도록 감싼 sleep.
 *
 * <p>인터럽트를 삼키지 않는다 — 잡은 즉시 인터럽트 상태를 되돌려 놓고
 * 언체크 예외로 바꿔 던진다. 인터럽트를 왜 이렇게 다뤄야 하는지는 step02에서 다룬다.
 */
public final class Sleeper {

    private Sleeper() {
    }

    public static void millis(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("자는 도중 인터럽트됨", e);
        }
    }
}
