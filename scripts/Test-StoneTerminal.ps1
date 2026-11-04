# Read-only terminal preflight. Does not install, start apps, change settings or initiate payments.
[CmdletBinding()]
param(
    [ValidatePattern('^[A-Za-z0-9._:-]+$')][string]$Serial,
    [string]$AdbPath = (Join-Path $env:LOCALAPPDATA 'Android/Sdk/platform-tools/adb.exe'),
    [string]$OutputFile
)

Set-StrictMode -Version 2.0
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
if (-not $OutputFile) { $OutputFile = Join-Path $projectRoot 'artifacts/stone-homologacao/preflight.json' }
$report = [ordered]@{
    collectedAt = [DateTimeOffset]::Now.ToString('o')
    mode = 'READ_ONLY_NO_PAYMENT'
    selectedSerial = $Serial
    status = 'BLOCKED'
    devices = @()
    device = [ordered]@{}
    paymentApp = [ordered]@{ status = 'NOT_VERIFIED'; query = $null; resolve = $null; candidates = @() }
    blockers = @()
    manualChecks = @('Stone test terminal and assigned StoneCode', 'Authorized installer and accepted APK certificate',
        'Order ID enabled in Stone Settings', 'Official payment app provenance', 'Pix and installment support',
        'Written authorization and test amounts before any transaction')
}

function Invoke-AdbQuery([string[]]$QueryArguments) {
    # All callers below use fixed read-only commands. No shell command strings are evaluated here.
    $previousPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = 'Continue'
        $lines = @(& $AdbPath @QueryArguments 2>&1)
        $queryExitCode = $LASTEXITCODE
    } finally { $ErrorActionPreference = $previousPreference }
    [pscustomobject]@{ exitCode = $queryExitCode; text = (($lines | ForEach-Object { [string]$_ }) -join "`n") }
}

try {
    if (-not (Test-Path -LiteralPath $AdbPath -PathType Leaf)) { throw 'ADB executable not found; supply -AdbPath' }
    $inventory = Invoke-AdbQuery @('devices', '-l')
    if ($inventory.exitCode -ne 0) { throw 'ADB inventory failed' }
    $report.devices = @($inventory.text -split "`r?`n" | ForEach-Object {
        if ($_ -match '^(\S+)\s+(device|unauthorized|offline)\b(.*)$') {
            [pscustomobject]@{ serial = $Matches[1]; state = $Matches[2]; details = $Matches[3].Trim() }
        }
    })
    if ($report.devices.Count -eq 0) { throw 'NO_TERMINAL_CONNECTED: model, settings and payment app cannot be verified' }
    if (-not $Serial) { throw 'SELECT_SERIAL: rerun with -Serial for the intended authorized terminal' }
    $selected = @($report.devices | Where-Object { $_.serial -eq $Serial })
    if ($selected.Count -ne 1 -or $selected[0].state -ne 'device') { throw 'SELECTED_TERMINAL_UNAVAILABLE_OR_UNAUTHORIZED' }
    foreach ($property in @('ro.product.manufacturer', 'ro.product.model', 'ro.build.version.release',
                            'ro.build.version.sdk', 'ro.build.fingerprint', 'ro.product.cpu.abilist')) {
        $value = Invoke-AdbQuery @('-s', $Serial, 'shell', 'getprop', $property)
        $report.device[$property] = if ($value.exitCode -eq 0) { $value.text.Trim() } else { 'UNAVAILABLE' }
    }
    $help = Invoke-AdbQuery @('-s', $Serial, 'shell', 'cmd', 'package', 'help')
    if ($help.exitCode -eq 0 -and $help.text -match 'query-activities' -and $help.text -match '--components') {
        $query = Invoke-AdbQuery @('-s', $Serial, 'shell', 'cmd', 'package', 'query-activities', '--components',
            '-a', 'android.intent.action.VIEW', '-d', 'payment-app://pay')
        $resolve = Invoke-AdbQuery @('-s', $Serial, 'shell', 'cmd', 'package', 'resolve-activity', '--components',
            '-a', 'android.intent.action.VIEW', '-d', 'payment-app://pay')
        $report.paymentApp.query = $query
        $report.paymentApp.resolve = $resolve
        $candidates = @($query.text -split "`r?`n" | Where-Object { $_.Trim() -match '^[A-Za-z0-9_.$]+/[A-Za-z0-9_.$]+$' } | ForEach-Object { $_.Trim() } | Sort-Object -Unique)
        $report.paymentApp.candidates = $candidates
        if ($query.exitCode -eq 0 -and $resolve.exitCode -eq 0 -and $candidates.Count -eq 1 -and $resolve.text.Contains($candidates[0])) {
            $report.paymentApp.status = 'SHELL_CANDIDATE_FOUND_NOT_HOMOLOGATED'
        } else {
            $report.blockers += 'PAYMENT_RESOLUTION_INCONCLUSIVE: inspect query results and official app installation'
        }
    } else {
        $report.blockers += 'PACKAGE_QUERY_UNSUPPORTED: verify through official terminal tools; do not infer app absence'
    }
    $report.blockers += 'MANUAL_EVIDENCE_REQUIRED: Settings, provenance, signature acceptance and financial authorization remain unverified'
} catch {
    $report.blockers += $_.Exception.Message
}

$report.blockers += 'CURRENT_0_9_2_ARTIFACT: review prohibited WAKE_LOCK/RECEIVE_BOOT_COMPLETED, Android Debug signing and HTTP exceptions in the runbook'
$outputPath = [System.IO.Path]::GetFullPath($OutputFile)
[System.IO.Directory]::CreateDirectory((Split-Path -Parent $outputPath)) | Out-Null
[System.IO.File]::WriteAllText($outputPath, ($report | ConvertTo-Json -Depth 8), (New-Object System.Text.UTF8Encoding($false)))
Write-Output ('Preflight: ' + $report.status + '; report: ' + $outputPath)
$report.blockers | ForEach-Object { Write-Output ('- ' + $_) }
exit 2
