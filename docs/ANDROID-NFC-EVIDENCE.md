# Evidências Android/NFC — 13/11/2026

**Entrega de implementação e validação local. Homologação em celular e pagamento NFC real permanecem pendentes. Nenhuma cobrança foi executada.**

## Identificação e preservação

- Base conferida: branch `stabilization/android-1.0-independent`, HEAD `c29bf376d22b4e908fb1a1a74f057037f51bc180`, árvore inicialmente limpa.
- Branch de trabalho: `homologation/android-nfc`.
- Commit da implementação/testes: `12146ab` (`feat(android): isolate homologation and add audited external TapStone flow`). O commit posterior contém este relatório e o roteiro.
- Repositório: `https://github.com/David1Mariano/ruy-pos-system-PVD.git`.
- Package operacional mantido: `br.com.ruy.pos`. Nenhum APK foi instalado, desinstalado ou executado em aparelho físico; nenhum comando de limpeza de dados foi executado. Não houve acesso à T8 nem a bancos operacionais. A versão/assinatura/dados instalados no celular não puderam ser lidos por ausência de aparelho conectado.
- App sem restrição por fabricante/modelo; instalação declarada Android 6/API23+, target API34. Isso não certifica todos os celulares. TapStone requer Android 10+, NFC habilitado, internet e conta/app Stone autorizados. Sem esses requisitos, as outras modalidades continuam disponíveis. APK Android não instala em iPhone.
- Modelo do aparelho, presença/estado de NFC, instalação/autorização Stone: **não verificados em dispositivo**. O aplicativo mostra diagnóstico local e exige confirmação do operador para os aspectos da conta/provedor.
- Room continua no schema 8; nenhuma migração nova ou destrutiva foi introduzida. V7→v8 foi testada com fixture; versões históricas 1→6 sem fixtures reais não foram homologadas.

## Artefatos e assinatura

| Item | Resultado |
|---|---|
| Versão base | 0.9.3 |
| Versão do APK isolado | 0.9.3-homologacao |
| VersionCode | 21 |
| Package do APK isolado | `br.com.ruy.pos.homologation` |
| APK local entregue | `artifacts/android-nfc-homologation/RUY-POS-0.9.3-homologacao-vc21.apk` |
| APK de instrumentação | `artifacts/android-nfc-homologation/RUY-POS-0.9.3-homologacao-tests.apk` |
| Tipo da assinatura | Android Debug; sem assinatura de distribuição comercial |
| Verificação apksigner | Exit 0, v1 e v2 válidos |
| Certificado SHA-256 | `cc33982765d3f045a34a89f484016327da7afe39ed693233646511a19171c1ec` |

SHA-256 completo do APK de homologação:

```text
1dfe44271496dd8a8b9ad25bbea35152fff18936dfb6e1ac8f9c1dd5719585a8
```

O certificado coincide com o APK anterior **disponível no projeto**, `artifacts/RuyPOS-0.9.2-Stone-debug.apk`. Não comprova a assinatura da instalação no celular. Não se deve substituir a instalação original por este APK; seu package é deliberadamente distinto. APKs, relatórios detalhados e logs ficam em `artifacts/`, ignorado pelo Git; o código e o resumo verificável dos testes são versionados.

O `aapt dump badging` do APK final confirmou package/versionCode/versionName acima e ausência de `android.permission.INTERNET`. Permanecem ACCESS_NETWORK_STATE, NFC opcional e permissões normais do WorkManager. O modo de homologação bloqueia também `SyncClient`, worker e entrada legada, e identifica pedidos fictícios fora da fila de produção.

## Resultados executados

Comando final aprovado:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:testHomologationUnitTest :app:assembleHomologation :app:assembleHomologationAndroidTest :app:lintHomologation --console=plain
```

| Verificação | Resultado observado |
|---|---|
| Unitários/Robolectric debug | 78 casos: 76 aprovados, 2 skips, 0 falhas/erros |
| Unitários/Robolectric homologation | 79 casos: 77 aprovados, 2 skips, 0 falhas/erros |
| Total nas duas variantes | 153 execuções aprovadas, 4 skips; há casos repetidos entre variantes |
| Build aplicativo isolado | Aprovado |
| Build APK instrumentado | Aprovado; 4 métodos de teste preparados |
| Lint homologation | 0 erros, 21 avisos; não é lint sem avisos |
| PowerShell do roteiro | Sintaxe analisada sem erros; script não instalado/executado contra dispositivo |
| Revisão Git | `git diff --check`/`git diff --cached --check` sem erros de whitespace |
| APK/certificado/package/hash | Verificados localmente com apksigner, aapt e SHA-256 |

As execuções incluem SQLite/Room em diretórios temporários do Robolectric, com reabertura de arquivos, concorrência e transações. Não equivalem a testes instrumentados no Android. Contagens por suíte: [ANDROID-NFC-TEST-RESULTS.json](ANDROID-NFC-TEST-RESULTS.json).

Cobertura executada:

- Contrato Smart POS anterior: valores, referências, campos aceitos, callback inválido/desconhecido/duplicado, recusa/cancelamento, retorno incompleto, divergências, timeout, falhas de abertura e conciliação; os contextos de lançamento são simulados. O caminho normal continua bloqueado por build.
- Fluxo TapStone externo local: pendência persistida, abandono/retorno arbitrário, retomada após reabrir SQLite, ausência/divergência de comprovante, pedido/valor/modalidade divergentes, confirmação simultânea/repetida, referência reutilizada, operador/evidência auditados, caixa fechado, pedido cancelado e mesa parcialmente quitada. O teste de requisitos locais exige simultaneamente Android compatível, NFC presente/ativo e internet validada; não simula uma transação NFC.
- Mesa 05: duas Coxinhas de R$8 e um Caldo de R$7, total R$23, bloqueio de liberação antes da quitação, fechamento integral e persistência sem duplicação.
- Caixa: abertura R$100, venda fictícia em dinheiro R$23, sangria R$20, esperado/contado R$103, diferença zero, modalidade e fechamento persistidos após reabrir o banco.
- Backup real da implementação em sandbox: banco/manifesto/foto PNG, comparação de pedidos, pagamentos, movimentos, produtos e imagem após restaurar e reabrir; ZIP inválido, banco/foto ausentes, checksum divergente, SQLite com cabeçalho válido mas conteúdo inválido, path traversal, ambiente divergente, exceção de substituição e interrupção injetada após substituição com rollback no próximo startup. ZIP legado validado em debug e recusado em homologation.
- Isolamento: package distinto, ausência de permissão INTERNET, bloqueio de envio, semente idempotente, caixa fictício, SIMULADO exclusivo e pedido fora da fila de sincronização de produção.

## Bloqueios e testes ainda não executados

1. `adb devices -l` iniciou e terminou sem aparelho físico. Modelo/NFC/Android/conta Stone não foram consultados em celular.
2. Instalados Android Emulator 37.2.13 e imagem Google APIs Android 30 x86_64, criado AVD exclusivo `RuyHomologation`. `-accel-check` retornou driver de hypervisor ausente. O perfil inicial exigia mais espaço que o disponível; o disco de dados foi reduzido a 1 GiB. Duas tentativas sem aceleração, com renderização por software, não disponibilizaram o Android (ADB `offline`). Os processos criados para esse AVD foram encerrados. Não se alteraram drivers nem recursos do Windows; não houve reinicialização do computador.
3. Tentativa efetiva de `:app:connectedHomologationAndroidTest`: **falhou antes da execução com `DeviceException: No connected devices!`**. Nenhum dos 4 métodos instrumentados foi executado.
4. Os dois testes FileProvider por variante foram explicitamente ignorados no host Windows. AndroidX usa regras de caminhos Android/Linux; o ensaio Robolectric com caminhos `C:\...` não resolveu a raiz. O bytecode da dependência AndroidX Core 1.13.0 foi inspecionado para confirmar o tratamento de `/`. Leitura via content URI, permissões entre apps e compartilhamento real continuam dependentes da suíte nativa e ensaio manual.
5. Sem evidência de navegação Compose executada, seletor Android de fotos/permissões, PDF nativo A4/paginação visual, compartilhamento com outro aplicativo, restauração/reinício do processo Android ou alternância real entre internet ligada/desligada. Essas etapas estão preparadas no roteiro e na instrumentação. A reabertura SQLite local e o cálculo de relatórios foram executados, mas não substituem esses ensaios.
6. Backup não foi homologado contra falha física de energia, armazenamento esgotado ou modelos variados. Não foram importados bancos operacionais. A instalação original não foi comparada fisicamente antes/depois; os comandos desta entrega não a acessaram.

Logs locais: `build-final.log`, `build-instrumented.log`, logs das tentativas de emulador, `lint-results-homologation.txt`, `apk-signature.txt` na pasta de artefatos. O [roteiro manual](ANDROID-NFC-RUNBOOK.md) e o [script com serial explícito](../scripts/Test-AndroidHomologation.ps1) permitem continuar em emulador funcional ou celular de testes isolado.

## Pagamentos reais e conclusão da pesquisa

Foi implementado **PAGAMENTO POR APROXIMAÇÃO — STONE EXTERNO**, com abertura manual da Stone e confirmação manual auditada. A modalidade não é aprovação automática nem consulta à adquirente. No APK entregue de homologação, a preparação de cobrança real fica desabilitada; usa-se dinheiro fictício ou SIMULADO. A interface existente foi mantida, com faixa contextual e controles do novo fluxo; não houve comparação visual em aparelho.

A [documentação de requisitos TapStone](https://ajuda.stone.com.br/compatibilidade-e-requisitos) pede Android 10+ com NFC e internet. O [procedimento oficial](https://ajuda.stone.com.br/pt_BR/tapstone/vender-com-tapstone) descreve a venda dentro do aplicativo Stone. Nas páginas/buscas públicas oficiais consultadas em 13/11/2026, não foi encontrado contrato SDK/API/Intent/Deeplink TapStone para terceiros. Isso não exclui contrato privado. Os materiais Smart POS e APIs online não foram tratados como autorização TapStone. As fontes e o caminho para solicitar parceria/homologação estão no roteiro.

Não houve leitura EMV genérica, coleta de dados de cartão, abertura do provedor financeiro, cobrança ou publicação de APK comercial. A referência do comprovante e o operador permitem auditoria local, mas não comprovam recebimento independentemente do operador. Uso real depende de elegibilidade Stone, revisão/ensaio operacional no aparelho e autorização expressa para a transação de teste. **Não declarar o celular operacionalmente homologado nem o NFC real homologado com base apenas nesta entrega.**
