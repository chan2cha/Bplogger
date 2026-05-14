$ErrorActionPreference = "Stop"

function Invoke-Step {
    param(
        [Parameter(Mandatory = $true)]
        [string] $Name,
        [Parameter(Mandatory = $true)]
        [scriptblock] $Command
    )

    Write-Host ""
    Write-Host "==> $Name"
    & $Command
    if ($LASTEXITCODE -ne 0) {
        throw "$Name failed with exit code $LASTEXITCODE."
    }
}

Set-Location (Split-Path -Parent $PSScriptRoot)

$env:GRADLE_USER_HOME = Join-Path (Get-Location) ".gradle-user-home"
if (-not $env:ANDROID_HOME) {
    $defaultAndroidHome = Join-Path $env:LOCALAPPDATA "Android\Sdk"
    if (Test-Path $defaultAndroidHome) {
        $env:ANDROID_HOME = $defaultAndroidHome
    }
}
if (-not $env:JAVA_HOME) {
    $defaultJavaHome = Join-Path $env:ProgramFiles "Android\Android Studio\jbr"
    if (Test-Path $defaultJavaHome) {
        $env:JAVA_HOME = $defaultJavaHome
    }
}
if ($env:JAVA_HOME) {
    $env:Path = (Join-Path $env:JAVA_HOME "bin") + [IO.Path]::PathSeparator + $env:Path
}

Invoke-Step "Environment" {
    if (-not $env:JAVA_HOME) {
        throw "JAVA_HOME is not set. Set JAVA_HOME to a valid JDK before running checks."
    }

    $java = Get-Command java -ErrorAction SilentlyContinue
    if (-not $java) {
        throw "java was not found on PATH. Add JAVA_HOME\bin to PATH before running checks."
    }

    java -version

    if (-not $env:ANDROID_HOME) {
        throw "ANDROID_HOME is not set. Set ANDROID_HOME or create local.properties with sdk.dir."
    }

    if (-not (Test-Path $env:ANDROID_HOME)) {
        throw "ANDROID_HOME points to a missing directory: $env:ANDROID_HOME"
    }
}

Invoke-Step "Unit tests" {
    .\gradlew.bat test
}

Invoke-Step "Lint debug" {
    .\gradlew.bat lintDebug
}

Invoke-Step "Assemble debug" {
    .\gradlew.bat assembleDebug
}

Invoke-Step "Assemble debug Android tests" {
    .\gradlew.bat assembleDebugAndroidTest
}

if ($env:RUN_CONNECTED_TESTS -eq "1") {
    Invoke-Step "Connected debug Android tests" {
        .\gradlew.bat connectedDebugAndroidTest
    }
}

Write-Host ""
Write-Host "All local checks passed."
