<#
.SYNOPSIS
Builds the signed release APK for one store flavor and installs it on the connected phone as an
update of the existing «اذکار نور» (ir.adhkar.app), preserving data, then launches it.

.EXAMPLE
.\scripts\Run-Phone.ps1
.\scripts\Run-Phone.ps1 -Store myket -Serial 2b945d16
#>
param(
    [ValidateSet('bazaar', 'myket')][string]$Store = 'bazaar',
    # Device serial from `adb devices -l`; optional when exactly one device is connected.
    [string]$Serial,
    [string]$JavaHome = 'C:\Program Files\Android\Android Studio\jbr',
    [string]$SdkRoot = "$env:LOCALAPPDATA\Android\Sdk",
    [string]$BuildToolsVersion = '36.0.0',
    # Defaults to the workstation's ignored repository init scripts when they exist.
    [string[]]$GradleInitScripts,
    [switch]$NoInitScripts,
    # Documented local fallback for release lint's desktop-dependency failure; never counts as lint passing.
    [switch]$SkipLint
)
$ErrorActionPreference = 'Stop'
$repo = Split-Path -Parent $PSScriptRoot
$package = 'ir.adhkar.app'
$activity = "$package/com.example.MainActivity"
$buildTools = Join-Path $SdkRoot "build-tools/$BuildToolsVersion"
$adb = Join-Path $SdkRoot 'platform-tools/adb.exe'
$aapt2 = Join-Path $buildTools 'aapt2.exe'
$apksigner = Join-Path $buildTools 'apksigner.bat'
foreach ($file in @((Join-Path $JavaHome 'bin/java.exe'), $adb, $aapt2, $apksigner)) {
    if (!(Test-Path -LiteralPath $file)) { throw "Required tool missing: $file" }
}

function Step($text) { Write-Host "==> $text" -ForegroundColor Cyan }

# Windows PowerShell 5.1 turns a native tool's redirected stderr into errors, which 'Stop' would
# treat as fatal even when the tool succeeds. Success is judged by $LASTEXITCODE / output instead.
function Invoke-Native([scriptblock]$Command) {
    $previous = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try { & $Command } finally { $ErrorActionPreference = $previous }
}

# Pick the device before the multi-minute build so a missing phone fails fast.
Step 'Looking for the phone'
$devices = @(Invoke-Native { & $adb devices } | Select-Object -Skip 1 | ForEach-Object {
    if ("$_" -match '^(\S+)\s+(\S+)$') { [pscustomobject]@{ Serial = $matches[1]; State = $matches[2] } }
})
if ($Serial) {
    $device = $devices | Where-Object Serial -eq $Serial
    if (!$device) { throw "Device $Serial is not connected. Connected: $(($devices.Serial) -join ', ')" }
} else {
    $ready = @($devices | Where-Object State -eq 'device')
    if ($ready.Count -ne 1) {
        & $adb devices -l
        throw "Expected exactly one ready device, found $($ready.Count). Pass -Serial."
    }
    $device = $ready[0]
}
if ($device.State -ne 'device') {
    throw "Device $($device.Serial) is '$($device.State)'. Unlock the phone and accept the USB debugging prompt."
}
$Serial = $device.Serial
Write-Host "Using $Serial ($(& $adb -s $Serial shell getprop ro.product.model))"

Push-Location $repo
$previousJava = $env:JAVA_HOME
$previousGradle = $env:GRADLE_USER_HOME
$previousAapt = ${env:ORG_GRADLE_PROJECT_android.aapt2FromMavenOverride}
try {
    if (!$NoInitScripts -and !$GradleInitScripts) {
        $GradleInitScripts = @('.tooling/release-local.init.gradle', '.tooling/release-mirror.init.gradle') |
            Where-Object { Test-Path -LiteralPath $_ }
    }
    $gradleArgs = @()
    if (!$NoInitScripts) {
        foreach ($initScript in $GradleInitScripts) {
            $gradleArgs += '--init-script'
            $gradleArgs += (Resolve-Path -LiteralPath $initScript).Path
        }
    }
    if ($SkipLint) {
        $gradleArgs += @('-x', 'lintVitalAnalyzeRelease', '-x', 'lintVitalReportRelease', '-x', 'lintVitalRelease')
        Write-Warning 'Release lint is skipped for this local build; it has not passed.'
    }

    $env:JAVA_HOME = $JavaHome
    $env:GRADLE_USER_HOME = Join-Path $env:USERPROFILE '.gradle'
    ${env:ORG_GRADLE_PROJECT_android.aapt2FromMavenOverride} = $aapt2

    $variant = (Get-Culture).TextInfo.ToTitleCase($Store)
    $logDirectory = Join-Path $repo 'app/build/run-phone'
    New-Item -ItemType Directory -Force -Path $logDirectory | Out-Null
    $log = Join-Path $logDirectory 'gradle.log'
    Step "Building assemble${variant}Release (log: $log)"
    $started = Get-Date
    Invoke-Native { & ./gradlew.bat "assemble${variant}Release" @gradleArgs --project-cache-dir .gradle-card-design --no-configuration-cache *> $log }
    # Judge success by Gradle's exit code; KSP can print an AWT exception on successful builds.
    if ($LASTEXITCODE -ne 0) {
        Get-Content -LiteralPath $log | Select-String -Pattern '^e: |What went wrong|FAILED|error:' | Select-Object -First 20 | ForEach-Object { Write-Host $_ -ForegroundColor Red }
        throw "Gradle failed. See $log"
    }
    Write-Host "Built in $([int]((Get-Date) - $started).TotalSeconds)s"

    $apk = Join-Path $repo "app/build/outputs/apk/$Store/release/app-$Store-release.apk"
    if (!(Test-Path -LiteralPath $apk) -or (Get-Item -LiteralPath $apk).LastWriteTime -lt $started) {
        throw "No fresh APK at $apk"
    }

    Step 'Checking package and signature'
    $badging = (& $aapt2 dump badging $apk) -join "`n"
    if ($badging -notmatch "package: name='$([regex]::Escape($package))' versionCode='(\d+)' versionName='([^']+)'") {
        throw "APK is not $package."
    }
    $versionCode = $matches[1]
    $versionName = $matches[2]
    & $apksigner verify $apk | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'APK signature verification failed.' }
    Write-Host "$package $versionName ($versionCode)"

    Step "Installing on $Serial (data preserved)"
    $install = (Invoke-Native { & $adb -s $Serial install -r $apk 2>&1 }) -join "`n"
    Write-Host $install
    if ($install -notmatch 'Success') {
        # Never uninstall, clear data or downgrade here; the user decides.
        throw "Installation failed. If the phone shows a prompt, approve it and rerun. Android said: $install"
    }

    Step 'Launching'
    & $adb -s $Serial shell am start -n $activity | Out-Null
    Start-Sleep -Seconds 2
    $installed = (& $adb -s $Serial shell dumpsys package $package) -join "`n"
    $installedVersion = [regex]::Match($installed, 'versionName=(\S+)').Groups[1].Value
    $updated = [regex]::Match($installed, 'lastUpdateTime=([^\r\n]+)').Groups[1].Value
    $focus = (& $adb -s $Serial shell dumpsys activity activities) -join "`n"
    $resumed = [regex]::Match($focus, 'topResumedActivity=[^\r\n]*').Value
    Write-Host "Installed version: $installedVersion, updated: $updated"
    if ($resumed -match [regex]::Escape($package)) {
        Write-Host "Running: $resumed" -ForegroundColor Green
    } else {
        Write-Warning "The app is not in the foreground (is the phone locked?). $resumed"
    }
} finally {
    $env:JAVA_HOME = $previousJava
    $env:GRADLE_USER_HOME = $previousGradle
    ${env:ORG_GRADLE_PROJECT_android.aapt2FromMavenOverride} = $previousAapt
    Pop-Location
}
