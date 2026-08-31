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

### Mac / Linux / Git Bash / WSL

```bash
# 1. Paper 1.21.8 서버 jar 다운로드 + run/ 구성 (Mojang EULA 동의 포함)
./scripts/setup-server.sh --accept-eula

# 2. 플러그인 빌드 후 run/plugins/ 에 자동 복사
./gradlew deployToRunServer

# 3. 서버 실행
./scripts/run-server.sh
```

### Windows (PowerShell)

`.sh` 스크립트는 PowerShell에서 실행되지 않으므로 `.ps1` 버전을 사용하세요.

```powershell
# 1. Paper 1.21.8 서버 jar 다운로드 + run/ 구성 (Mojang EULA 동의 포함)
.\scripts\setup-server.ps1 -AcceptEula

# 2. 플러그인 빌드 후 run/plugins/ 에 자동 복사
.\gradlew.bat deployToRunServer

# 3. 서버 실행
.\scripts\run-server.ps1
```

`실행할 수 없습니다. ... 이 시스템에서 스크립트를 실행할 수 없으므로...` 같은 실행 정책 오류가 나면,
아래 명령으로 현재 세션에서만 스크립트 실행을 허용한 뒤 다시 시도하세요.

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
```

### 공통 안내

- `--accept-eula` / `-AcceptEula` 없이 실행하면 `run/eula.txt` 가 `eula=false` 로 생성되며,
  https://aka.ms/MinecraftEULA 를 확인한 뒤 직접 `eula=true` 로 바꿔야 서버가 뜹니다.
- `run/` 은 `.gitignore` 에 포함되어 있어 서버 jar/월드 데이터가 커밋되지 않습니다.
- 서버를 종료하려면 콘솔에 `stop` 을 입력하세요.

## 구역(Area) 관리

WorldEdit(소프트 디펜던시)과 연동되는 땅 관리 기능입니다. `run/plugins/` 에 WorldEdit이 설치되어 있어야
`/area create` 로 새 구역을 만들 수 있습니다.

- **구역(region)**: WorldEdit으로 선택한 직육면체 영역에 이름을 붙인 것.
- **월드 기본 구역(world)**: 어떤 구역에도 속하지 않는, 월드의 나머지 모든 공간. `world:<월드이름>` 으로 지정.
- **멤버(member)**: 해당 구역에 항상 입장/퇴장 가능한 플레이어 목록.
- **비멤버 권한(entrance/exit)**: 멤버가 아닌 플레이어에게 입장(`entrance`)/퇴장(`exit`)을 허용할지 여부. 기본값은 둘 다 허용.

```
/area region create <이름>                             # 현재 WorldEdit 선택 영역으로 region:<이름> 생성
/area region delete <이름>
/area world create <월드이름>                           # world:<월드이름> 등록 (기본: entrance/exit 모두 허용)
/area world delete <월드이름>                           # world:<월드이름> 을 기본값으로 초기화
/area list
/area info <region:이름|world:월드이름>
/area modify <target> member add <닉네임>
/area modify <target> member remove <닉네임>
/area modify <target> role permission add <entrance|exit>
/area modify <target> role permission remove <entrance|exit>
```

예시:
```
/area region create green
/area modify region:green member add Steve
/area world create world
/area modify world:world role permission remove entrance
```
→ `green` 구역은 `Steve`가 항상 드나들 수 있고, `world` 월드에서 구역으로 지정되지 않은 나머지 공간은
비멤버(=world:world 의 멤버가 아닌 모든 플레이어)의 입장이 차단됩니다. 새로 등록되는 월드/구역은
entrance·exit 권한이 기본적으로 둘 다 허용된 상태로 시작합니다.

전부 관리자(OP) 전용 명령이며, 설정은 `plugins/BrockenreichLand/areas.yml` 에 저장됩니다.
