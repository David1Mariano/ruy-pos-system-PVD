param(
    [Parameter(Mandatory = $true)][string]$StoreFile,
    [Parameter(Mandatory = $true)][string]$Alias
)
$ErrorActionPreference = 'Stop'
$workspace = [IO.Path]::GetFullPath((Split-Path -Parent $PSScriptRoot)).TrimEnd('\')
$target = [IO.Path]::GetFullPath($StoreFile)
if ($target.StartsWith($workspace + '\', [StringComparison]::OrdinalIgnoreCase)) { throw 'Mantenha a chave fora do Git.' }
if (-not (Test-Path -LiteralPath $target -PathType Leaf)) { throw 'Keystore não encontrado.' }
if ($Alias -match '(?i)androiddebugkey') { throw 'Uma chave debug não é uma assinatura comercial definitiva.' }
Write-Host 'Somente build local. Este script não instala, publica nem distribui o aplicativo.'
$storeSecret = Read-Host 'Senha do keystore (entrada local protegida)' -AsSecureString
$keySecret = Read-Host 'Senha da chave (vazio se igual à senha do keystore)' -AsSecureString
$storePointer = [IntPtr]::Zero; $keyPointer = [IntPtr]::Zero
$releaseEnvNames = @('RUY_RELEASE_STORE_FILE','RUY_RELEASE_KEY_ALIAS','RUY_RELEASE_STORE_PASSWORD','RUY_RELEASE_KEY_PASSWORD')
$previous = @{}
foreach ($name in $releaseEnvNames) { $previous[$name] = [Environment]::GetEnvironmentVariable($name, 'Process') }
try {
    $storePointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($storeSecret)
    $env:RUY_RELEASE_STORE_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($storePointer)
    if ($keySecret.Length -eq 0) { $env:RUY_RELEASE_KEY_PASSWORD = $env:RUY_RELEASE_STORE_PASSWORD }
    else { $keyPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($keySecret); $env:RUY_RELEASE_KEY_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($keyPointer) }
    $env:RUY_RELEASE_STORE_FILE = $target; $env:RUY_RELEASE_KEY_ALIAS = $Alias
    Push-Location $workspace
    try {
        & .\gradlew.bat :app:assembleRelease :app:bundleRelease --no-daemon --no-configuration-cache --console=plain
        if ($LASTEXITCODE -ne 0) { throw 'Build de release falhou. Não distribua o artefato.' }
    } finally { Pop-Location }
} finally {
    foreach ($name in $releaseEnvNames) { [Environment]::SetEnvironmentVariable($name, $previous[$name], 'Process') }
    if ($storePointer -ne [IntPtr]::Zero) { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($storePointer) }
    if ($keyPointer -ne [IntPtr]::Zero) { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($keyPointer) }
    $storeSecret.Dispose(); $keySecret.Dispose()
}
