# BrockenreichLand

Paper 1.21.8 기반 마인크래프트 서버 플러그인 (Kotlin + Gradle).

## 요구 사항

- JDK 21
- VSCode + 권장 확장 (`.vscode/extensions.json` 참고: Kotlin, Gradle for Java, Extension Pack for Java)

## 빌드

```bash
./gradlew build
```

빌드된 플러그인 jar는 `build/libs/brockenreich-land-<version>.jar` 에 생성됩니다.

## 로컬 서버에서 테스트

1. `run/` 디렉터리를 만들고 Paper 1.21.8 서버 jar를 내려받아 넣습니다.
2. `run/eula.txt` 에 `eula=true` 를 작성합니다.
3. `run/plugins/` 에 빌드된 jar를 복사합니다.
4. 서버 jar를 실행합니다 (`java -jar paper-1.21.8-*.jar --nogui`).

`run/` 은 `.gitignore` 에 포함되어 있어 커밋되지 않습니다.
