# SIMULATED ADB responses only. Does not connect to Android or run app/payment tests.
Set-StrictMode -Version 2.0
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$fixtureRoot = Join-Path $projectRoot 'artifacts/stone-homologacao/simulated-preflight'
[System.IO.Directory]::CreateDirectory($fixtureRoot) | Out-Null
$fakeTemplate = @'
$scenario = '__SCENARIO__'
$query = @($args)
Add-Content -LiteralPath (Join-Path $PSScriptRoot ($scenario + '-commands.log')) -Value ($query -join ' ')
if ($query[0] -eq 'devices') {
    Write-Output 'List of devices attached'
    if ($scenario -eq 'Unauthorized') { Write-Output 'SIMULATED-TERMINAL unauthorized'; exit 0 }
    if ($scenario -ne 'NoDevice') { Write-Output 'SIMULATED-TERMINAL device model:SIMULATED' }
    exit 0
}
if ($query[0] -ne '-s' -or $query[1] -ne 'SIMULATED-TERMINAL') { exit 9 }
if ($query[3] -eq 'getprop') {
    if ($query[4] -eq 'ro.build.version.sdk') { Write-Output '30' } else { Write-Output 'SIMULATED' }
    exit 0
}
if ($query[3] -eq 'cmd' -and $query[4] -eq 'package') {
    switch ($query[5]) {
        'help' {
            if ($scenario -eq 'Unsupported') { Write-Output 'SIMULATED unsupported'; exit 1 }
            Write-Output 'query-activities resolve-activity --components'; exit 0
        }
        'query-activities' {
            Write-Output 'br.com.stone.simulated/.PaymentActivity'
            if ($scenario -eq 'MultipleHandlers') { Write-Output 'com.other.simulated/.PaymentActivity' }
            exit 0
        }
        'resolve-activity' {
            if ($scenario -eq 'MultipleHandlers') { Write-Output 'android/.ResolverActivity' }
            else { Write-Output 'br.com.stone.simulated/.PaymentActivity' }
            exit 0
        }
    }
}
exit 9
'@

$completed = 0
foreach ($scenario in @('NoDevice', 'Unauthorized', 'SingleDevice', 'MultipleHandlers', 'Unsupported')) {
    $fakePath = Join-Path $fixtureRoot ($scenario + '-adb.ps1')
    $reportPath = Join-Path $fixtureRoot ($scenario + '-report.json')
    $commandLog = Join-Path $fixtureRoot ($scenario + '-commands.log')
    [System.IO.File]::WriteAllText($fakePath, $fakeTemplate.Replace('__SCENARIO__', $scenario), (New-Object System.Text.UTF8Encoding($false)))
    [System.IO.File]::WriteAllText($commandLog, '')
    & powershell.exe -NoProfile -File (Join-Path $PSScriptRoot 'Test-StoneTerminal.ps1') -Serial SIMULATED-TERMINAL -AdbPath $fakePath -OutputFile $reportPath | Out-Null
    if ($LASTEXITCODE -ne 2) { throw "SIMULATED $scenario did not remain blocked" }
    $report = [System.IO.File]::ReadAllText($reportPath) | ConvertFrom-Json
    if ($report.status -ne 'BLOCKED' -or $report.mode -ne 'READ_ONLY_NO_PAYMENT') { throw "SIMULATED $scenario wrong status" }
    switch ($scenario) {
        'NoDevice' { if ($report.blockers[0] -notmatch 'NO_TERMINAL_CONNECTED') { throw 'No-device guard failed' } }
        'Unauthorized' { if ($report.blockers[0] -notmatch 'UNAUTHORIZED') { throw 'Unauthorized guard failed' } }
        'SingleDevice' { if ($report.paymentApp.status -ne 'SHELL_CANDIDATE_FOUND_NOT_HOMOLOGATED') { throw 'Single-handler query failed' } }
        'MultipleHandlers' { if ($report.paymentApp.candidates.Count -ne 2 -or $report.paymentApp.status -ne 'NOT_VERIFIED') { throw 'Ambiguous-handler guard failed' } }
        'Unsupported' { if (($report.blockers -join ' ') -notmatch 'PACKAGE_QUERY_UNSUPPORTED') { throw 'Unsupported-query guard failed' } }
    }
    $commands = [System.IO.File]::ReadAllText($commandLog)
    if ($commands -match '\b(install|uninstall|clear|force-stop)\b|\bam start\b') { throw 'A non-read-only ADB command was used' }
    $completed++
    Write-Output ("SIMULATED PASS: $scenario")
}
Write-Output ("SIMULATED PREFLIGHT TESTS: $completed passed; no physical terminal used")
