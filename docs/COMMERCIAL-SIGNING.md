# Assinatura comercial — auditoria e procedimento

## Resultado da busca de 13/11/2026

Nenhum keystore de release foi identificado como pertencente ao Ruy POS. Foram pesquisados nomes de arquivos `.jks`, `.keystore`, `.p12` e `.pfx` no projeto, perfil local, ProgramData e diretórios adicionais acessíveis dos discos fixos. A busca não prova ausência em áreas inacessíveis ou em cópias externas.

Foi encontrado o keystore de depuração em `C:\Users\Pichau\.android\debug.keystore`, além de arquivos de outros aplicativos. Não se abriram/descriptografaram nem reutilizaram keystores de terceiros. Não houve tentativa de adivinhar senhas nem exportação de chave privada. O inventário de caminhos fica somente no artefato local ignorado pelo Git.

O certificado do APK de homologação foi verificado por `apksigner` e coincide com o artefato anterior disponível no projeto:

```text
Android Debug — SHA-256
cc33982765d3f045a34a89f484016327da7afe39ed693233646511a19171c1ec
```

Isso não identifica a assinatura da instalação física no Samsung, que não estava conectado, e não transforma a chave debug em chave comercial. A chave e os dados existentes não foram substituídos.

**Nenhuma chave comercial nova foi gerada nesta entrega.** Conforme solicitado, foi preparado um procedimento para geração futura. Portanto, não existe fingerprint comercial novo a informar.

| Campo | Proposta para uma identidade nova, ainda não criada |
|---|---|
| Caminho local | `C:\Users\Pichau\AppData\Local\RuyPOS\Signing\ruy-pos-commercial.p12` |
| Alias | `ruy-pos-commercial` |
| Formato | PKCS12 protegido por senha |
| Algoritmo proposto | RSA 3072 / SHA256withRSA |
| Fingerprint SHA-256 comercial | Indisponível até gerar/conferir o certificado |

## Antes de gerar

Uma nova chave **não permite atualizar automaticamente um APK já distribuído com outra assinatura**. Primeiro, o responsável deve conferir o certificado instalado e o histórico de distribuição/Play App Signing. Não trocar a chave de um app distribuído, não desinstalar para contornar incompatibilidade e não limpar dados. A instalação atual deve continuar utilizável enquanto se define uma estratégia de assinatura/atualização compatível.

Os artefatos comerciais desta entrega estão **sem assinatura**, não são instaláveis como APK definitivo e não devem ser publicados. O AAB também está sem assinatura de upload. Nada foi publicado ou instalado.

## Geração futura sob guarda do proprietário

Somente para uma identidade nova aprovada, em terminal local privado, sem transcrição/gravação:

```powershell
.\scripts\New-CommercialSigningKey.ps1 -AcknowledgeNewIdentity
```

O script não foi executado em seu caminho de geração. Seu procedimento:

1. Recusa gerar sem a opção explícita de nova identidade, recusa caminho dentro do repositório e nunca sobrescreve arquivo existente.
2. Exige pasta dedicada vazia. Restringe sua ACL ao usuário atual e SYSTEM; não altera permissões de arquivos existentes de outros aplicativos.
3. Aciona `keytool` do JDK com formato/algoritmo indicados. A senha é digitada localmente no prompt protegido do keytool, sem constar no comando, Git, chat ou relatório. Use senha aleatória de pelo menos 20 caracteres e guarde-a em gerenciador de senhas.
4. Lista o certificado público para conferir alias e SHA-256. Anote esse fingerprint em registro de custódia. Não publique/exporte a chave privada.

A análise sintática dos scripts passou. Foram executados quatro testes negativos de proteção: ausência da opção de nova identidade; caminho dentro do Git; arquivo debug já existente; tentativa de build comercial com alias debug. Todos recusaram a ação antes de gerar chave ou pedir senha. A geração real, suas ACLs efetivas e a assinatura com uma senha real ainda exigem execução/controladoria local autorizada; não foram declaradas testadas.

## Configuração do Gradle e build protegido

O Gradle aceita os quatro parâmetros somente por variáveis de ambiente: `RUY_RELEASE_STORE_FILE`, `RUY_RELEASE_STORE_PASSWORD`, `RUY_RELEASE_KEY_ALIAS`, `RUY_RELEASE_KEY_PASSWORD`. Configuração parcial falha; ausência completa produz candidato sem assinatura. O storeType é PKCS12. Nenhum segredo entra em `BuildConfig` nem no APK.

Para usar uma chave PKCS12 já aprovada, execute localmente:

```powershell
.\scripts\Build-ProtectedRelease.ps1 `
  -StoreFile "$env:LOCALAPPDATA\RuyPOS\Signing\ruy-pos-commercial.p12" `
  -Alias ruy-pos-commercial
```

O script solicita senhas com `Read-Host -AsSecureString`, passa-as temporariamente ao processo por ambiente, evita daemon/cache de configuração do Gradle e restaura o ambiente anterior ao terminar. As conversões BSTR são zeradas. Não existe persistência de senha em arquivo. Ele somente compila: não instala, não publica e não distribui. Não habilite logs de depuração, dumps de ambiente ou transcrições contendo segredos.

Após um build assinado, conferir com `apksigner verify --verbose --print-certs` e comparar o fingerprint com o registro de custódia e, em atualização, com a identidade de assinatura aplicável à instalação/canal. Conferir também package, versionCode e hash do APK. Não distribuir se houver divergência. `.jks`, `.keystore`, `.p12`, `.pfx` e propriedades de assinatura estão ignorados pelo Git.

## Backup seguro e futuras atualizações

- Manter duas cópias verificadas do PKCS12, em armazenamento cifrado sob guarda do proprietário, em locais distintos; uma cópia externa/offline. Não foram feitas cópias nesta entrega porque a chave ainda não existe.
- Guardar senha em gerenciador de senhas separado das cópias. Registrar caminho, alias, certificado público/fingerprint, data, responsável e canal de distribuição, sem registrar a senha ou chave privada no relatório.
- Validar as cópias comparando o hash do arquivo cifrado e lendo o certificado com a senha em terminal protegido. Essa validação não deve substituir/expor a chave operacional.
- Preservar a mesma identidade para as atualizações futuras e aumentar versionCode. Para Google Play, distinguir chave de assinatura do app de chave de upload, conforme o programa adotado. Nunca gerar outra chave como solução improvisada para perda de senha ou falha de atualização.

O candidato mantém targetSdk 34. Submissão atual ao Google Play também requer adequação à política de target API vigente e validação no Android correspondente; não foi anunciada prontidão para Play. Mudar target/assinatura sem QA em aparelho foi evitado para preservar o comportamento aprovado.

Fontes: [assinatura Android](https://developer.android.com/studio/publish/app-signing), [requisitos atuais de target API do Google Play](https://developer.android.com/google/play/requirements/target-sdk). Nenhuma operação de assinatura comercial, instalação, envio para loja, alteração de serviço Windows ou migração da instalação existente foi realizada.
