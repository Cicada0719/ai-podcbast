param(
    [string]$ApkPath = "delivery\XingYueEnglish-release.apk",
    [string]$OutputDir = "",
    [string]$PackageName = "com.xingyue.english"
)

$ErrorActionPreference = "Stop"

function Resolve-ProjectRoot {
    $scriptDir = Split-Path -Parent $PSCommandPath
    return (Resolve-Path (Join-Path $scriptDir "..")).Path
}

function Find-ApkSigner {
    $sdk = $env:ANDROID_HOME
    if (-not $sdk) { $sdk = $env:ANDROID_SDK_ROOT }
    if (-not $sdk) {
        $defaultSdk = Join-Path $env:LOCALAPPDATA "Android\Sdk"
        if (Test-Path $defaultSdk) { $sdk = $defaultSdk }
    }
    if (-not $sdk) { return $null }
    $tools = Get-ChildItem -LiteralPath (Join-Path $sdk "build-tools") -Recurse -Filter "apksigner.bat" -ErrorAction SilentlyContinue |
        Sort-Object FullName -Descending
    return $tools | Select-Object -First 1
}

function Add-Check {
    param(
        [System.Collections.ArrayList]$Checks,
        [string]$Name,
        [string]$Status,
        [string]$Detail
    )
    [void]$Checks.Add([ordered]@{
        name = $Name
        status = $Status
        detail = $Detail
    })
}

function Read-RegexValue {
    param([string]$Text, [string]$Pattern)
    $match = [regex]::Match($Text, $Pattern)
    if ($match.Success) { return $match.Groups[1].Value.Trim() }
    return ""
}

function Invoke-Capture {
    param([string]$FileName, [string]$Arguments)
    $psi = [System.Diagnostics.ProcessStartInfo]::new()
    $psi.FileName = $FileName
    $psi.Arguments = $Arguments
    $psi.UseShellExecute = $false
    $psi.RedirectStandardOutput = $true
    $psi.RedirectStandardError = $true
    $process = [System.Diagnostics.Process]::Start($psi)
    $stdout = $process.StandardOutput.ReadToEnd()
    $stderr = $process.StandardError.ReadToEnd()
    $process.WaitForExit()
    return [ordered]@{
        exitCode = $process.ExitCode
        output = ($stdout + "`n" + $stderr).Trim()
    }
}

$root = Resolve-ProjectRoot
Set-Location $root

if (-not $OutputDir) {
    $stamp = Get-Date -Format "yyyyMMdd-HHmmss"
    $OutputDir = Join-Path $root "delivery\release-readiness-$stamp"
}
New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null

$apk = if ([System.IO.Path]::IsPathRooted($ApkPath)) { $ApkPath } else { Join-Path $root $ApkPath }
$checks = [System.Collections.ArrayList]::new()
$buildText = Get-Content -LiteralPath (Join-Path $root "androidApp\build.gradle.kts") -Raw -Encoding UTF8
$rootBuildText = Get-Content -LiteralPath (Join-Path $root "build.gradle.kts") -Raw -Encoding UTF8
$manifestPath = Join-Path $root "androidApp\build\intermediates\merged_manifests\release\processReleaseManifest\AndroidManifest.xml"
$manifestText = if (Test-Path $manifestPath) { Get-Content -LiteralPath $manifestPath -Raw -Encoding UTF8 } else { "" }
$privacyPath = Join-Path $root "Privacy Policy.md"
$privacyText = if (Test-Path $privacyPath) { Get-Content -LiteralPath $privacyPath -Raw -Encoding UTF8 } else { "" }
$networkConfigPath = Join-Path $root "androidApp\src\main\res\xml\network_security_config.xml"
$networkConfigText = if (Test-Path $networkConfigPath) { Get-Content -LiteralPath $networkConfigPath -Raw -Encoding UTF8 } else { "" }
$localPropertiesPath = Join-Path $root "local.properties"
$localText = if (Test-Path $localPropertiesPath) { Get-Content -LiteralPath $localPropertiesPath -Raw -Encoding UTF8 } else { "" }

$applicationId = Read-RegexValue $buildText 'applicationId\s*=\s*"([^"]+)"'
$targetSdk = Read-RegexValue $buildText 'targetSdk\s*=\s*(\d+)'
$minSdk = Read-RegexValue $buildText 'minSdk\s*=\s*(\d+)'
$versionCode = Read-RegexValue $buildText 'versionCode\s*=\s*(\d+)'
$versionName = Read-RegexValue $rootBuildText 'version\s*=\s*"([^"]+)"'

Add-Check $checks "release APK exists" ($(if (Test-Path $apk) { "GREEN" } else { "RED" })) $apk
Add-Check $checks "applicationId" ($(if ($applicationId -eq $PackageName) { "GREEN" } else { "RED" })) $applicationId
Add-Check $checks "version metadata" ($(if ($versionCode -and $versionName) { "GREEN" } else { "RED" })) "versionCode=$versionCode versionName=$versionName"
Add-Check $checks "targetSdk" ($(if ([int]$targetSdk -ge 35) { "GREEN" } else { "YELLOW" })) "targetSdk=$targetSdk minSdk=$minSdk"

if ($manifestText) {
    $permissions = [regex]::Matches($manifestText, '<uses-permission[^>]+android:name="([^"]+)"') |
        ForEach-Object { $_.Groups[1].Value } |
        Sort-Object -Unique
    Add-Check $checks "allowBackup disabled" ($(if ($manifestText -match 'android:allowBackup="false"') { "GREEN" } else { "RED" })) "release merged manifest"
    Add-Check $checks "release not debuggable" ($(if ($manifestText -match 'android:debuggable="true"') { "RED" } else { "GREEN" })) "android:debuggable=true not present"
    Add-Check $checks "permission disclosure set" ($(if ($permissions -contains "android.permission.INTERNET" -and $permissions -contains "android.permission.ACCESS_NETWORK_STATE") { "GREEN" } else { "YELLOW" })) ($permissions -join ", ")
} else {
    $permissions = @()
    Add-Check $checks "release merged manifest" "RED" "missing $manifestPath"
}

Add-Check $checks "privacy policy exists" ($(if (Test-Path $privacyPath) { "GREEN" } else { "RED" })) $privacyPath
Add-Check $checks "privacy policy mentions final permissions" ($(if ($privacyText -match 'ACCESS_NETWORK_STATE' -and $privacyText -match 'INTERNET') { "GREEN" } else { "RED" })) "Privacy Policy.md"
Add-Check $checks "cleartext traffic review" ($(if ($networkConfigText -match 'cleartextTrafficPermitted="true"') { "YELLOW" } else { "GREEN" })) "true allows http links; keep only if platform import requires it"

$hasReleaseKeys =
    $localText -match '(?m)^xingyue\.release\.storeFile\s*=' -and
    $localText -match '(?m)^xingyue\.release\.storePassword\s*=' -and
    $localText -match '(?m)^xingyue\.release\.keyAlias\s*=' -and
    $localText -match '(?m)^xingyue\.release\.keyPassword\s*='
Add-Check $checks "release signing config present" ($(if ($hasReleaseKeys) { "GREEN" } else { "RED" })) "local.properties xingyue.release.*"

$apksigner = Find-ApkSigner
$signingOutput = ""
if ($apksigner -and (Test-Path $apk)) {
    $capture = Invoke-Capture $apksigner.FullName "verify --print-certs `"$apk`""
    $signingOutput = $capture.output
    $signingStatus = if ($signingOutput -match 'CN=Android Debug') { "RED" } elseif ($signingOutput -match 'Signer #1 certificate') { "GREEN" } else { "RED" }
    Add-Check $checks "release signing certificate" $signingStatus ($signingOutput.Trim() -replace "`r?`n", " | ")
} else {
    Add-Check $checks "release signing certificate" "RED" "apksigner or APK missing"
}

$overall = if ($checks | Where-Object { $_.status -eq "RED" }) {
    "RED"
} elseif ($checks | Where-Object { $_.status -eq "YELLOW" }) {
    "YELLOW"
} else {
    "GREEN"
}

$result = [ordered]@{
    generatedAt = (Get-Date).ToString("s")
    overall = $overall
    apkPath = $apk
    applicationId = $applicationId
    versionCode = $versionCode
    versionName = $versionName
    checks = $checks
}

$jsonPath = Join-Path $OutputDir "results.json"
$mdPath = Join-Path $OutputDir "release-readiness-report.md"
$result | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $jsonPath -Encoding UTF8

$lines = New-Object System.Collections.Generic.List[string]
$lines.Add("# Android Release Readiness: $overall")
$lines.Add("")
$lines.Add("- APK: $apk")
$lines.Add("- applicationId: $applicationId")
$lines.Add("- version: $versionName ($versionCode)")
$lines.Add("")
$lines.Add("| Check | Status | Detail |")
$lines.Add("| --- | --- | --- |")
foreach ($check in $checks) {
    $detail = ($check.detail -replace "\|", "/" -replace "`r?`n", " ").Trim()
    if ($detail.Length -gt 260) { $detail = $detail.Substring(0, 260) + "..." }
    $lines.Add("| $($check.name) | $($check.status) | $detail |")
}
$lines.Add("")
if ($overall -eq "RED") {
    $lines.Add("## Blocking Items")
    $lines.Add("")
    $lines.Add("- Configure a real release keystore in local.properties before store upload.")
    $lines.Add("- Rebuild release and rerun this script until the signing checks are not RED.")
}
$lines | Set-Content -LiteralPath $mdPath -Encoding UTF8

Write-Host "Release readiness: $overall"
Write-Host $mdPath
if ($overall -eq "RED") { exit 2 }
