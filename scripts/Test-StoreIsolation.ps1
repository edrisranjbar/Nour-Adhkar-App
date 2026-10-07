# Source and release-tool checks; does not invoke Gradle or run the Android app.
$ErrorActionPreference = 'Stop'
$repo = Split-Path -Parent $PSScriptRoot
Add-Type -AssemblyName System.IO.Compression.FileSystem
foreach ($channel in @('bazaar', 'myket')) {
    $pattern = if ($channel -eq 'myket') { 'bazaar://|cafebazaar\.ir|com\.farsitel\.bazaar' } else { 'myket://|myket\.ir|ir\.mservices\.market' }
    foreach ($source in @('main', $channel)) {
        Get-ChildItem -LiteralPath (Join-Path $repo "app/src/$source") -Recurse -File |
            Where-Object { $_.Extension -in '.kt', '.xml', '.json', '.txt', '.html' } |
            ForEach-Object {
                if ((Get-Content -LiteralPath $_.FullName -Raw) -match $pattern) {
                    throw "Competing store reference in $channel source set: $($_.FullName)"
                }
            }
    }
    $config = Join-Path $repo "app/src/$channel/java/com/example/store/StoreConfig.kt"
    if (!(Test-Path -LiteralPath $config)) { throw "Missing $channel source-set configuration." }
    Write-Output "$channel source isolation OK"
}
$fixtureDirectory = Join-Path ([System.IO.Path]::GetTempPath()) "nour-store-fixtures-$([guid]::NewGuid())"
New-Item -ItemType Directory -Path $fixtureDirectory | Out-Null
function New-ArtifactFixture([string]$Name, [string]$Dex, [string]$Xml = '') {
    $path = Join-Path $fixtureDirectory "$Name.apk"
    $zip = [System.IO.Compression.ZipFile]::Open($path, [System.IO.Compression.ZipArchiveMode]::Create)
    try {
        if ($Dex) {
            $entry = $zip.CreateEntry('classes.dex')
            $writer = [System.IO.StreamWriter]::new($entry.Open(), [System.Text.Encoding]::UTF8)
            try { $writer.Write($Dex) } finally { $writer.Dispose() }
        }
        $entry = $zip.CreateEntry('AndroidManifest.xml')
        $writer = [System.IO.StreamWriter]::new($entry.Open(), [System.Text.Encoding]::Unicode)
        try { $writer.Write($Xml) } finally { $writer.Dispose() }
    } finally { $zip.Dispose() }
    return $path
}
function Assert-Rejected([string]$Channel, [string]$Path) {
    $rejected = $false
    try { & "$PSScriptRoot/Verify-StoreArtifact.ps1" -Store $Channel -Artifact $Path }
    catch { $rejected = $true }
    if (!$rejected) { throw "Scanner incorrectly accepted fixture: $Path" }
}
try {
    & "$PSScriptRoot/Verify-StoreArtifact.ps1" -Store myket -Artifact (New-ArtifactFixture 'myket-valid' 'myket://comment?id=ir.adhkar.app')
    & "$PSScriptRoot/Verify-StoreArtifact.ps1" -Store bazaar -Artifact (New-ArtifactFixture 'bazaar-valid' 'bazaar://details?id=ir.adhkar.app')
    Assert-Rejected myket (New-ArtifactFixture 'myket-cross-dex' 'myket://comment?id=ir.adhkar.app bazaar://details')
    Assert-Rejected myket (New-ArtifactFixture 'myket-cross-resource' 'myket://comment?id=ir.adhkar.app' 'https://cafebazaar.ir/app/ir.adhkar.app')
    Assert-Rejected bazaar (New-ArtifactFixture 'bazaar-cross-resource' 'bazaar://details?id=ir.adhkar.app' 'ir.mservices.market')
    Assert-Rejected myket (New-ArtifactFixture 'myket-no-routing' 'some unrelated DEX strings')
    Assert-Rejected myket (New-ArtifactFixture 'myket-no-dex' '' 'myket://comment?id=ir.adhkar.app')
    Write-Output 'Artifact scanner fixtures OK: 2 accepted, 5 rejected as expected.'
} finally {
    Get-ChildItem -LiteralPath $fixtureDirectory -File | ForEach-Object { Remove-Item -LiteralPath $_.FullName }
    Remove-Item -LiteralPath $fixtureDirectory
}
