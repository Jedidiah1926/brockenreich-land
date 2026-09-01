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
- **멤버(member)**: 해당 구역에서 모든 권한을 항상 허용받는 플레이어 목록.
- **admin("땅 주인")**: 서버 OP가 아니어도, 그 구역 하나에 한해 `/area modify`/`/area info` 를 쓸 수 있는 플레이어 목록. member/권한과는 별개로, "이 명령을 쓸 수 있는가"에 대한 권한입니다. admin을 임명/해제하는 것 자체는 OP만 가능합니다 (admin이 스스로를 승격시키거나 다른 admin을 마음대로 못 늘리도록).
- **parent(부모 구역, admin 상속)**: 구역(region)에만 설정 가능. 어떤 구역에 부모 구역들을 지정하면, **모든 부모 구역에서 admin인 플레이어만** 자식 구역도 자동으로 admin이 됩니다 (AND 조건 — 부모가 여러 개면 전부 만족해야 함). 부모 중 하나에서라도 admin 자격을 잃으면 자식의 상속된 admin 자격도 즉시 같이 빠집니다. 순환 참조(자기 자신이 조상이 되는 구조)는 만들 수 없고, 설정도 OP 전용입니다.
- **`@everyone` 권한**: 멤버가 아닌 모든 플레이어에게 적용되는 기본 권한. 새 구역은 `administration` 을 제외한 모든 권한이 기본 허용 상태로 시작합니다.
- **개별 유저 권한**: 특정 닉네임에게 `@everyone` 설정과 별개로 추가로 권한을 허용할 수 있음 (`@everyone`이 막혀 있어도 개별 허용된 유저는 통과 가능 — 합집합 방식, 뺏는 방향으로는 동작하지 않음).

### 권한 목록

| 권한 | 설명 |
|---|---|
| `entrance` | 걸어서(또는 탈것 없이) 구역에 들어갈 수 있는가 |
| `exit` | 걸어서(또는 탈것 없이) 구역에서 나갈 수 있는가 |
| `boatEntrance` | 보트를 탄 채로 구역에 들어갈 수 있는가 (`entrance` 와 별개) |
| `boatExit` | 보트를 탄 채로 구역에서 나갈 수 있는가 (`exit` 와 별개) |

보트가 경계선에 걸쳐있는 상태에서 내리면, 내린 자리 기준으로 걸어서 들어갈 수 있는 권한(`entrance`)이 없으면 내리는 것 자체가 막힙니다 (보트 이동 검사만으로는 못 잡는 경계 우회 방지).

`entrance`/`exit`는 걸어서 이동하는 것뿐 아니라 엔더 진주 투척 및 코러스 열매 섭취로 인한 순간이동, 침대 사용(취침 위치가 경계 너머일 때)에도 동일하게 적용됩니다 (나가는 쪽 `exit`, 들어가는 쪽 `entrance` 모두 필요). 피스톤에 밀려서 경계를 넘는 경우처럼 이동 이벤트 자체가 발생하지 않는 케이스는 매 틱 위치를 검사해 허용되지 않은 위치면 직전의 안전한 위치로 되돌리는 별도의 보정 로직(AreaPlayerGuard)으로 막습니다.
| `interaction` | 상호작용 (버튼, 상자 등 우클릭 + 주민 거래, 동물 먹이주기, 아이템 액자 회전, 보트 탑승, 갑옷 거치대에 장비 넣고 빼기 등 엔티티 우클릭). 대상(블록/엔티티)의 위치 기준으로 검사하므로, 경계 바깥에서 사거리로 안쪽의 블록/엔티티를 상호작용하는 것도, 반대로 권한 없는 바깥 갑옷 거치대의 아이템을 권한 있는 안쪽에서 사거리로 빼가는 것도 막힙니다. 활공 중이 아닐 때 폭죽 로켓을 쏘는 것도 여기 포함 |
| `pickupItem` | 아이템 줍기 |
| `dropItem` | 아이템 던지기 |
| `blockBreak` | 블록 부수기 |
| `blockPlace` | 블록 설치하기 |
| `blockIgniting` | 블록에 불 붙이기 |
| `hangingPlace` | 액자/그림 등 걸이형 블록 설치 |
| `hangingBreak` | 액자/그림 등 걸이형 블록 파괴 |
| `projectileLaunch` | 발사체 발사 (활, 눈덩이 등). 엘리트라로 활공 중에 폭죽 로켓으로 부스트하는 것도 여기 포함 |
| `attackEntity` | 엔티티(몹 등) 공격. 공격자 위치와 피격자 위치 둘 다 검사하므로, 휩쓸기 공격으로 경계 너머의 엔티티를 때리는 것도 막힙니다 |
| `attackPlayer` | 플레이어 공격. `attackEntity`와 동일하게 공격자/피격자 양쪽 위치를 검사합니다 |
| `bucketEmpty` | 양동이 비우기 |
| `bucketFill` | 양동이 채우기 |
| `administration` | 이 구역 안에서 다른 모든 권한 검사를 무시하는 와일드카드 권한 (기본값: 비허용) |

### 보호(Protection) 목록

권한이 "플레이어가 뭘 할 수 있는가"라면, 보호는 "기계 장치가 구역 경계를 넘을 수 있는가"를 막는 것으로, 기본값은 비활성(off)입니다.

| 보호 | 설명 |
|---|---|
| `piston` | 피스톤(스티키 피스톤 + 슬라임/꿀 블록으로 옆으로 끌려가는 블록 포함)이 이 구역의 경계 안팎으로 블록을 밀거나 당기지 못하게 막음 |
| `flood` | 이 구역 안에서 시작된 물/용암이 구역 경계 밖으로 퍼지지 못하게 막음 (외부에서 안으로 흘러들어오는 건 막지 않음) |
| `potion` | 투척용 포션이 이 구역 안에서 깨지면 파티클을 포함해 스플래시 자체가 통째로 취소됨. 구역 밖에서 깨진 경우엔 그 스플래시는 그대로 나되, 범위 안에서 이 구역에 있는 대상만 효과가 제외됨. 잔류형 포션도 이 구역 안에서 깨지면 잔류 구름 자체가 생기지 않고, 구역 밖에서 생긴 구름이 시간이 지나며 범위가 커져 안쪽까지 걸쳐도 그 순간 안에 있는 대상에게는 효과가 적용되지 않음 |
| `explosion` | TNT, 엔드 수정, 크리퍼, 위더, 침대/속박의 닻 등 모든 폭발로부터 이 구역 안의 블록이 파괴되지 않게 막음. 터지는 개체(TNT 등) 자체가 이 구역 안에 있으면 소리·파티클까지 포함해 폭발이 아예 통째로 취소됨. 개체는 밖에 있는데 폭발 범위만 구역 안까지 걸치는 경우엔, 이미 밖에서 소리·파티클이 발생한 뒤라 그건 그대로 나고 블록 파괴만 안쪽이 보호됨 |
| `explosionDamage` | 위 폭발들로 인해 이 구역 안에 있는 엔티티/플레이어가 데미지를 받지 않게 막음 (`explosion` 과 별개로 켜고 끌 수 있음) |
| `overflow` | 나무/거대버섯 등 구조물이 자랄 때, 묘목이 심어진 곳이 이 구역 안이면 나뭇잎 등 생성되는 블록이 구역 경계 밖으로 나가지 못하게 막음. 잔디에 뼛가루를 사용해 풀/꽃이 퍼지는 경우도 동일하게, 뼛가루를 사용한 블록이 이 구역 안이면 그 결과 블록이 경계 밖으로 나가지 못하게 막음 |
| `redstone` | 이 구역 안의 레드스톤 전선이 구역 경계와 맞닿아 있으면(이웃 블록이 다른 구역에 속하면) 신호 변화가 전달되지 않게 막음 — 밖에서 온 신호가 안으로 못 들어오고, 안에서 만든 신호도 밖으로 못 나감 |
| `fire` | 이 구역 안에서 불이 붙지 못하게 막음 (부싯돌, 번개, 용암, 화염구 등 확산이 아닌 모든 발화 원인) |
| `fireSpread` | 이 구역 안에서 불이 스스로 번지지 못하게 막음 (인접 블록으로 불이 옮겨붙는 것 + 불에 타서 블록이 사라지는 것 둘 다) |
| `dispenser` | 발사기가 바라보는 방향 바로 앞 칸이 이 구역과 다른 구역이면 발사(화살/포션/불꽃 등)나 양동이 액체 분출을 막음. 안쪽을 향해 쏘는 것과 안쪽에서 안쪽으로 쏘는 것은 그대로 정상 작동, 경계를 넘나드는 경우만 막힘 (경계 안쪽/바깥쪽 중 어느 쪽에 이 보호가 있어도 적용됨) |
| `sponge` | 스폰지가 이 구역 안에 있으면, 경계 밖에 있는 물은 빨아들이지 못하게 막음. 같은 구역 안의 물은 정상적으로 흡수됨 |

```
/area region create <이름>                             # 현재 WorldEdit 선택 영역으로 region:<이름> 생성
/area region delete <이름>
/area world create <월드이름>                           # world:<월드이름> 등록 (기본: administration 제외 모든 권한 허용)
/area world delete <월드이름>                           # world:<월드이름> 을 기본값으로 초기화
/area list
/area info <region:이름|world:월드이름>
/area modify <target> role add member <닉네임>
/area modify <target> role remove member <닉네임>
/area modify <target> role add admin <닉네임>          # OP 전용 - 이 구역만 관리할 수 있는 admin 임명
/area modify <target> role remove admin <닉네임>       # OP 전용
/area modify region:<이름> parent add <부모구역이름>    # OP 전용 - 모든 부모의 admin이면 자동 admin
/area modify region:<이름> parent remove <부모구역이름> # OP 전용
/area modify <target> role permission @everyone <add|remove> <권한>
/area modify <target> role permission <닉네임> <add|remove> <권한>
/area modify <target> protection <add|remove> <보호>
```

`member`/`admin`/`permission`은 전부 `role` 아래의 하위 종류입니다 — member도 admin도 결국 "역할"이라는 개념이고,
그 역할에 누구를 넣을지(member/admin) 또는 어떤 권한을 부여할지(permission)의 차이일 뿐입니다.

`region`/`world` 생성·삭제와 `list`, admin 임명/해제, parent 설정은 OP만 가능합니다. `modify`(role member/permission,
protection)와 `info`는 OP 이거나, 그 구역의 admin으로 등록된(또는 parent를 통해 상속받은) 플레이어면 자기 구역에
한해 사용할 수 있습니다.

parent 예시:
```
/area modify region:lime parent add white
/area modify region:lime parent add green
```
→ `white`와 `green` 양쪽 모두에서 admin인 플레이어만 `lime`도 자동으로 admin이 됩니다. 이후 그 플레이어가
`green`의 admin에서 제거되면, `lime`에 대한 admin 자격도 (별도 명령 없이) 즉시 같이 사라집니다.

역할(`@everyone`)은 항상 `@` 접두사로 지정하고, 특정 유저는 접두사 없이 닉네임을 그대로 씁니다.

예시:
```
/area region create green
/area modify region:green role add member Steve
/area world create world
/area modify world:world role permission @everyone remove entrance
/area modify world:world role permission Alex add entrance
/area modify region:green role permission @everyone remove blockBreak
```
→ `green` 구역은 `Steve`가 항상 모든 걸 할 수 있고, `world` 월드에서 구역으로 지정되지 않은 나머지 공간은
`@everyone`(월드의 멤버가 아닌 모든 플레이어)의 입장이 차단되지만, `Alex`는 개별로 입장이 허용되어
`@everyone` 설정과 무관하게 들어갈 수 있습니다. `green` 구역에서는 `@everyone`(멤버 제외)의 블록 파괴가 막힙니다.

전부 관리자(OP) 전용 명령이며, 설정은 `plugins/BrockenreichLand/areas.yml` 에 저장됩니다.
