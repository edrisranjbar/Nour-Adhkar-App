$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$out = Join-Path $root 'marketing\cafebazaar-covers'
New-Item -ItemType Directory -Force -Path $out | Out-Null

$bg = 'C:\Users\edi\.codex\generated_images\01a02561-5736-7f70-a598-f452a65587f6\exec-e39ee5b6-a0a0-49f0-a08c-6eff638f873e.png'
$refs = 'C:\Users\edi\Downloads\Telegram Desktop'
$font = 'C\:/Windows/Fonts/tahoma.ttf'
$bold = 'C\:/Windows/Fonts/tahomabd.ttf'

function New-Cover([string]$name, [string[]]$inputs, [string]$filter) {
    $target = Join-Path $out $name
    $args = @('-y')
    foreach ($input in $inputs) { $args += @('-i', $input) }
    $args += @('-filter_complex', $filter, '-map', '[v]', '-frames:v', '1', '-c:v', 'mjpeg', '-q:v', '5', '-pix_fmt', 'yuvj420p', $target)
    & ffmpeg @args
    if ($LASTEXITCODE -ne 0) { throw "ffmpeg failed for $name" }
    $item = Get-Item -LiteralPath $target
    if ($item.Length -ge 1000000) {
        $args[-5] = '8'
        & ffmpeg @args
        if ($LASTEXITCODE -ne 0) { throw "ffmpeg compression failed for $name" }
        $item = Get-Item -LiteralPath $target
    }
    if ($item.Length -ge 1000000) { throw "$name is not below 1 MB" }
    "{0} | {1} bytes" -f $item.FullName, $item.Length
}

$overview = @"
[0:v]scale=1680:960[bg];
[1:v]scale=-1:790[s1];[2:v]scale=-1:735[s2];
[bg][s1]overlay=95:105:format=auto[t1];[t1][s2]overlay=510:155:format=auto[t2];
[t2]drawbox=x=1280:y=145:w=290:h=68:color=0xE2E9DC:t=fill,
drawtext=fontfile='$bold':text='همراه آرامش روزانه':fontcolor=0x366C39:fontsize=27:x=1410-text_w/2:y=163,
drawtext=fontfile='$bold':text='نور اذکار':fontcolor=0x366C39:fontsize=82:x=1570-text_w:y=270,
drawtext=fontfile='$font':text='اذکار، دعا و یاد خدا؛ همیشه همراه شما':fontcolor=0x5C695F:fontsize=35:x=1570-text_w:y=385,
drawbox=x=1120:y=475:w=450:h=3:color=0xC7D2C2:t=fill,
drawtext=fontfile='$bold':text='•  اذکار و دعا':fontcolor=0x263028:fontsize=31:x=1570-text_w:y=535,
drawtext=fontfile='$bold':text='•  جست‌وجوی سریع':fontcolor=0x263028:fontsize=31:x=1570-text_w:y=621,
drawtext=fontfile='$bold':text='•  رابط کاربری آرام':fontcolor=0x263028:fontsize=31:x=1570-text_w:y=707[v]
"@
New-Cover 'nour-cover-01-overview.jpg' @($bg, (Join-Path $refs 'photo_2026-08-21_21-00-09 (2).jpg'), (Join-Path $refs 'photo_2026-08-21_21-00-07.jpg')) $overview

$features = @"
[0:v]scale=1680:960[bg];
[1:v]scale=-1:760[s1];[2:v]scale=-1:735[s2];
[bg][s1]overlay=70:120:format=auto[t1];[t1][s2]overlay=490:145:format=auto[t2];
[t2]drawbox=x=1280:y=120:w=290:h=68:color=0xE2E9DC:t=fill,
drawtext=fontfile='$bold':text='برای ساختن یک عادت ماندگار':fontcolor=0x366C39:fontsize=27:x=1410-text_w/2:y=138,
drawtext=fontfile='$bold':text='عبادت منظم، ذکر آگاهانه':fontcolor=0x366C39:fontsize=58:x=1570-text_w:y=245,
drawtext=fontfile='$font':text='پیشرفت روزانه‌ات را ببین و ادامه بده':fontcolor=0x5C695F:fontsize=34:x=1570-text_w:y=350,
drawbox=x=1010:y=465:w=560:h=133:color=0xF8F8F3:t=fill,
drawtext=fontfile='$bold':text='چک‌لیست عبادت':fontcolor=0x366C39:fontsize=32:x=1515-text_w:y=488,
drawtext=fontfile='$font':text='پیگیری ساده فرائض و اعمال روزانه':fontcolor=0x5C695F:fontsize=24:x=1515-text_w:y=542,
drawbox=x=1010:y=625:w=560:h=133:color=0xF8F8F3:t=fill,
drawtext=fontfile='$bold':text='تسبیح شمار هوشمند':fontcolor=0x366C39:fontsize=32:x=1515-text_w:y=648,
drawtext=fontfile='$font':text='ثبت ذکرها و نگهداری تاریخچه':fontcolor=0x5C695F:fontsize=24:x=1515-text_w:y=702[v]
"@
New-Cover 'nour-cover-02-habits-tasbih.jpg' @($bg, (Join-Path $refs 'photo_2026-08-21_21-00-08.jpg'), (Join-Path $refs 'photo_2026-08-21_21-00-07 (2).jpg')) $features

$dark = @"
[0:v]scale=1680:960[bg];color=c=0x0F1411:s=1680x960[dk];
nullsrc=s=1680x960,geq=lum='if(gt(X,850-Y*0.24),255,0)'[mask];[dk][mask]alphamerge[dka];
[bg][dka]overlay=0:0[t0];color=c=0xB19977:s=7x1100,format=rgba,rotate=-13*PI/180:c=none[ln];
[t0][ln]overlay=735:-45[t1];[1:v]scale=-1:675[s1];[2:v]scale=-1:650[s2];
[t1][s1]overlay=100:230:format=auto[t2];[t2][s2]overlay=1070:250:format=auto[t3];
[t3]drawtext=fontfile='$bold':text='هر دو حالت، یک تجربه آرام':fontcolor=0x366C39:fontsize=39:x=560-text_w:y=92,
drawtext=fontfile='$bold':text='روشن یا تاریک':fontcolor=0xF3F6F1:fontsize=62:x=1540-text_w:y=92,
drawtext=fontfile='$font':text='آرامش چشم‌ها در هر ساعت شبانه‌روز':fontcolor=0xBCC9BE:fontsize=30:x=1540-text_w:y=185[v]
"@
New-Cover 'nour-cover-03-light-dark.jpg' @($bg, (Join-Path $refs 'photo_2026-08-21_21-00-06 (2).jpg'), (Join-Path $refs 'photo_2026-08-21_21-00-03.jpg')) $dark
