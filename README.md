# java-thread-study

자바 스레드를 **테스트로 관찰하며** 배우는 저장소.

기본 구조(`Thread`, `Runnable`, 공유 상태, 스레드 풀)를 손으로 만져본 뒤 가상 스레드로 넘어간다.
가상 스레드의 의미는 플랫폼 스레드의 한계를 겪어봐야 드러나기 때문이다.

- Java 25 (Temurin) · Gradle 9.7.1 · JUnit 5 + AssertJ
- 외부 동시성 라이브러리는 쓰지 않는다. 배우려는 원리가 라이브러리 뒤로 숨기 때문에.

## 진행 방식

각 단계는 **이론 → 문제 → 정리** 세 덩어리다. 문제는 내가 직접 푼다.

1. **읽는다** — `docs/step0N/이론.md`
   그 단계에서 쓰는 클래스와 메서드가 무엇이고 각각 뭘 하는지. 실제 JDK 소스에서 뽑은 시그니처로.
   끝에 "읽고 나서 스스로 답해보기"가 있다. 여기서 막히면 다시 읽는다.
2. **푼다** — `src/test/.../step0N_*/`
   문제마다 *무엇을 확인하려는 문제인지* → *할 일* → *통과하면 무슨 뜻인지* 순으로 적혀 있다.
   전부 `fail("아직 풀지 않았다")`로 시작한다. 필요한 API는 이론에 전부 나와 있다.
3. **적는다** — `docs/step0N/정리.md`
   풀면서 알게 된 것, 막혔던 지점, 아직 모르겠는 것.

막히면 물어본다 — 답이 아니라 힌트를 받는다.

## 실행

```bash
./gradlew test
```

한 단계만 돌리려면:

```bash
./gradlew test --tests '*ThreadCreationTest*'
```

테스트가 찍는 타임라인 로그가 콘솔에 그대로 나온다.

```
     0ms | worker           | 작업 시작
     0ms | Test worker      | worker를 기다린다
   107ms | worker           | 작업 끝
```

## 로드맵

| | 단계 | 주제 | 핵심 질문 |
|---|---|---|---|
| 🟡 | 01 | Thread / Runnable | `start()`와 `run()`은 뭐가 다른가, 왜 Runnable을 쓰나 |
| ⬜ | 02 | 생명주기 · interrupt | 스레드는 어떻게 끝나나, 취소는 왜 "협력"인가 |
| ⬜ | 03 | 공유 상태 | 경합 재현 → `synchronized` / `volatile` / `Atomic`은 각각 뭘 해결하나 |
| ⬜ | 04 | wait·notify · Lock | 조건 대기, `ReentrantLock`, 데드락 만들어보기 |
| ⬜ | 05 | ExecutorService | 스레드를 왜 직접 안 만드나, 풀 고갈은 어떻게 생기나 |
| ⬜ | 06 | CompletableFuture | 비동기 조합과 예외 전파, 어느 스레드에서 도나 |
| ⬜ | 07 | 가상 스레드 | 플랫폼 스레드와 뭐가 다른가, 캐리어·pinning, 100만 개 실험 |
| ⬜ | 08 | 구조적 동시성 · ScopedValue | `--enable-preview`, `ThreadLocal`의 대안 |

단계마다 [`docs/step0N/`](docs/)에 이론과 정리가 한 쌍으로 쌓인다.

- step01: [이론](docs/step01/이론.md) · [정리](docs/step01/정리.md)

## 테스트 두 종류를 섞지 않는다

동시성 테스트는 결과가 흔들린다. 그래서 성격을 갈라놓는다.

- **검증 테스트** — 실행 순서를 못 박아 항상 같은 결과가 나오게 만든 뒤 단언한다.
- **관찰 테스트** (`@Tag("observation")`) — 경합이나 순서 뒤바뀜을 일부러 드러낸다. **단언하지 않는다.**
  경합은 "가끔" 일어나는 것이 본질이라, 단언을 걸면 그 테스트가 거짓말쟁이가 된다.

관찰 테스트만 빼고 돌리려면:

```bash
./gradlew test -PexcludeTags=observation
```

## 구조

```
src/test/java/study/thread/
├── support/            공통 관찰 도구 (Timeline, Sleeper)
├── step01_basics/      단계 하나 = 패키지 하나
└── ...
docs/                   단계 하나 = 노트 하나
```
