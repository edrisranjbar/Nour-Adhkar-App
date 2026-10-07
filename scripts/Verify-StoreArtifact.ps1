param(
    [Parameter(Mandatory)][ValidateSet('bazaar', 'myket')][string]$Store,
    [Parameter(Mandatory)][string]$Artifact
)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$forbidden = if ($Store -eq 'myket') {
    @('bazaar://', 'cafebazaar.ir', 'com.farsitel.bazaar')
} else {
    @('myket://', 'myket.ir', 'ir.mservices.market')
}
$expected = if ($Store -eq 'myket') { 'myket://comment?id=ir.adhkar.app' } else { 'bazaar://details?id=ir.adhkar.app' }
$foundExpected = $false
$dexCount = 0
$zip = [System.IO.Compression.ZipFile]::OpenRead((Resolve-Path -LiteralPath $Artifact).Path)
try {
    foreach ($entry in $zip.Entries) {
        # Check code, resources, manifests and assets; skip large audio/font/image payloads.
        if ($entry.FullName -notmatch '(\.dex$|\.xml$|\.arsc$|\.pb$|\.json$|\.txt$|\.html$)') { continue }
        $stream = $entry.Open()
        $buffer = [System.IO.MemoryStream]::new()
        try {
            $stream.CopyTo($buffer)
            $bytes = $buffer.ToArray()
            $content = [System.Text.Encoding]::UTF8.GetString($bytes)
            $wideContent = [System.Text.Encoding]::Unicode.GetString($bytes)
            if ($entry.FullName -match '\.dex$') { $dexCount++ }
            if ($content.Contains($expected)) { $foundExpected = $true }
            foreach ($needle in $forbidden) {
                if ($content.Contains($needle) -or $wideContent.Contains($needle)) {
                    throw "Competing store reference '$needle' found in $($entry.FullName)."
                }
            }
        } finally { $stream.Dispose(); $buffer.Dispose() }
    }
    if ($dexCount -eq 0 -or !$foundExpected) { throw 'Missing DEX or expected store routing; refusing this artifact.' }
} finally { $zip.Dispose() }
Write-Output "Verified $Store routing in $Artifact"
