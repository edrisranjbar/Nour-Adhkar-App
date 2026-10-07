param(
    [ValidateSet('bazaar', 'myket', 'both')][string]$Store = 'both',
    [Parameter(Mandatory)][string]$ChangelogFa,
    [Parameter(Mandatory)][string]$ChangelogEn,
    [string]$JavaHome = 'C:\Program Files\Android\Android Studio\jbr',
    [string]$SdkBuildTools = "$env:LOCALAPPDATA\Android\Sdk\build-tools\36.0.0",
    [string]$BundleSigner = '.tooling/bundlesigner-0.1.13.jar'
)
$ErrorActionPreference = 'Stop'
$repo = Split-Path -Parent $PSScriptRoot
$faPath = (Resolve-Path -LiteralPath $ChangelogFa).Path
$enPath = (Resolve-Path -LiteralPath $ChangelogEn).Path
foreach ($file in @((Join-Path $JavaHome 'bin/java.exe'), (Join-Path $SdkBuildTools 'aapt2.exe'),
    (Join-Path $SdkBuildTools 'apksigner.bat'))) {
    if (!(Test-Path -LiteralPath $file)) { throw "Required tool missing: $file" }
}
$stores = if ($Store -eq 'both') { @('bazaar', 'myket') } else { @($Store) }
Push-Location $repo
try {
    & "$PSScriptRoot/Test-StoreIsolation.ps1"
    $gradle = Get-Content -LiteralPath 'app/build.gradle.kts' -Raw
    $version = [regex]::Match($gradle, 'versionName\s*=\s*"([^"]+)"').Groups[1].Value
    $code = [regex]::Match($gradle, 'versionCode\s*=\s*(\d+)').Groups[1].Value
    if (!$version -or !$code) { throw 'Cannot resolve release version.' }
    $stamp = Get-Date -Format 'yyyyMMdd-HHmmss-fff'
    if ($stores -contains 'bazaar') {
        $BundleSigner = (Resolve-Path -LiteralPath $BundleSigner).Path
    }
    $previousJava = $env:JAVA_HOME
    $previousGradle = $env:GRADLE_USER_HOME
    $previousAapt = ${env:ORG_GRADLE_PROJECT_android.aapt2FromMavenOverride}
    $env:JAVA_HOME = $JavaHome
    $env:GRADLE_USER_HOME = Join-Path $env:USERPROFILE '.gradle'
    ${env:ORG_GRADLE_PROJECT_android.aapt2FromMavenOverride} = Join-Path $SdkBuildTools 'aapt2.exe'
    $identities = @()
    try {
        foreach ($channel in $stores) {
            $variant = (Get-Culture).TextInfo.ToTitleCase($channel)
            $directory = Join-Path $repo "release/$channel-$version-vc$code/$stamp.d"
            New-Item -ItemType Directory -Path $directory | Out-Null
            & ./gradlew.bat "assemble${variant}Release" "bundle${variant}Release" "test${variant}DebugUnitTest" --project-cache-dir .gradle-card-design --no-configuration-cache *> (Join-Path $directory 'gradle.log')
            if ($LASTEXITCODE -ne 0) { throw "Gradle failed. See $directory/gradle.log" }
            $baseName = "nour-adhkar-$channel-$version-vc$code"
            $apk = Join-Path $directory "$baseName.apk"
            $aab = Join-Path $directory "$baseName.aab"
            Copy-Item -LiteralPath "app/build/outputs/apk/$channel/release/app-$channel-release.apk" -Destination $apk
            Copy-Item -LiteralPath "app/build/outputs/bundle/${channel}Release/app-$channel-release.aab" -Destination $aab
            Copy-Item -LiteralPath $faPath -Destination (Join-Path $directory 'changelog-fa.md')
            Copy-Item -LiteralPath $enPath -Destination (Join-Path $directory 'changelog-en.md')
            & "$PSScriptRoot/Verify-StoreArtifact.ps1" -Store $channel -Artifact $apk
            & "$PSScriptRoot/Verify-StoreArtifact.ps1" -Store $channel -Artifact $aab
            $badging = & (Join-Path $SdkBuildTools 'aapt2.exe') dump badging $apk
            if ($LASTEXITCODE -ne 0 -or ($badging -join "`n") -notmatch "package: name='ir\.adhkar\.app' versionCode='$code' versionName='$([regex]::Escape($version))'") {
                throw 'APK package or version does not match the intended release.'
            }
            $certificate = & (Join-Path $SdkBuildTools 'apksigner.bat') verify --print-certs $apk
            if ($LASTEXITCODE -ne 0) { throw 'APK signature verification failed.' }
            $digest = ($certificate | Select-String 'Signer #1 certificate SHA-256 digest:').ToString()
            if (!$digest) { throw 'No signing certificate digest.' }
            $identities += $digest
            $certificate | Set-Content -LiteralPath (Join-Path $directory 'signature.txt') -Encoding utf8
            if ($channel -eq 'bazaar') {
                $properties = @{}
                if (Test-Path -LiteralPath 'release-signing.properties') {
                    Get-Content -LiteralPath 'release-signing.properties' | ForEach-Object {
                        if ($_ -match '^\s*([^#=]+)=(.*)$') { $properties[$matches[1].Trim()] = $matches[2].Trim() }
                    }
                }
                $keyPath = if ($env:KEYSTORE_PATH) { $env:KEYSTORE_PATH } else { $properties['storeFile'] }
                $alias = if ($env:KEY_ALIAS) { $env:KEY_ALIAS } else { $properties['keyAlias'] }
                $previousStorePassword = $env:NOUR_BIN_STORE_PASSWORD
                $previousKeyPassword = $env:NOUR_BIN_KEY_PASSWORD
                try {
                    $env:NOUR_BIN_STORE_PASSWORD = if ($env:STORE_PASSWORD) { $env:STORE_PASSWORD } else { $properties['storePassword'] }
                    $env:NOUR_BIN_KEY_PASSWORD = if ($env:KEY_PASSWORD) { $env:KEY_PASSWORD } else { $properties['keyPassword'] }
                    if (!$keyPath -or !$alias -or !$env:NOUR_BIN_STORE_PASSWORD -or !$env:NOUR_BIN_KEY_PASSWORD) { throw 'BIN signing credentials are incomplete.' }
                    & (Join-Path $JavaHome 'bin/java.exe') -jar $BundleSigner genbin --bundle $aab --bin $directory --ks $keyPath --ks-key-alias $alias --ks-pass env:NOUR_BIN_STORE_PASSWORD --key-pass env:NOUR_BIN_KEY_PASSWORD --v2-signing-enabled true --v3-signing-enabled false *> (Join-Path $directory 'bundlesigner.log')
                    if ($LASTEXITCODE -ne 0 -or !(Get-ChildItem -LiteralPath $directory -Filter '*.bin')) { throw 'BIN generation failed.' }
                } finally {
                    $env:NOUR_BIN_STORE_PASSWORD = $previousStorePassword
                    $env:NOUR_BIN_KEY_PASSWORD = $previousKeyPassword
                }
            }
            Get-ChildItem -LiteralPath $directory -File | Where-Object { $_.Extension -in '.apk', '.aab', '.bin', '.md' } |
                Get-FileHash -Algorithm SHA256 | ForEach-Object { "$($_.Hash)  $(Split-Path -Leaf $_.Path)" } |
                Set-Content -LiteralPath (Join-Path $directory 'SHA256SUMS.txt') -Encoding utf8
            Write-Output "Prepared $channel release: $directory"
        }
        if (@($identities | Select-Object -Unique).Count -ne 1) { throw 'Store APKs have different signing certificates.' }
    } finally {
        $env:JAVA_HOME = $previousJava
        $env:GRADLE_USER_HOME = $previousGradle
        ${env:ORG_GRADLE_PROJECT_android.aapt2FromMavenOverride} = $previousAapt
    }
} finally { Pop-Location }
