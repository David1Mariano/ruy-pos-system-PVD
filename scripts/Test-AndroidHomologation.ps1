param(
    [Parameter(Mandatory = $true)][string]$Serial,
    [switch]$IsolatedTestPhone
)
$ErrorActionPreference = 'Stop'
$workspace = Split-Path -Parent $PSScriptRoot
$sdkRoot = $env:ANDROID_HOME
if (-not $sdkRoot) { throw 'Defina ANDROID_HOME.' }
$adb = Join-Path $sdkRoot 'platform-tools/adb.exe'
function Invoke-Adb([string[]]$Arguments) {
    & $adb -s $Serial @Arguments
    if ($LASTEXITCODE -ne 0) { throw "ADB falhou: $($Arguments[0])" }
}
if ($Serial -notmatch '^emulator-\d+$' -and -not $IsolatedTestPhone) {
    throw 'Para celular de testes isolado, informe -IsolatedTestPhone. Nunca use a T8 comercial.'
}
$manufacturer = (Invoke-Adb @('shell', 'getprop', 'ro.product.manufacturer') | Out-String).Trim()
$model = (Invoke-Adb @('shell', 'getprop', 'ro.product.model') | Out-String).Trim()
if ("$manufacturer $model" -match '(?i)stone|\bT8\b|sunmi|gpos|l400|l300') { throw 'Terminal comercial não permitido neste roteiro.' }
Push-Location $workspace
try {
    & .\gradlew.bat :app:assembleHomologation :app:assembleHomologationAndroidTest --console=plain
    if ($LASTEXITCODE -ne 0) { throw 'Build falhou.' }
    $metadata = Get-Content app/build/outputs/apk/homologation/output-metadata.json -Raw | ConvertFrom-Json
    $testMetadata = Get-Content app/build/outputs/apk/androidTest/homologation/output-metadata.json -Raw | ConvertFrom-Json
    if ($metadata.applicationId -ne 'br.com.ruy.pos.homologation' -or $testMetadata.applicationId -ne 'br.com.ruy.pos.homologation.test') { throw 'Package inesperado; instalação bloqueada.' }
    Invoke-Adb @('install', '-r', (Join-Path $workspace 'app/build/outputs/apk/homologation/app-homologation.apk'))
    Invoke-Adb @('install', '-r', (Join-Path $workspace 'app/build/outputs/apk/androidTest/homologation/app-homologation-androidTest.apk'))
    $result = Invoke-Adb @('shell', 'am', 'instrument', '-w', '-r', 'br.com.ruy.pos.homologation.test/androidx.test.runner.AndroidJUnitRunner')
    $result | Write-Output
    $summary = $result | Out-String
    if ($summary -match 'FAILURES!!!|INSTRUMENTATION_FAILED|shortMsg=' -or $summary -notmatch 'OK \(\d+ tests?\)') {
        throw 'A instrumentação não comprovou aprovação de todos os testes.'
    }
} finally { Pop-Location }
