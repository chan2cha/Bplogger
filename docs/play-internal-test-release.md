# Play 내부 테스트 배포 절차

## 1. 목적

Google Play Console 내부 테스트 트랙으로 Pulse Log를 제한된 테스터에게 배포하기 위한 절차다.

## 2. 현재 준비 상태

- 앱 표시명: `Pulse Log`
- 현재 `applicationId`: `com.pulselog`
- release 서명 설정: `keystore.properties`가 있으면 `bundleRelease`에 자동 적용
- 업로드 산출물: `app/build/outputs/bundle/release/app-release.aab`
- 개인정보처리방침 문서: `docs/privacy-policy.md`

주의:

- Play Console에 첫 빌드를 업로드하면 패키지명은 바꿀 수 없다.
- 현재 패키지명은 앱명에 맞춰 `com.pulselog`로 정리했다.
- Play Console에 첫 빌드를 업로드하면 이 패키지명은 고정된다.

## 3. 업로드 키 생성

Android Studio 또는 JDK `keytool`로 upload key를 만든다.

예시:

```powershell
keytool -genkeypair -v -keystore release-upload-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias pulse-log-upload
```

생성 후 루트에 `keystore.properties`를 만든다.

```properties
storeFile=release-upload-key.jks
storePassword=실제_비밀번호
keyAlias=pulse-log-upload
keyPassword=실제_비밀번호
```

보안:

- `keystore.properties`, `*.jks`, `*.keystore`는 git에 올리지 않는다.
- upload key와 비밀번호를 분실하면 이후 업데이트 빌드를 만들 수 없으므로 별도 보관한다.

## 4. release AAB 빌드

PowerShell:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat bundleRelease
```

결과 파일:

```text
app/build/outputs/bundle/release/app-release.aab
```

## 5. Play Console 내부 테스트 업로드

1. Play Console에서 새 앱을 생성한다.
2. 앱 이름은 `Pulse Log`로 설정한다.
3. 내부 테스트 트랙으로 이동한다.
4. 테스터 이메일 목록을 만든다.
5. 새 release를 만들고 `app-release.aab`를 업로드한다.
6. release note를 입력한다.
7. 검토 후 내부 테스트 트랙에 출시한다.
8. 테스터에게 opt-in 링크를 공유한다.

개인정보처리방침 URL:

- GitHub에 `docs/privacy-policy.md`를 올린 뒤 Raw 또는 GitHub Pages URL을 Play Console 개인정보처리방침 항목에 입력한다.
- GitHub Pages를 사용할 경우 예시는 `https://<계정명>.github.io/<저장소명>/docs/privacy-policy.html` 형식이다.

공식 문서 기준:

- 내부 테스트는 최대 100명 테스터에게 빠르게 배포할 수 있다.
- 첫 앱은 내부 테스터에게 빠르게 제공될 수 있지만, 임시 이름/스토어 정보가 최대 48시간 보일 수 있다.
- 테스터는 Google 계정 이메일로 등록되어야 한다.

## 6. 다음 릴리즈 규칙

- Play에 이미 업로드한 뒤에는 `versionCode`를 반드시 증가시킨다.
- `versionName`은 사용자가 볼 버전 문자열이다.
- 내부 테스트라도 패키지명은 처음 업로드한 뒤 변경할 수 없다.
