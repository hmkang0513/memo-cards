# 암기카드 (memo-cards)

옵시디언 볼트에서 Claude가 추출한 암기 카드를 JSON으로 가져와, 폰 로컬(Room)에서 암기하는 개인용 안드로이드 앱.

- 스택: Kotlin, Jetpack Compose, Room
- 복습 방식: SM-2 간격 반복 / 모름만 반복 모드
- 카드 데이터: JSON 파일 가져오기 (`data/` 참고)

## 빌드
```
./gradlew assembleDebug
```
JDK 17, Android SDK(`ANDROID_HOME`)가 필요하다. 도구는 `D:\drive\tools`에 설치해 사용한다.
