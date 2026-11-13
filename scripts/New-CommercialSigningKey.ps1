param(
    [string]$StoreFile = (Join-Path $env:LOCALAPPDATA 'RuyPOS\Signing\ruy-pos-commercial.p12'),
    [string]$Alias = 'ruy-pos-commercial',
    [switch]$AcknowledgeNewIdentity
)
$ErrorActionPreference = 'Stop'
if (-not $AcknowledgeNewIdentity) { throw 'Uma nova chave não substitui a assinatura de um app distribuído. Revise o roteiro e use -AcknowledgeNewIdentity somente para uma identidade nova aprovada.' }
$workspace = [IO.Path]::GetFullPath((Split-Path -Parent $PSScriptRoot)).TrimEnd('\')
$target = [IO.Path]::GetFullPath($StoreFile)
if ($target.StartsWith($workspace + '\', [StringComparison]::OrdinalIgnoreCase)) { throw 'O keystore deve ficar fora do repositório.' }
if (Test-Path -LiteralPath $target) { throw 'O arquivo já existe. Não será substituído.' }
if ($Alias -notmatch '^[a-z0-9][a-z0-9._-]{2,63}$') { throw 'Alias inválido.' }
$keytool = Join-Path $env:JAVA_HOME 'bin/keytool.exe'
if (-not (Test-Path -LiteralPath $keytool)) { throw 'Configure JAVA_HOME para JDK 17.' }
$folder = Split-Path -Parent $target
if ((Test-Path -LiteralPath $folder) -and @(Get-ChildItem -LiteralPath $folder -Force).Count -gt 0) { throw 'Use uma pasta dedicada vazia; nenhuma permissão de arquivos existentes será alterada.' }
if (-not (Test-Path -LiteralPath $folder)) { New-Item -ItemType Directory -Path $folder -Force | Out-Null }
& {
    $acl = New-Object Security.AccessControl.DirectorySecurity
    $acl.SetAccessRuleProtection($true, $false)
    $identity = [Security.Principal.WindowsIdentity]::GetCurrent().User
    foreach ($principal in @($identity, [Security.Principal.SecurityIdentifier]::new('S-1-5-18'))) {
        $rule = [Security.AccessControl.FileSystemAccessRule]::new($principal, 'FullControl', 'ContainerInherit,ObjectInherit', 'None', 'Allow')
        $acl.AddAccessRule($rule)
    }
    Set-Acl -LiteralPath $folder -AclObject $acl
}
Write-Host 'Execute em terminal local sem transcrição/gravação. O keytool solicitará a senha sem exibi-la. Use senha aleatória com pelo menos 20 caracteres e guarde-a em um gerenciador de senhas.'
& $keytool -genkeypair -v -storetype PKCS12 -keystore $target -alias $Alias -keyalg RSA -keysize 3072 -sigalg SHA256withRSA -validity 10000 -dname 'CN=RUY POS Commercial, O=RUY POS, C=BR'
if ($LASTEXITCODE -ne 0) { throw 'Geração não concluída. Não reutilize um arquivo parcial sem revisão.' }
Write-Host "Keystore: $target"
Write-Host "Alias: $Alias"
Write-Host 'Verifique e anote o fingerprint SHA-256 do certificado público. O comando abaixo solicitará a senha localmente.'
& $keytool -list -v -keystore $target -alias $Alias
if ($LASTEXITCODE -ne 0) { throw 'Não foi possível conferir o certificado.' }
Write-Host 'Faça duas cópias em armazenamento cifrado sob sua guarda, em locais distintos, e teste a leitura do certificado nas cópias. Nunca envie a chave privada ou a senha ao Git/chat.'
