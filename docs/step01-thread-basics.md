# step01 — Thread와 Runnable

테스트: [`step01_basics/ThreadCreationTest`](../src/test/java/study/thread/step01_basics/ThreadCreationTest.java)

## 무엇을 확인했나

### 1. "새 스레드에서 실행됐다"를 증명하는 방법

출력을 눈으로 보는 대신, **실제로 실행한 스레드의 이름을 붙잡아** 단언했다.

```java
Thread thread = new Thread(() -> ranOn.set(Thread.currentThread().getName()), "runnable-thread");
thread.start();
thread.join();

assertThat(ranOn.get()).isEqualTo("runnable-thread");
```

이 저장소 전체에서 반복될 패턴이다. 동시성은 "그렇게 보였다"와 "그렇다"의 간극이 큰 분야라,
관찰한 것을 항상 붙잡을 수 있는 값으로 바꾼다.

### 2. `start()`와 `run()`

`run()`은 **그냥 메서드 호출**이다. 새 스레드가 생기지 않는다.

```java
thread.run();

assertThat(ranOn.get()).isEqualTo(Thread.currentThread().getName()); // 내가 실행했다
assertThat(thread.getState()).isEqualTo(Thread.State.NEW);           // 태어나지도 않았다
```

`State.NEW`가 결정적인 증거다. `start()`를 부르지 않았으니 스레드는 시작조차 안 했고,
`run()`의 코드는 호출한 스레드가 자기 스택에서 실행했을 뿐이다.

새 스레드를 만드는 것은 `start()`뿐이다. `start()`가 OS 스레드를 만들고, 그 스레드가 `run()`을 호출한다.

### 3. `Thread` 상속 vs `Runnable`

| | Thread 상속 | Runnable |
|---|---|---|
| 상속 자리 | 이미 써버림 (자바는 단일 상속) | 비어 있음 |
| 관심사 | 실행 주체 + 할 일이 한 덩어리 | **할 일만** — 누가 실행할지는 모름 |

Runnable은 "무엇을 할지"만 안다. 그래서 **같은 작업을 다른 실행 주체에게 넘길 수 있다.**

```java
Runnable task = () -> ...;

new Thread(task).start();  // 새 스레드가 실행
task.run();                // 그냥 내가 실행
// executor.submit(task);  // 스레드 풀이 실행 (step05)
```

작업과 실행 주체의 분리 — 이게 step05의 `ExecutorService`, 나아가 step07의 가상 스레드로 이어지는 출발점이다.
가상 스레드도 결국 "이 `Runnable`을 누가 실행하느냐"의 답을 바꾼 것이다.

### 4. 스레드는 재사용할 수 없다

```java
thread.start();
thread.join();
assertThat(thread.getState()).isEqualTo(Thread.State.TERMINATED);

assertThatThrownBy(thread::start).isInstanceOf(IllegalThreadStateException.class);
```

끝난 스레드는 되살릴 수 없다. 그래서 요청마다 스레드를 새로 만들면 매번 생성 비용을 낸다.
→ **스레드 풀이 존재하는 이유** (step05)
→ **그런데 가상 스레드는 "그냥 매번 만들어라"라고 한다** (step07). 여기서 뒤집힌다.

### 5. `join()`

`join()`이 돌아왔다는 것은 그 스레드가 끝났다는 뜻이다. 타임라인이 그대로 보여준다.

```
     0ms | worker           | 작업 시작
     0ms | Test worker      | worker를 기다린다
   107ms | worker           | 작업 끝
   107ms | Test worker      | worker가 끝난 것을 확인했다
```

`main`이 107ms 동안 멈춰 있었다. 기다리는 쪽은 그동안 아무것도 못 한다 —
이 낭비를 어떻게 줄일 것인가가 step06(`CompletableFuture`)의 질문이다.

### 6. 실행 순서는 보장되지 않는다 (관찰)

t1~t5를 순서대로 `start()`했는데:

```
시작 순서: [t1, t2, t3, t4, t5]
끝난 순서: [t2, t3, t4, t1, t5]
```

첫 실행부터 뒤바뀌었다. `start()` 호출 순서는 **실행 순서를 약속하지 않는다.**
스케줄링은 OS가 결정하고, 우리는 관여하지 못한다.

이 테스트에는 단언을 걸지 않았다. 뒤바뀌는 것도 정상이고 안 뒤바뀌는 것도 정상이라,
어느 쪽을 단언해도 거짓말이 되기 때문이다.

## 남은 질문 (다음 단계로)

- 실행 중인 스레드를 밖에서 어떻게 멈추나? → step02 (interrupt)
- 이 스레드들이 **같은 변수**를 건드리면? → step03 (공유 상태)
