# 암기카드 (memo-cards) 작업 규칙

개인용 안드로이드 암기 앱(Kotlin, Compose, Room)이다. 사용자가 공부 자료(옵시디언 노트, 텍스트, PDF 등)를 주면
Claude가 **암기 카드 JSON**을 만들고, 사용자가 앱에서 그 JSON을 가져와 암기한다.

- 언어: 응답·주석·문서·커밋 메시지는 한국어, 변수명·함수명은 영어.
- 옵시디언 볼트: `D:\drive\obsidian\Obsidian Vault` (노트는 **읽기만** 한다. 원본·`_자료/`·`raw/`는 수정 금지)

---

## 1. 카드 만들기 (가장 자주 하는 작업)

사용자가 "이 노트/자료로 카드 만들어줘"라고 하면 아래 순서로 한다.

1. 자료를 읽는다. 노트 경로, 폴더, 붙여넣은 텍스트, 업로드한 파일 모두 가능하다.
   - 볼트의 `company/`, `회사/` 노트는 기밀 우려가 있어 사용자가 명시하지 않으면 제외한다.
2. **암기가 필요한 항목만** 뽑는다. 자료 전체를 옮기지 않는다.
3. 카드 JSON을 `data\<덱 이름>.json`에 저장한다 (UTF-8, 스키마는 2장).
4. 사용자에게 카드 수와 예시 2~3개를 보여주고, 폰으로 옮기는 방법(4장)을 안내한다.
5. 추측하거나 자료에 없는 내용은 카드로 만들지 않는다. 일반 지식으로 보충했다면 `extra`에 "자료 외 일반 지식"이라고 표시한다.
6. 만든 파일은 git에 커밋한다 (6장).

### 무엇을 카드로 만드는가
| 자료 종류 | front | back | 비고 |
|---|---|---|---|
| 외국어 단어 노트 (`type: vocab`) | 단어 (노트 제목) | 짧은 뜻 (`meaning` 기준) | `reading`에 히라가나 읽기, `extra`에 예문 1개 |
| 개념·용어 | 용어 또는 질문 | 정의 또는 답 | 한 카드는 **한 가지만** 묻는다 |
| 비교 개념 | "A와 B의 차이는?" | 차이 요약 | 표가 있으면 핵심만 줄여서 |
| 코드 문제 | 코드 + "출력은?" | 정답 | `extra`에 이유 설명 |
| 자격증 문제 | 문제 | 정답 | `extra`에 해설, 선택지는 front에 줄바꿈으로 |

- 너무 긴 설명은 쪼갠다. back은 화면에 한눈에 들어오게 짧게 쓴다.
- 줄바꿈이 필요하면 JSON 문자열 안에 `\n`을 쓴다.
- 같은 내용을 중복해서 만들지 않는다. 기존 덱에 있는 uid인지 먼저 확인한다.

---

## 2. 카드 JSON 스키마 (version 1)

코드 정의: `app/src/main/java/com/kanghm/memocards/data/DeckJson.kt`, 예시: `data/sample.json`, `data/test-java.json`

```json
{
  "version": 1,
  "decks": [
    {
      "name": "덱 이름",
      "source": "출처 (노트 경로, 책 이름 등)",
      "cards": [
        {
          "uid": "고정 식별자",
          "front": "앞면 (필수)",
          "back": "뒷면 (필수)",
          "reading": "읽기 (선택, 일본어는 히라가나)",
          "extra": "예문·해설·출처 (선택)",
          "oneWay": "true면 방향 설정과 상관없이 항상 front가 앞면 (코드 문제 등, 선택, 기본 false)"
        }
      ]
    }
  ]
}
```

- `progress` 필드는 앱이 내보내는 **백업 파일**에만 있다. 카드를 새로 만들 때는 넣지 않는다.
- 모르는 키는 앱이 무시하지만, 필수 키(`name`, `cards`, `uid`, `front`, `back`)가 빠지면 가져오기가 실패한다.

### uid 규칙 (중요)
- `uid`는 같은 카드를 식별하는 **고정 값**이다. 앱은 uid로 재가져오기 때 알고있음/모름 기록을 이어 붙인다.
- 형식: **볼트 기준 노트 경로** (`/` 구분), 한 노트에서 여러 장이면 `경로#항목` (예: `language/java/….md#멀티태스킹`).
  노트가 없는 자료는 `<자료이름>/<항목>`.
- **한번 정한 uid는 바꾸지 않는다.** 바꾸면 새 카드로 취급돼 기존 진행 기록이 끊긴다.
- 내용(front/back/extra)을 고치는 것은 괜찮다. 같은 uid로 다시 가져오면 내용만 갱신되고 기록은 유지된다.
- 덱 파일 하나에 같은 uid가 두 번 나오면 안 된다.

### 덱 이름
- 앱 홈에 주제별 목록으로 보인다. 덱 이름이 같으면 같은 덱으로 합쳐진다.
- 예: `Java Silver - 클래스와 상속`, `일본어 단어`, `영어 표현`. 너무 크게 묶지 말고 범위별로 나눈다.

---

## 3. Java Silver 등 코드·자격증 카드

- 코드는 front에 그대로 넣고 마지막에 "출력은?"처럼 질문을 붙인다. 들여쓰기와 줄바꿈(`\n`)을 유지한다.
- **코드 문제·질문형 카드는 `"oneWay": true`로 만든다.** 안 그러면 앱에서 `뜻→단어` 방향일 때 정답이 앞면에 나온다.
- 정답은 back에 짧게, 이유는 `extra`에 쓴다.
- 코드와 정답은 **실제로 맞는지 확인**한다. 확신이 없으면 카드로 만들지 않거나 `extra`에 "미검증"이라고 적는다.
- 교재·덤프 문제 원문을 그대로 옮기지 않는다. 사용자가 직접 요약·재구성한 내용이나 개념 설명 위주로 만든다.
  원문 자료는 볼트의 `_자료/`에 두고 읽기만 한다.

---

## 4. 폰으로 옮기기

1. 폰을 USB로 연결하고 USB 디버깅을 켠다.
2. 파일을 폰의 Download 폴더로 보낸다 (Git Bash는 경로 변환을 꺼야 한다).
   ```
   MSYS_NO_PATHCONV=1 /d/drive/tools/android-sdk/platform-tools/adb.exe push data/<덱>.json /sdcard/Download/<덱>.json
   ```
3. 앱 홈 오른쪽 위 **가져오기(위쪽 화살표)** → 선택 창 ☰ → **다운로드** → 파일 선택.
   ("최근" 목록에는 안 보일 수 있다.)
4. "덱 N개 가져옴 (새 카드 X, 갱신 Y)" 메시지를 확인한다.

- 같은 파일을 다시 가져와도 안전하다. 기존 카드는 내용만 갱신되고 진행 기록은 유지된다.
- 파일에서 지운 카드는 앱에서 자동으로 지워지지 않는다 (덱 삭제는 홈에서 직접).
- **백업**: 앱 홈의 내보내기(아래쪽 화살표)로 진행 기록 포함 JSON을 저장할 수 있다.

---

## 5. 앱 빌드·설치

환경은 모두 D드라이브에 있다. 새 셸에서는 환경변수가 안 잡혀 있을 수 있어 명령마다 지정한다.

```
export JAVA_HOME=/d/drive/tools/jdk17 GRADLE_USER_HOME=/d/drive/tools/gradle-home
./gradlew.bat testDebugUnitTest      # 단위 테스트
./gradlew.bat assembleDebug          # app/build/outputs/apk/debug/app-debug.apk
MSYS_NO_PATHCONV=1 /d/drive/tools/android-sdk/platform-tools/adb.exe install -r app/build/outputs/apk/debug/app-debug.apk
```

- JDK `D:\drive\tools\jdk17`, SDK `D:\drive\tools\android-sdk`, Gradle `D:\drive\tools\gradle-9.8.2`
- `local.properties`의 `sdk.dir`은 **슬래시 경로**(`D:/drive/tools/android-sdk`)로 쓴다. 백슬래시는 `\t`가 탭으로 깨진다. (이 파일은 git에 올리지 않는다.)
- compileSdk는 37이다. 최신 Compose/Navigation 의존성이 요구한다.
- 의존성 버전은 `gradle/libs.versions.toml`에서 관리한다.

### 구조
| 위치 | 역할 |
|---|---|
| `data/` (패키지) | Room 엔티티·DAO, JSON 스키마, `DeckRepository`(가져오기/내보내기), `SettingsStore` |
| `domain/` | `Scheduler`(SM-2, 모름만 반복), `StudyQueue`(큐 구성) — 안드로이드 의존 없는 순수 Kotlin |
| `ui/` | 화면: home, study, cards, settings |
| `docs/알고리즘.md` | 복습 주기 근거와 출처 |
| `data/*.json` (프로젝트 루트의 `data\`) | 카드 JSON 파일 |

- 복습 로직을 바꾸면 `app/src/test/…/SchedulerTest.kt`를 같이 고치고 `docs/알고리즘.md`도 갱신한다.
- DB 스키마(엔티티)를 바꾸면 Room `version`을 올리고 마이그레이션을 만든다. 안 그러면 폰의 기존 진행 기록이 깨진다.

---

## 6. git·커밋

- 커밋 메시지는 한국어, 변경 단위별로 나눈다 (예: "카드 목록 화면", "SM-2 간격 수정", "Java 덱 JSON 추가").
- 카드 JSON을 새로 만들거나 고치면 코드와 따로 커밋한다.
- 커밋 끝에 공동 작성자 줄을 붙인다.
- 원격 저장소 푸시는 사용자가 확인한 뒤에만 한다 (private 저장소).
- `local.properties`, `build/`, `.gradle/`, `*.apk`는 올리지 않는다 (`.gitignore`에 있음).

---

## 7. 작업 후 기록

볼트의 `log.md`에 작업을 남긴다. 형식은 볼트 `CLAUDE.md` §10을 따른다.
```
## [YYYY-MM-DD] note | 카드 추출: <덱 이름> (N장)
- 생성: data/<덱>.json, 원본: [[노트 이름]]
```
