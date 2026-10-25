Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
if (-not $scriptDir) { $scriptDir = $PSScriptRoot }
if (-not $scriptDir) { $scriptDir = [System.IO.Path]::GetDirectoryName([System.Diagnostics.Process]::GetCurrentProcess().MainModule.FileName) }
$base = Split-Path -Parent $scriptDir
if (-not (Test-Path "$base\SpaceWar.jar")) {
    $base = "C:\Users\Uživatel\Desktop\game_space_war"
}

$form = New-Object System.Windows.Forms.Form
$form.Text = "Space War Launcher"
$form.Size = New-Object System.Drawing.Size(560, 270)
$form.StartPosition = "CenterScreen"
$form.FormBorderStyle = "None"
$form.BackColor = [System.Drawing.Color]::FromArgb(4, 6, 12)
$form.TopMost = $true

# Custom Retro Sci-Fi Border
$form.add_Paint({
    param($sender, $e)
    $borderPen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(59, 130, 246), 2)
    $e.Graphics.DrawRectangle($borderPen, 1, 1, 557, 267)
    $innerPen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(30, 58, 138), 1)
    $e.Graphics.DrawRectangle($innerPen, 6, 6, 547, 257)
    $cornerPen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(253, 224, 71), 2)
    # Tech corner brackets
    $e.Graphics.DrawLine($cornerPen, 6, 6, 26, 6)
    $e.Graphics.DrawLine($cornerPen, 6, 6, 6, 26)
    $e.Graphics.DrawLine($cornerPen, 553, 6, 533, 6)
    $e.Graphics.DrawLine($cornerPen, 553, 6, 553, 26)
    $e.Graphics.DrawLine($cornerPen, 6, 263, 26, 263)
    $e.Graphics.DrawLine($cornerPen, 6, 263, 6, 243)
    $e.Graphics.DrawLine($cornerPen, 553, 263, 533, 263)
    $e.Graphics.DrawLine($cornerPen, 553, 263, 553, 243)
})

# Header Label
$title = New-Object System.Windows.Forms.Label
$title.Text = "S P A C E   W A R"
$title.ForeColor = [System.Drawing.Color]::FromArgb(240, 246, 255)
$title.Font = New-Object System.Drawing.Font("Consolas", 18, [System.Drawing.FontStyle]::Bold)
$title.TextAlign = "MiddleCenter"
$title.Size = New-Object System.Drawing.Size(540, 40)
$title.Location = New-Object System.Drawing.Point(10, 25)
$form.Controls.Add($title)

# Subtitle
$sub = New-Object System.Windows.Forms.Label
$sub.Text = "GALAXY CONQUEST ENGINE // INITIALIZING CLIENT"
$sub.ForeColor = [System.Drawing.Color]::FromArgb(96, 165, 250)
$sub.Font = New-Object System.Drawing.Font("Consolas", 9, [System.Drawing.FontStyle]::Regular)
$sub.TextAlign = "MiddleCenter"
$sub.Size = New-Object System.Drawing.Size(540, 20)
$sub.Location = New-Object System.Drawing.Point(10, 70)
$form.Controls.Add($sub)

# Status Box
$status = New-Object System.Windows.Forms.Label
$status.Text = "> Calibrating stellar matrix & parallax starfield..."
$status.ForeColor = [System.Drawing.Color]::FromArgb(148, 163, 184)
$status.Font = New-Object System.Drawing.Font("Consolas", 9)
$status.Size = New-Object System.Drawing.Size(500, 25)
$status.Location = New-Object System.Drawing.Point(30, 125)
$form.Controls.Add($status)

# Progress Panel Frame
$pnlBg = New-Object System.Windows.Forms.Panel
$pnlBg.Size = New-Object System.Drawing.Size(500, 22)
$pnlBg.Location = New-Object System.Drawing.Point(30, 155)
$pnlBg.BackColor = [System.Drawing.Color]::FromArgb(15, 23, 42)
$form.Controls.Add($pnlBg)

# Progress Fill
$pnlBar = New-Object System.Windows.Forms.Panel
$pnlBar.Size = New-Object System.Drawing.Size(10, 22)
$pnlBar.Location = New-Object System.Drawing.Point(0, 0)
$pnlBar.BackColor = [System.Drawing.Color]::FromArgb(59, 130, 246)
$pnlBg.Controls.Add($pnlBar)

# Loading timer animation
$timer = New-Object System.Windows.Forms.Timer
$timer.Interval = 25
$step = 0
$statusMessages = @(
    "> Allocating procedural spiral arms...",
    "> Slicing 11 celestial biomes & moons...",
    "> Generating 4-faction starship fleets...",
    "> Starting OpenGL canvas..."
)

$timer.add_Tick({
    $script:step += 10
    if ($step -le 500) {
        $pnlBar.Width = $step
        $msgIdx = [Math]::Min([Math]::Floor($step / 125), $statusMessages.Length - 1)
        $status.Text = $statusMessages[$msgIdx]
    } else {
        $timer.Stop()
        $form.Close()
    }
})

$form.Add_Shown({
    $timer.Start()
    $jarPath = "$base\SpaceWar.jar"
    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = "javaw.exe"
    $psi.Arguments = "-jar `"$jarPath`""
    $psi.WorkingDirectory = $base
    [System.Diagnostics.Process]::Start($psi) | Out-Null
})

[void]$form.ShowDialog()
