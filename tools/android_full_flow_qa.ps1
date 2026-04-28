param(
    [string]$ApkPath,
    [string]$PackageName = "com.xingyue.english.debug",
    [string]$ActivityName = "com.xingyue.english.android.XingYueEnglishActivity",
    [string]$DeviceSerial = "",
    [string]$AdbPath = "adb",
    [int]$LaunchWaitSeconds = 10,
    [int]$LogcatTailLines = 2000
)

$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RepoRoot = Split-Path -Parent $ScriptDir
$Timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$OutputDir = Join-Path $RepoRoot "delivery\full-qa-$Timestamp"
$RawResultsPath = Join-Path $OutputDir "results.raw.json"
$ResultsPath = Join-Path $OutputDir "results.json"
$ReportPath = Join-Path $OutputDir "red-green-report.md"
$ReportScript = Join-Path $ScriptDir "android_qa_report.py"

New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null

if ([string]::IsNullOrWhiteSpace($ApkPath)) {
    $candidateApks = @(
        "delivery\XingYueEnglish-debug.apk",
        "androidApp\build\outputs\apk\debug\androidApp-debug.apk"
    )
    foreach ($candidate in $candidateApks) {
        $full = Join-Path $RepoRoot $candidate
        if (Test-Path -LiteralPath $full) {
            $ApkPath = $full
            break
        }
    }
}

if ([string]::IsNullOrWhiteSpace($ApkPath)) {
    throw "No APK path supplied and no default debug APK was found."
}

$ApkPath = (Resolve-Path -LiteralPath $ApkPath).Path

if ($AdbPath -eq "adb" -and -not (Get-Command $AdbPath -ErrorAction SilentlyContinue)) {
    $androidHomeAdb = if ($env:ANDROID_HOME) { Join-Path $env:ANDROID_HOME "platform-tools\adb.exe" } else { "" }
    $androidSdkRootAdb = if ($env:ANDROID_SDK_ROOT) { Join-Path $env:ANDROID_SDK_ROOT "platform-tools\adb.exe" } else { "" }
    if ($androidHomeAdb -and (Test-Path -LiteralPath $androidHomeAdb)) {
        $AdbPath = $androidHomeAdb
    } elseif ($androidSdkRootAdb -and (Test-Path -LiteralPath $androidSdkRootAdb)) {
        $AdbPath = $androidSdkRootAdb
    }
}

if (-not (Get-Command $AdbPath -ErrorAction SilentlyContinue) -and -not (Test-Path -LiteralPath $AdbPath)) {
    throw "adb was not found. Pass -AdbPath or set ANDROID_HOME."
}

if (-not (Test-Path -LiteralPath $ReportScript)) {
    throw "Missing report helper: $ReportScript"
}

$SerialArgs = @()
if (-not [string]::IsNullOrWhiteSpace($DeviceSerial)) {
    $SerialArgs = @("-s", $DeviceSerial)
}

$Steps = New-Object System.Collections.Generic.List[object]
$Artifacts = [ordered]@{}

function Add-Step {
    param(
        [string]$Name,
        [bool]$Ok,
        [string]$Detail
    )
    $Steps.Add([ordered]@{
        name = $Name
        ok = $Ok
        detail = $Detail
    }) | Out-Null
}

function Invoke-Adb {
    param(
        [string[]]$Arguments,
        [string]$StepName,
        [switch]$AllowFailure
    )
    $output = & $AdbPath @SerialArgs @Arguments 2>&1
    $exit = $LASTEXITCODE
    $rawDetail = ($output | Out-String).Trim()
    if ([string]::IsNullOrWhiteSpace($rawDetail)) {
        $rawDetail = "exit=$exit"
    }
    $displayDetail = $rawDetail
    if ($displayDetail.Length -gt 2000) {
        $displayDetail = $displayDetail.Substring(0, 2000) + "... (truncated; see artifact file when available)"
    }
    $ok = ($exit -eq 0)
    Add-Step -Name $StepName -Ok $ok -Detail $displayDetail
    if (-not $ok -and -not $AllowFailure) {
        throw "$StepName failed: $rawDetail"
    }
    return [ordered]@{
        ok = $ok
        exitCode = $exit
        output = $rawDetail
    }
}

function Invoke-AdbBytes {
    param(
        [string[]]$Arguments,
        [string]$OutputPath,
        [string]$StepName
    )
    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = $AdbPath
    $allArgs = $SerialArgs + $Arguments
    $psi.Arguments = ($allArgs | ForEach-Object {
        $arg = [string]$_
        if ($arg -match '[\s"]') {
            '"' + ($arg -replace '"', '\"') + '"'
        } else {
            $arg
        }
    }) -join " "
    $psi.RedirectStandardOutput = $true
    $psi.RedirectStandardError = $true
    $psi.UseShellExecute = $false

    $process = [System.Diagnostics.Process]::Start($psi)
    $buffer = New-Object System.IO.MemoryStream
    $process.StandardOutput.BaseStream.CopyTo($buffer)
    $stderr = $process.StandardError.ReadToEnd()
    $process.WaitForExit()

    [System.IO.File]::WriteAllBytes($OutputPath, $buffer.ToArray())
    $ok = ($process.ExitCode -eq 0 -and (Test-Path -LiteralPath $OutputPath) -and ((Get-Item -LiteralPath $OutputPath).Length -gt 0))
    $detail = if ($ok) { $OutputPath } else { "exit=$($process.ExitCode) $stderr" }
    Add-Step -Name $StepName -Ok $ok -Detail $detail
    if (-not $ok) {
        throw "$StepName failed: $detail"
    }
}

function Write-TextFile {
    param(
        [string]$Path,
        [string]$Text
    )
    Set-Content -LiteralPath $Path -Value $Text -Encoding UTF8
}

function New-Text {
    param([int[]]$Codepoints)
    return -join ($Codepoints | ForEach-Object { [char]$_ })
}

function New-RegexAny {
    param([string[]]$Items)
    return ($Items | ForEach-Object { [regex]::Escape($_) }) -join "|"
}

function Invoke-UiDump {
    param([string]$StepName)
    $last = $null
    for ($attempt = 1; $attempt -le 4; $attempt++) {
        $last = Invoke-Adb -Arguments @("exec-out", "uiautomator", "dump", "/dev/tty") -StepName "$StepName attempt $attempt" -AllowFailure
        if ($last.ok -and $last.output -match "<hierarchy") {
            return $last
        }
        Start-Sleep -Milliseconds 850
    }
    return $last
}

function Get-UiCenterForText {
    param(
        [string]$Xml,
        [string]$Text
    )
    $escaped = [regex]::Escape($Text)
    $pattern = '<node(?=[^>]*(text|content-desc)="' + $escaped + '")[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"'
    $matches = [regex]::Matches($Xml, $pattern)
    if ($matches.Count -le 0) {
        return $null
    }
    $match = $matches |
        Sort-Object { ([int]$_.Groups[3].Value + [int]$_.Groups[5].Value) / 2 } -Descending |
        Select-Object -First 1
    if (-not $match.Success) {
        return $null
    }
    $x1 = [int]$match.Groups[2].Value
    $y1 = [int]$match.Groups[3].Value
    $x2 = [int]$match.Groups[4].Value
    $y2 = [int]$match.Groups[5].Value
    return [ordered]@{
        x = [int](($x1 + $x2) / 2)
        y = [int](($y1 + $y2) / 2)
        bounds = "[$x1,$y1][$x2,$y2]"
    }
}

function Save-QaStepArtifacts {
    param(
        [string]$StepId,
        [string]$Label
    )
    $safeStep = $StepId -replace '[^A-Za-z0-9_-]', '-'
    $screenshot = Join-Path $OutputDir "$safeStep.png"
    Invoke-AdbBytes -Arguments @("exec-out", "screencap", "-p") -OutputPath $screenshot -StepName "$Label screenshot"
    $Artifacts["$safeStep-screenshot"] = $screenshot

    $xmlPath = Join-Path $OutputDir "$safeStep.xml"
    $ui = Invoke-UiDump -StepName "$Label UI XML"
    Write-TextFile -Path $xmlPath -Text $ui.output
    $Artifacts["$safeStep-uiXml"] = $xmlPath
    Add-Step -Name "$Label UI hierarchy" -Ok ($ui.output -match "<hierarchy") -Detail $xmlPath

    $stepLog = Join-Path $OutputDir "$safeStep-logcat.txt"
    $log = Invoke-Adb -Arguments @("logcat", "-d", "-t", "500") -StepName "$Label logcat snapshot" -AllowFailure
    Write-TextFile -Path $stepLog -Text $log.output
    $Artifacts["$safeStep-logcat"] = $stepLog
    return $ui.output
}

function Tap-TextFromUi {
    param(
        [string]$Text,
        [string]$StepName
    )
    $ui = Invoke-UiDump -StepName "$StepName locate UI"
    $point = Get-UiCenterForText -Xml $ui.output -Text $Text
    if ($null -eq $point) {
        Add-Step -Name "$StepName tap" -Ok $false -Detail "Could not find text/content-desc '$Text'"
        return $false
    }
    Invoke-Adb -Arguments @("shell", "input", "tap", "$($point.x)", "$($point.y)") -StepName "$StepName tap $Text at $($point.bounds)" | Out-Null
    Start-Sleep -Milliseconds 900
    return $true
}

try {
    Invoke-Adb -Arguments @("version") -StepName "adb available" | Out-Null

    $devices = Invoke-Adb -Arguments @("devices") -StepName "adb devices"
    if ($devices.output -notmatch "`tdevice") {
        throw "No online adb device found. Output: $($devices.output)"
    }

    Invoke-Adb -Arguments @("install", "-r", "-d", $ApkPath) -StepName "install APK" | Out-Null
    Invoke-Adb -Arguments @("logcat", "-c") -StepName "clear logcat" | Out-Null
    Invoke-Adb -Arguments @("shell", "am", "force-stop", $PackageName) -StepName "force-stop app before launch" -AllowFailure | Out-Null

    $resolved = Invoke-Adb -Arguments @("shell", "cmd", "package", "resolve-activity", "--brief", $PackageName) -StepName "resolve launcher activity" -AllowFailure
    $component = "$PackageName/$ActivityName"
    if ($resolved.ok) {
        $resolvedLines = @($resolved.output -split "`r?`n" | Where-Object { $_ -match "/" })
        if ($resolvedLines.Count -gt 0) {
            $component = $resolvedLines[-1].Trim()
        }
    }

    Invoke-Adb -Arguments @("shell", "am", "start", "-n", $component) -StepName "launch app" | Out-Null
    Start-Sleep -Seconds $LaunchWaitSeconds

    $appPid = Invoke-Adb -Arguments @("shell", "pidof", "-s", $PackageName) -StepName "verify app process" -AllowFailure
    if (-not $appPid.ok -or [string]::IsNullOrWhiteSpace($appPid.output)) {
        Add-Step -Name "process running" -Ok $false -Detail "pidof returned no pid for $PackageName"
    } else {
        Add-Step -Name "process running" -Ok $true -Detail $appPid.output
    }

    $textLearning = New-Text @(0x5B66, 0x4E60)
    $textMaterials = New-Text @(0x7D20, 0x6750)
    $textPractice = New-Text @(0x7EC3, 0x4E60)
    $textWords = New-Text @(0x8BCD, 0x5E93)
    $textProfile = New-Text @(0x6211, 0x7684)

    $homePattern = New-RegexAny @(
        (New-Text @(0x661F, 0x6708, 0x966A, 0x4F60, 0x5B66, 0x82F1, 0x8BED)),
        (New-Text @(0x7EE7, 0x7EED, 0x5B66, 0x4E60)),
        (New-Text @(0x4ECA, 0x65E5, 0x5B66, 0x4E60))
    )
    $materialsPattern = New-RegexAny @(
        (New-Text @(0x7D20, 0x6750, 0x5E93)),
        (New-Text @(0x4E0A, 0x4F20, 0x5185, 0x5BB9)),
        (New-Text @(0x7C98, 0x8D34, 0x94FE, 0x63A5))
    )
    $practicePattern = New-RegexAny @(
        (New-Text @(0x7EC3, 0x4E60, 0x8BA1, 0x5212)),
        (New-Text @(0x4ECA, 0x65E5, 0x987A, 0x5E8F)),
        (New-Text @(0x8BCD, 0x5E93, 0x9636, 0x68AF))
    )
    $wordsPattern = New-RegexAny @(
        (New-Text @(0x751F, 0x8BCD, 0x672C)),
        (New-Text @(0x5B66, 0x4E60, 0x8BBE, 0x7F6E))
    )
    $profilePattern = New-RegexAny @(
        (New-Text @(0x4E2A, 0x4EBA, 0x6570, 0x636E)),
        (New-Text @(0x4E91, 0x7AEF, 0x8F6C, 0x5199)),
        (New-Text @(0x6570, 0x636E, 0x5907, 0x4EFD))
    )

    $homeUi = Save-QaStepArtifacts -StepId "01-home" -Label "home"
    Add-Step -Name "home has main learning entry" -Ok ($homeUi -match $homePattern) -Detail "Checked home UI text"

    $tabCases = @(
        @{ id = "02-materials"; text = $textMaterials; expect = $materialsPattern },
        @{ id = "03-practice"; text = $textPractice; expect = $practicePattern },
        @{ id = "04-words"; text = $textWords; expect = $wordsPattern },
        @{ id = "05-profile"; text = $textProfile; expect = $profilePattern },
        @{ id = "06-home-return"; text = $textLearning; expect = $homePattern }
    )
    foreach ($case in $tabCases) {
        $caseId = $case["id"]
        $caseText = $case["text"]
        $caseExpect = $case["expect"]
        $tapped = Tap-TextFromUi -Text $caseText -StepName "$caseId navigate"
        if ($tapped) {
            $xml = Save-QaStepArtifacts -StepId $caseId -Label $caseText
            Add-Step -Name "$caseId expected UI visible" -Ok ($xml -match $caseExpect) -Detail "Expected pattern: $caseExpect"
        }
    }

    $logcat = Join-Path $OutputDir "logcat.txt"
    $log = Invoke-Adb -Arguments @("logcat", "-d", "-t", "$LogcatTailLines") -StepName "save logcat" -AllowFailure
    Write-TextFile -Path $logcat -Text $log.output
    $Artifacts["logcat"] = $logcat

    $crashLog = Join-Path $OutputDir "crash-logcat.txt"
    $crash = Invoke-Adb -Arguments @("logcat", "-b", "crash", "-d", "-t", "300") -StepName "save crash logcat" -AllowFailure
    Write-TextFile -Path $crashLog -Text $crash.output
    $Artifacts["crashLogcat"] = $crashLog

    $logText = if (Test-Path -LiteralPath $logcat) { Get-Content -LiteralPath $logcat -Raw -ErrorAction SilentlyContinue } else { "" }
    $crashText = if (Test-Path -LiteralPath $crashLog) { Get-Content -LiteralPath $crashLog -Raw -ErrorAction SilentlyContinue } else { "" }
    $hasCrash = ($logText -match "FATAL EXCEPTION|ANR in|Force finishing activity") -or
        ($crashText -match "FATAL EXCEPTION|ANR in|Force finishing activity")
    Add-Step -Name "no obvious crash markers" -Ok (-not $hasCrash) -Detail ($(if ($hasCrash) { "Crash marker found in logcat artifacts" } else { "No fatal crash markers found" }))
}
catch {
    Add-Step -Name "script error" -Ok $false -Detail $_.Exception.Message
}
finally {
    $overallOk = -not ($Steps | Where-Object { -not $_.ok })
    $results = [ordered]@{
        ok = $overallOk
        timestamp = $Timestamp
        apkPath = $ApkPath
        packageName = $PackageName
        activityName = $ActivityName
        deviceSerial = $(if ([string]::IsNullOrWhiteSpace($DeviceSerial)) { "default" } else { $DeviceSerial })
        outputDir = $OutputDir
        artifacts = $Artifacts
        steps = $Steps
    }

    $results | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $RawResultsPath -Encoding UTF8

    $python = Get-Command python -ErrorAction SilentlyContinue
    if (-not $python) {
        $python = Get-Command py -ErrorAction SilentlyContinue
    }
    if ($python) {
        & $python.Source $ReportScript --input $RawResultsPath --json-output $ResultsPath --markdown-output $ReportPath
    } else {
        Copy-Item -LiteralPath $RawResultsPath -Destination $ResultsPath -Force
        Set-Content -LiteralPath $ReportPath -Encoding UTF8 -Value "# Android Full Flow QA`n`nPython was not found; see results.json."
    }

    Remove-Item -LiteralPath $RawResultsPath -Force -ErrorAction SilentlyContinue
    Write-Host "QA output: $OutputDir"
    Write-Host "Report: $ReportPath"
    Write-Host "Results: $ResultsPath"

    if (-not $overallOk) {
        exit 1
    }
}
