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

## 로컬 서버에서 바로 실행하기

이 저장소에는 플러그인 소스만 있고, 실제로 구동할 Paper 서버 본체(jar)는 포함되어 있지 않습니다.
아래 스크립트로 최신 Paper 1.21.8 빌드를 받아 `run/` 에 로컬 테스트 서버를 구성할 수 있습니다.

```bash
# 1. Paper 1.21.8 서버 jar 다운로드 + run/ 구성 (Mojang EULA 동의 포함)
./scripts/setup-server.sh --accept-eula

# 2. 플러그인 빌드 후 run/plugins/ 에 자동 복사
./gradlew deployToRunServer

# 3. 서버 실행
./scripts/run-server.sh
```

- `--accept-eula` 없이 실행하면 `run/eula.txt` 가 `eula=false` 로 생성되며,
  https://aka.ms/MinecraftEULA 를 확인한 뒤 직접 `eula=true` 로 바꿔야 서버가 뜹니다.
- `run/` 은 `.gitignore` 에 포함되어 있어 서버 jar/월드 데이터가 커밋되지 않습니다.
- 서버를 종료하려면 콘솔에 `stop` 을 입력하세요.
