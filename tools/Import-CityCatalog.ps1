param(
    [string]$CitiesZip = '.tooling/cities15000.zip',
    [string]$AdminCodes = '.tooling/admin1CodesASCII.txt',
    [string]$Output = 'app/src/main/assets/cities.tsv'
)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem
function Normalize-CityName([string]$value) {
    $text = $value.Normalize([System.Text.NormalizationForm]::FormD)
    $text = [regex]::Replace($text, '\p{M}', '')
    return $text.Replace('ي', 'ی').Replace('ى', 'ی').Replace('ك', 'ک').Replace([string][char]0x200c, '').Trim().ToLowerInvariant()
}
$regions = @{}
Get-Content -LiteralPath $AdminCodes -Encoding utf8 | ForEach-Object {
    $fields = $_ -split "`t"
    if ($fields.Count -ge 2) { $regions[$fields[0]] = $fields[1] }
}
$archive = [System.IO.Compression.ZipFile]::OpenRead((Resolve-Path -LiteralPath $CitiesZip))
try {
    $reader = [System.IO.StreamReader]::new($archive.GetEntry('cities15000.txt').Open())
    try {
        $rows = [System.Collections.Generic.List[string]]::new()
        while (($line = $reader.ReadLine()) -ne $null) {
            $fields = $line -split "`t"
            if ($fields.Count -lt 19 -or $fields[8] -notmatch '^[A-Z]{2}$') { continue }
            $aliases = [System.Collections.Generic.HashSet[string]]::new()
            foreach ($name in (@($fields[1], $fields[2]) + ($fields[3] -split ','))) {
                $normalized = Normalize-CityName $name
                if ($normalized) { [void]$aliases.Add($normalized) }
            }
            $region = $regions[($fields[8] + '.' + $fields[10])]
            $rows.Add((@($fields[1], ($aliases -join ','), $region, $fields[8], $fields[4], $fields[5], $fields[14]) -join "`t"))
        }
        [System.IO.File]::WriteAllLines((Join-Path (Get-Location) $Output), $rows, [System.Text.UTF8Encoding]::new($false))
        Write-Output "Wrote $($rows.Count) GeoNames cities to $Output"
    } finally { $reader.Dispose() }
} finally { $archive.Dispose() }
