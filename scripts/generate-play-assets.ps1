Add-Type -AssemblyName System.Drawing

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$outDir = Join-Path $root "docs\store-assets"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null

function New-Brush([int]$r, [int]$g, [int]$b, [int]$a = 255) {
    return New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb($a, $r, $g, $b))
}

function New-Pen([int]$r, [int]$g, [int]$b, [float]$w = 1, [int]$a = 255) {
    return New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb($a, $r, $g, $b), $w)
}

function Add-RoundedRect($path, [float]$x, [float]$y, [float]$w, [float]$h, [float]$r) {
    $d = $r * 2
    $path.AddArc($x, $y, $d, $d, 180, 90)
    $path.AddArc($x + $w - $d, $y, $d, $d, 270, 90)
    $path.AddArc($x + $w - $d, $y + $h - $d, $d, $d, 0, 90)
    $path.AddArc($x, $y + $h - $d, $d, $d, 90, 90)
    $path.CloseFigure()
}

function Draw-Heart($g, [float]$cx, [float]$cy, [float]$scale, $brush) {
    $path = New-Object System.Drawing.Drawing2D.GraphicsPath
    $points = New-Object 'System.Collections.Generic.List[System.Drawing.PointF]'
    for ($i = 0; $i -le 240; $i++) {
        $t = [Math]::PI * 2 * ($i / 240)
        $x = 16 * [Math]::Pow([Math]::Sin($t), 3)
        $y = 13 * [Math]::Cos($t) - 5 * [Math]::Cos(2 * $t) - 2 * [Math]::Cos(3 * $t) - [Math]::Cos(4 * $t)
        $points.Add([System.Drawing.PointF]::new($cx + $x * $scale, $cy - $y * $scale))
    }
    $path.AddPolygon($points.ToArray())
    $g.FillPath($brush, $path)
    $path.Dispose()
}

function Draw-Card($g, [float]$x, [float]$y, [float]$w, [float]$h, [float]$r, $fill, $stroke) {
    $path = New-Object System.Drawing.Drawing2D.GraphicsPath
    Add-RoundedRect $path $x $y $w $h $r
    $g.FillPath($fill, $path)
    $g.DrawPath($stroke, $path)
    $path.Dispose()
}

function Save-PlayIcon {
    $bitmap = New-Object System.Drawing.Bitmap(512, 512, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($bitmap)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit

    $bg = New-Brush 255 244 247
    $panel = New-Brush 255 255 255
    $accent = New-Brush 204 54 82
    $shadow = New-Brush 210 84 103 36
    $stroke = New-Pen 242 198 209 4

    $g.Clear([System.Drawing.Color]::Transparent)
    Draw-Card $g 38 38 436 436 96 $bg $stroke
    Draw-Card $g 86 96 340 320 70 $panel (New-Pen 245 215 222 3)
    Draw-Heart $g 256 254 10.2 $shadow
    Draw-Heart $g 256 242 9.4 $accent

    $font = New-Object System.Drawing.Font("Arial", 34, [System.Drawing.FontStyle]::Bold)
    $textBrush = New-Brush 64 41 49
    $format = New-Object System.Drawing.StringFormat
    $format.Alignment = [System.Drawing.StringAlignment]::Center
    $g.DrawString("Pulse", $font, $textBrush, [System.Drawing.RectangleF]::new(0, 350, 512, 54), $format)

    $path = Join-Path $outDir "play-store-icon-512.png"
    $bitmap.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $g.Dispose()
    $bitmap.Dispose()
}

function Save-FeatureGraphic {
    $bitmap = New-Object System.Drawing.Bitmap(1024, 500, [System.Drawing.Imaging.PixelFormat]::Format24bppRgb)
    $g = [System.Drawing.Graphics]::FromImage($bitmap)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit

    $panel = New-Brush 255 255 255
    $accent = New-Brush 204 54 82
    $ink = New-Brush 49 37 44
    $subtle = New-Brush 111 94 102
    $blue = New-Pen 54 122 153 4
    $red = New-Pen 204 54 82 4
    $green = New-Pen 71 145 92 4
    $grid = New-Pen 238 219 224 2

    $g.Clear([System.Drawing.Color]::FromArgb(255, 246, 248))
    Draw-Heart $g 78 80 2.2 (New-Brush 204 54 82 45)
    Draw-Heart $g 924 430 3.6 (New-Brush 204 54 82 32)

    $titleFont = New-Object System.Drawing.Font("Arial", 56, [System.Drawing.FontStyle]::Bold)
    $bodyFont = New-Object System.Drawing.Font("Arial", 27, [System.Drawing.FontStyle]::Regular)
    $smallFont = New-Object System.Drawing.Font("Arial", 18, [System.Drawing.FontStyle]::Regular)
    $labelFont = New-Object System.Drawing.Font("Arial", 17, [System.Drawing.FontStyle]::Bold)

    Draw-Heart $g 94 108 2.4 $accent
    $g.DrawString("Pulse Log", $titleFont, $ink, 64, 142)
    $g.DrawString("Blood pressure and weight tracking", $bodyFont, $ink, 66, 222)
    $g.DrawString("Calendar | Charts | PDF summary", $smallFont, $subtle, 70, 278)

    Draw-Card $g 620 58 290 384 38 $panel (New-Pen 242 202 212 2)
    Draw-Card $g 646 92 238 80 22 (New-Brush 255 246 248) (New-Pen 244 216 222 2)
    $g.DrawString("TODAY", $labelFont, $accent, 670, 112)
    $g.DrawString("128/82", (New-Object System.Drawing.Font("Arial", 28, [System.Drawing.FontStyle]::Bold)), $ink, 668, 134)

    Draw-Card $g 646 196 238 126 22 (New-Brush 255 255 255) (New-Pen 238 219 224 2)
    for ($i = 0; $i -lt 4; $i++) {
        $y = 220 + $i * 24
        $g.DrawLine($grid, 666, $y, 864, $y)
    }

    $redPoints = @(
        [System.Drawing.PointF]::new(666, 274),
        [System.Drawing.PointF]::new(704, 252),
        [System.Drawing.PointF]::new(742, 260),
        [System.Drawing.PointF]::new(780, 232),
        [System.Drawing.PointF]::new(822, 244),
        [System.Drawing.PointF]::new(862, 218)
    )
    $bluePoints = @(
        [System.Drawing.PointF]::new(666, 296),
        [System.Drawing.PointF]::new(704, 282),
        [System.Drawing.PointF]::new(742, 286),
        [System.Drawing.PointF]::new(780, 268),
        [System.Drawing.PointF]::new(822, 276),
        [System.Drawing.PointF]::new(862, 260)
    )
    $g.DrawLines($red, $redPoints)
    $g.DrawLines($blue, $bluePoints)

    Draw-Card $g 646 342 238 58 18 (New-Brush 255 246 248) (New-Pen 244 216 222 2)
    $g.DrawLine($green, 672, 376, 712, 366)
    $g.DrawLine($green, 712, 366, 752, 370)
    $g.DrawLine($green, 752, 370, 792, 358)
    $g.DrawLine($green, 792, 358, 842, 362)
    $g.DrawString("PDF", $labelFont, $accent, 808, 360)

    $path = Join-Path $outDir "play-feature-graphic-1024x500.png"
    $bitmap.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $g.Dispose()
    $bitmap.Dispose()
}

Save-PlayIcon
Save-FeatureGraphic

Write-Host "Generated Play Store assets in $outDir"
