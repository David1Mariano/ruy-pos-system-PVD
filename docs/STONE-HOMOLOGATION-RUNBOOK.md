# Ruy POS 0.9.2 — roteiro operacional Stone

Auditoria de 04/11/2026, horário de Brasília/São Paulo. Escopo: preparar instalação e ensaios autorizados de deeplink, retorno, conciliação, Caixa e mesas. **Situação: bloqueado para liberação; não homologado.** APK e interface premium preservados. Nenhum comando de instalação ou transação financeira foi executado nesta preparação.

## 1. Identificar exatamente o candidato e a origem

| Item | Evidência conferida |
| --- | --- |
| Repositório | `https://github.com/David1Mariano/ruy-pos-system-PVD.git` |
| Branch | `feature/premium-ui-redesign` |
| Implementação consultada no remoto | `e0b08df8df54fa23f44ef394d4f39c84d6c27ae1` |
| Commits da entrega anterior | `8d70535` e `e0b08df`, ambos contidos no branch |
| APK existente, sem novo build | `artifacts/RuyPOS-0.9.2-Stone-debug.apk` |
| Package / Activity inicial | `br.com.ruy.pos` / `.ui.activity.PosActivity` |
| VersionName / versionCode | `0.9.2` / `20` |
| minSdk / targetSdk | `23` / `34` |
| Tamanho | 17.428.180 bytes |
| Assinaturas verificadas | v1 e v2 válidas; um signatário; `apksigner` retornou 0 |
| Certificado | `C=US, O=Android, CN=Android Debug`, RSA 2048 bits |
| SHA-256 do certificado | `cc33982765d3f045a34a89f484016327da7afe39ed693233646511a19171c1ec` |
| SHA-256 do APK | `b1c7b9bd9f87494a43a6f4d9834a0524d5101c094c01c6e27dfd900a822ceea0` |

A consulta `git ls-remote origin refs/heads/feature/premium-ui-redesign` confirmou a publicação da implementação. Os documentos/preflight desta preparação são publicados em commit posterior, sem mudar o APK. Um APK re-assinado terá outro SHA-256: anexar seu novo certificado, hash e aprovação ao registro, sem sobrescrever a identificação do candidato atual.

## 2. Bloqueios concretos e responsáveis pela resolução

| ID | Situação constatada | Evidência necessária para fechar |
| --- | --- | --- |
| B01 | ADB não listou dispositivos. Modelo, Android, firmware e identificação do terminal não informados. | Operador identifica terminal Stone autorizado; inventário com fabricante/modelo, Android/API, firmware, serial em registro restrito e comprovante de vínculo ao ambiente de testes. |
| B02 | APK assinado com Android Debug; aceitação no equipamento não demonstrada. | Equipe Stone confirma fluxo de assinatura/instalação para esse modelo e fornece/aceita o artefato correspondente. Manter identidade de assinatura em atualizações. |
| B03 | **Manifest final contém `android.permission.WAKE_LOCK` e `android.permission.RECEIVE_BOOT_COMPLETED`.** A lista oficial Stone as proíbe. | Tratar compatibilidade do WorkManager/sincronização e entregar posteriormente artefato adequado, com nova inspeção do Manifest final e validação Stone. Não basta conferir o Manifest fonte. |
| B04 | APK permite HTTP em `127.0.0.1`, `localhost`, `192.168.1.5`, `10.0.2.2` e `pos-hml.ruycaldodecana.com.br`. | Definir/remover exceções no artefato de homologação conforme exigência Stone; evidenciar HTTPS/TLS e whitelist das URLs realmente utilizadas. `SyncClient` exigir HTTPS não elimina essas permissões de rede do APK. |
| B05 | Ajustes Stone, ID do pedido e app Pagamento não foram observados. | Captura da opção habilitada, versão/origem do app oficial, resolução da Intent e retorno/comprovante com referência compatível. |
| B06 | Não há autorização específica de ensaios financeiros nem valor/limite/plano aprovado. | Responsável registra ambiente, casos autorizados, valores em centavos, cartões/simuladores de teste aprovados, responsável e janela de execução. Teste que liquide dinheiro real exige autorização explícita própria. |
| B07 | Conciliação é manual; não há consulta autenticada online à Stone nem controle de perfil/PIN para atestar essa conferência. | Responsável operacional e Stone aceitam o procedimento e definem quem pode executá-lo, evidência independente e controle de acesso ao terminal; se exigirem verificação online/perfis no software, abrir alteração separada antes de liberar. |
| B08 | Encerrar o fluxo de pagamento não equivale a estornar uma venda paga. Não há deeplink de estorno nem compensação no Caixa nesta versão. | Escopo assinado distingue abandono antes da confirmação de estorno financeiro. Se estorno integrado for requisito de liberação, permanece bloqueado até implementação e teste próprios. |
| B09 | Pré-requisitos gerais citam seção de contato/suporte. Não foi localizada seção explícita de “Fale Conosco” nos fontes ativos. | Conferir exigência no processo aplicável com a Stone e demonstrar atendimento. Qualquer alteração de interface fica para trabalho separado, preservando esta entrega. |

B03 é sustentado por `aapt dump badging` do APK e pelo relatório `app/build/outputs/logs/manifest-merger-debug-report.txt`: ambas as permissões vêm de `androidx.work:work-runtime:2.9.0`. A seção de permissões proibidas é publicada em [Boas Práticas e Segurança Stone](https://sdkandroid.stone.com.br/reference/boas-pr%C3%A1ticas-e-seguran%C3%A7a). O arquivo `res/xml/network_security_config.xml` empacotado confirma B04. **Não foram alterados fontes Android, dependências, Manifest ou interface para contornar esses bloqueios.**

## 3. Distinguir ambiente, instalação e autorização financeira

Preencher no registro de execução antes de tocar no fluxo de pagamento:

| Campo obrigatório | Situação desta auditoria |
| --- | --- |
| Fabricante/modelo, Android/API e firmware | Não informado/não coletado |
| Serial e responsável pelo terminal | Não informado; guardar em evidência restrita |
| Terminal de homologação fornecido/autorizado pela Stone | Não comprovado |
| Vínculo serial ↔ StoneCode de testes e ativação | Não comprovado |
| Versões dos aplicativos Stone e canal de origem | Não coletadas |
| Protocolo Stone / responsável técnico / instalador autorizado | Pendente |
| Autorização de instalar neste equipamento | Pendente de evidência específica do terminal |
| Plano financeiro, valores, limites e instrumentos de teste | Não autorizado nesta sessão |
| APK/certificado aceitos para o modelo | Pendente |

Instalar o app não autoriza uma cobrança. Terminal de testes também não é prova automática de que uma operação não liquida valores. Confirmar isso com o responsável Stone e usar apenas instrumentos/cenários fornecidos para o ambiente. Não usar um StoneCode encontrado em exemplos da internet sem vínculo validado pela equipe de integrações. A [FAQ de POS Stone](https://sdkandroid.stone.com.br/page/pos-android) trata associação e assinaturas; a confirmação do fornecedor deve ficar anexada ao processo.

## 4. Conferir o que depende do equipamento e do contrato

| Recurso | Dependência a verificar | Evidência de aceite |
| --- | --- | --- |
| Instalação/assinatura | Fabricante, modelo, firmware, política de distribuição e certificado | APK aceito pelo canal oficial e certificado registrado |
| Android e visibilidade | API real, versão do app Pagamento e resolução de `payment-app://pay`; atenção ao Android 11+ | Inventário, query/resolve e mensagem do próprio Ruy POS |
| Chip/NFC/tarja | Leitor, firmware e métodos habilitados no terminal/cartão de testes | Resultado por modo autorizado; não generalizar um ensaio para outro modo |
| Crédito e parcelas | Habilitação comercial, contrato e suporte do app Stone; não depende apenas do hardware | Opção disponível, `NONE`/`MERCHANT`/`ISSUER`, parcelas e comprovante conferidos |
| Pix | StoneCode/estabelecimento habilitado, ambiente, app Pagamento, conectividade e versão | QR correto, confirmação no histórico Stone e tipo de retorno observado |
| QR de cartão vs Pix | Fluxo da versão instalada; QR é meio de apresentação, sem equivalência automática entre enums | Registrar tipo enviado, método escolhido na Stone e tipo retornado |
| Retorno e recuperação | Modelo/firmware, gestão de tarefas e processos, Activity e versão Stone | Retorno quente/frio, ausência/duplicação e recuperação no equipamento |
| Comprovantes | Impressora/app oficial/versão; impressão customizada do Ruy não foi implementada | Referência, ATK, valor e autorização acessíveis ao operador |
| Rede | Wi-Fi/chip Stone, HTTPS/TLS e whitelist aplicável | Conectividade aprovada; sem compartilhar tokens ou senhas |

Os [pré-requisitos Stone](https://sdkandroid.stone.com.br/reference/pre-requisitos) citam, como exemplos, GPOS700X/Android 8.1, L400/11, L300/7.1 e Sunmi P2-B/7.1. Essa lista não substitui a leitura do terminal concreto. O [processo de assinatura/build por fabricante](https://sdkandroid.stone.com.br/reference/processo-de-build-de-aplica%C3%A7%C3%A3o) precisa ser confirmado para a integração exclusivamente por deeplink; não adicionar SDK/providers ou flavors por analogia nesta preparação.

## 5. Preflight somente de leitura

Na raiz do repositório, executar em PowerShell:

```powershell
# Não instala nem abre aplicativos; saída 2 significa bloqueado/inconclusivo.
powershell -NoProfile -File .\scripts\Test-StoneTerminal.ps1

# Depois de identificar o serial exato do terminal autorizado:
powershell -NoProfile -File .\scripts\Test-StoneTerminal.ps1 -Serial SERIAL_AUTORIZADO
```

Se o SDK estiver em outro local, usar `-AdbPath C:\caminho\platform-tools\adb.exe`. O relatório padrão é `artifacts/stone-homologacao/preflight.json`, ignorado pelo Git. Preservar cópias datadas no dossiê restrito antes de nova execução.

O script coleta inventário, propriedades de fabricante/modelo/Android/firmware/ABI e, quando suportadas, consultas de resolução de Activities. Ele não usa `am start` para `payment-app`, não instala APKs, não muda configurações e não envia pagamentos. `unauthorized`/`offline`, comando indisponível ou consulta inconclusiva são bloqueios de observação; não significam aplicativo inexistente. Não tentar habilitar ADB em equipamento cuja política não o permita.

Uma Activity encontrada pela shell é apenas candidata. O operador ainda verifica a origem oficial do pacote e sua versão em Informações do aplicativo; depois verifica o `isAvailable()` no contexto do Ruy POS. Confirmar que existe um único handler compatível, que o retorno `ruypos-stone://pay-response` pertence ao Ruy e que as declarações de terminal/ID estão preenchidas com base em observação real. O script mantém saída bloqueada para o candidato 0.9.2 enquanto existirem os bloqueios documentados e não concede autorização financeira.

Resultado executado nesta preparação: `NO_TERMINAL_CONNECTED`, zero dispositivos, saída 2 esperada. Sintaxe PowerShell validada. Os caminhos que consultam hardware **não foram executados** nesta sessão. O teste auxiliar abaixo passou em cinco cenários **simulados**, com respostas de ADB fictício: nenhum terminal, terminal não autorizado, handler único, múltiplos handlers e consulta indisponível. Esse teste não usa ADB real nem se soma aos 58 testes da aplicação.

```powershell
powershell -NoProfile -File .\scripts\Test-StoneTerminal.SimulatedTests.ps1
```

## 6. Preparar assinatura e instalação pelo canal autorizado

1. Enviar ao processo oficial Stone os metadados do candidato e o modelo/firmware, e solicitar a rota aplicável de assinatura/instalação. Nenhum envio a terceiros foi feito nesta sessão.
2. Resolver B02–B04 e confirmar os demais critérios de entrada. Uma assinatura Android válida não implica confiança do terminal. Material privado de assinatura é tratado apenas no canal acordado; não inserir JKS/senhas no Git, no roteiro ou no dossiê público.
3. Registrar o SHA-256 e o certificado do **artefato efetivamente aprovado**. Caso seja outro APK, usar outro nome e preservar o atual; reconferir package, versão, permissões e configuração de rede. Se houver novo build/submissão, seguir o [versionamento exigido pela Stone](https://sdkandroid.stone.com.br/reference/especifica%C3%A7%C3%B5es-de-version-name).
4. No terminal, conferir se `br.com.ruy.pos` já está instalado. Não desinstalar, limpar dados, fazer downgrade nem trocar certificado para contornar erro: isso pode apagar pedidos/tentativas e reiniciar referências numéricas. Conciliar as pendências e combinar preservação/migração de dados antes da atualização.
5. Preferir o instalador/distribuição indicados pela Stone. Em POS de produção, USB/ADB pode estar bloqueado; não tentar contornar a restrição.

**Comandos preparados, não executados.** Utilizar somente em terminal de desenvolvimento/homologação cujo ADB esteja autorizado, com artefato já aceito para esse modelo:

```powershell
$adbExe = Join-Path $env:LOCALAPPDATA 'Android/Sdk/platform-tools/adb.exe'
$terminalSerial = 'SERIAL_AUTORIZADO'
$approvedApk = (Resolve-Path '.\artifacts\ARTEFATO_APROVADO_PELA_STONE.apk').Path
$approvedHash = 'SHA256_FORNECIDO_NO_PROCESSO_DE_APROVACAO'
if ((Get-FileHash -LiteralPath $approvedApk -Algorithm SHA256).Hash -ine $approvedHash) {
    throw 'SHA-256 diferente do artefato aprovado'
}

& $adbExe -s $terminalSerial shell pm path br.com.ruy.pos
# Conferir versão/certificado já instalados antes de prosseguir.
& $adbExe -s $terminalSerial install -r $approvedApk
if ($LASTEXITCODE -ne 0) { throw 'Instalação rejeitada: registrar erro e acionar o responsável Stone' }
& $adbExe -s $terminalSerial shell dumpsys package br.com.ruy.pos
# Abrir apenas o Ruy POS, sem URI de pagamento:
& $adbExe -s $terminalSerial shell am start -n br.com.ruy.pos/.ui.activity.PosActivity
```

Parar em `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, `INSTALL_FAILED_VERSION_DOWNGRADE`, restrição de usuário, assinatura não aceita ou perda de dados. Não usar `uninstall`, `pm clear`, flags de downgrade ou alterações em apps internos Stone como solução genérica. Conferir package/versão instalados, integridade dos pedidos/tentativas e funcionamento da interface existente. O procedimento ADB é descrito no [Android Developers](https://developer.android.com/tools/adb); a política do POS continua sendo a definida pela Stone.

## 7. Ajustes Stone e disponibilidade do pagamento

| Conferência no equipamento | Ação do operador | Registro esperado |
| --- | --- | --- |
| Terminal/ambiente ativo | Conferir o vínculo de testes e ativação, sem trocar dados de estabelecimento por tentativa | Modelo/firmware, protocolo Stone, tela de ambiente sem credenciais |
| Apps internos Stone | Conferir conjunto e versão fornecidos pela Stone, incluindo Pagamento e Ajustes | Nome, package, versão, origem oficial e responsável |
| ID do pedido | No app Ajustes Stone, localizar e habilitar a funcionalidade de ID do pedido | Captura da opção e caminho real nessa versão; não inventar submenu |
| Parcelação | Conferir o que está habilitado e acordado com Stone/estabelecimento | `NONE` à vista; `MERCHANT`/`ISSUER` apenas quando suportados e contratados |
| Pix/QR | Conferir habilitação e fluxo da versão e ambiente com a Stone | Protocolo e evidência do ensaio específico |
| Declarações no Ruy | Checkout eletrônico → “Conferir Ajustes Stone”; marcar apenas opções observadas | Registro de quem conferiu, data e firmware |
| Disponibilidade real | Observar resolução e mensagem do Ruy; erros são investigados antes da cobrança | Um handler oficial resolvido, sem erro de abertura ou validação |

O código não lê automaticamente o estado interno do Ajustes Stone. As caixas do Ruy são declarações do operador e não habilitam funções na Stone. Sem ID confirmado, o gateway bloqueia o envio. A [documentação de pagamento](https://sdkandroid.stone.com.br/reference/pagamento-deeplink) define a referência numérica e os parâmetros; a [configuração oficial do deeplink](https://sdkandroid.stone.com.br/reference/configuracao-deeplink) exige os aplicativos Stone no POS. Se faltarem, solicitar o pacote oficial à equipe de parcerias.

Registrar eco/presença da referência numérica no retorno e/ou comprovante. Se faltar referência ou houver tipo/valor divergente, o retorno não deve comprovar recebimento; manter conciliação pendente até existir correlação independente suficiente.

Para Pix, usar o fluxo `PIX` implementado no checkout e confirmar o comportamento real. A [FAQ Stone sobre QR](https://sdkandroid.stone.com.br/page/deeplink) distingue QR de cartão e Pix e relata mudanças do uso histórico de `INSTANT_PAYMENT`. Não alterar modalidade ou interpretar enums como equivalentes só para obter aprovação.

## 8. Matriz de ensaios preparados — nenhum executado no hardware

Todos os casos abaixo começam como **NÃO EXECUTADO**. Usar uma ficha por combinação de modelo, firmware, app Stone, certificado e ambiente. Casos com transação exigem B06 fechado; os valores são definidos no plano autorizado, sem adotar automaticamente valores mínimos ou cartões reais.

| Caso | Natureza e execução planejada | Resultado esperado e evidência |
| --- | --- | --- |
| H01 — instalação/abertura | Hardware, sem cobrança; instalar candidato aceito e abrir Ruy | APK/certificado/package/versão corretos, dados preservados e telas premium navegáveis; log de instalação e capturas |
| H02 — resolução/Ajustes | Hardware, sem cobrança; preflight, observar app Stone e ID do pedido | Origem/versão oficiais, handler único e ID habilitado; relatório e captura |
| H03 — débito | Hardware, transação de teste autorizada; pedido/comanda e Caixa abertos, um clique | Tentativa salva antes da abertura, mesmo valor/ref/método no retorno; nenhuma receita antes de conciliar; comprovante/ATK e estados do Room |
| H04 — crédito à vista | Hardware, transação autorizada; uma parcela | `CREDIT`, `NONE`, sem contagem de parcelas enviada; valor e autorização conferidos |
| H05 — crédito parcelado | Hardware, transação autorizada; somente opção comercialmente habilitada | `MERCHANT` ou `ISSUER` acordado, contagem entre 2 e 99; comprovante coerente; nenhuma taxa calculada pelo Ruy |
| H06 — Pix | Hardware, transação autorizada; usar dispositivo/simulador de pagador fornecido para o ambiente | QR por si só não produz `PAGO`; confirmar recebimento no histórico Stone, ATK e método retornado; registrar incompatibilidades |
| H07 — recusa | Hardware, cenário de recusa disponibilizado pela Stone | Zero movimento de recebimento, mesa ocupada e bloqueio de retry até conferência; retorno real e extrato |
| H08 — encerramento antes de autorizar | Hardware, fluxo autorizado; sair da Stone antes da confirmação | `code=2` quando entregue ou estado indeterminado quando sem correlação; zero receita, sem liberação de mesa e retry somente após extrato conferido |
| H09 — duplicidade | **Injeção simulada no hardware**, apenas laboratório e tentativa de teste; reentregar callback permitido da mesma tentativa, sem abrir URI `payment-app` | Zero movimentos adicionais; após baixa conciliada, exatamente um movimento para o paymentId. Registrar que foi injeção e não duplicação espontânea Stone |
| H10 — interrupção do Ruy | Hardware, ensaio autorizado em momento acordado; interromper somente Ruy após tentativa persistida e depois reabrir | Mesmo pedido/paymentId/attemptId/referência; nenhuma cobrança relançada; conciliar com histórico Stone antes de retry. Não encerrar app Stone para simular retomada do Ruy |
| H11 — sem retorno | Hardware/laboratório autorizado; cenário de retorno perdido combinado com Stone | Sem baixa; tentativa persistente e bloqueada. Ao retomar, após limiar de 2 min e verificação em primeiro plano a cada 30 s, estado indeterminado; conferir extrato |
| H12 — retorno inválido/divergente | **Injeção simulada no hardware**, em dados de laboratório isolados | Endpoint desconhecido, valor/ref/método/parcelas divergentes não geram receita; evento/estado documentados |
| H13 — saldo parcial e quitação | Hardware, transações de teste autorizadas; pagar parte e depois saldo | Parcial mantém comanda/mesa; quitação integral fecha/libera; dois pagamentos distintos, um movimento por pagamento |
| H14 — Caixa | Hardware, após conciliações; conferir e fechar sessão | Total por método correto, dinheiro físico exclui Pix/cartão, nenhuma baixa duplicada e bloqueio de fechamento com pendência |
| H15 — cancelamento/estorno de venda paga | **Não implementado no Ruy 0.9.2**, fora da aprovação atual | Não declarar coberto por H08. Se exigido, bloquear liberação até fluxo de cancelamento e compensação financeira próprios. Eventual operação no app Stone precisa de autorização financeira específica e não atualiza automaticamente o Caixa Ruy |

Para H09/H12, manter valores/identificadores fictícios ou dados permitidos de transação de homologação. Não fabricar callback para “validar” um recebimento inexistente nem escolher “Recebido” com base nessa injeção. Separar resultados de callbacks naturais e de injeções.

O [cancelamento oficial Stone](https://sdkandroid.stone.com.br/reference/cancelamento-deeplink) usa contrato próprio. O APK atual só registra retorno `pay-response`; não implementa esse contrato. Encerramento, recusa e ausência de retorno também não provam que uma venda autorizada foi estornada.

## 9. Conferir a conciliação antes de PAGO/Caixa

Para cada tentativa autorizada, manter operador, horário com fuso, pedido UUID, referência Stone, attemptId, paymentId, sessão de Caixa e método/parcelas/valor em centavos.

1. No retorno, conferir que não há `PAGO` nem movimento de recebimento automático. Mesmo um `code=0` coerente deve permanecer em conciliação. Mensagem/Toast não é comprovante.
2. No aplicativo Stone, localizar a operação pelo conjunto referência, valor, modalidade, horário e ATK. Para cartões, conferir autorização. Um ATK isolado também pode identificar transação recusada; confirmar o **status de recebimento** no histórico/comprovante.
3. Somente com evidência independente suficiente, no Ruy usar “Conferir comprovante/extrato e conciliar”: reintroduzir dados, identificar operador/documento e marcar a declaração de conferência. Os dados do callback não devem ser a única fonte.
4. Se o extrato comprovar ausência de recebimento e ausência de transação em andamento, usar “Não recebido” e registrar a evidência antes de nova tentativa. Em divergência ou dúvida, manter pendente e acionar Stone; não trocar modalidade, valor ou referência para forçar baixa.
5. Conferir que pagamento/pedido/ATK e movimento foram atualizados uma vez na sessão correta. Testar nova entrega do retorno e da conciliação: nenhum movimento adicional.
6. Conferir saldo parcial/integral e estado da comanda/mesa. Não liberar mesa manualmente para contornar o bloqueio. Pendências legadas sem vínculo não devem ser associadas por suposição; sua regularização no Caixa atual é explicitamente identificada.

Para inspeção autorizada de SQLite, usar ferramenta compatível com Android/terminal e captura consistente do banco. Não copiar só o `.db` aberto ignorando WAL, nem editar registros. Se a ferramenta não estiver disponível, marcar a evidência de banco como pendente, sem substituí-la por Toast. Consultas de leitura úteis, adaptando os identificadores ao caso:

```sql
SELECT stoneOrderId, attemptId, paymentId, orderId, state, activeSlot, origin,
       atk, reconciledBy, reconciliationEvidence FROM stone_attempts
WHERE stoneOrderId = :referencia;
SELECT paymentId, orderId, amountCents, method, installments, status,
       transactionRef, cashSessionId FROM payments WHERE paymentId = :paymentId;
SELECT movementId, sessionId, type, amountCents, orderId FROM cash_movements
WHERE movementId = :movementId; -- valor: payment:<paymentId>
SELECT id, totalCents, paymentStatus, stoneTransactionId, comandaId
FROM orders WHERE id = :orderId;
SELECT comandaId, tableId, status, totalCents FROM table_orders WHERE comandaId = :comandaId;
SELECT tableId, status FROM tables WHERE tableId = :tableId;
SELECT fingerprint, attemptId, result, createdAt FROM stone_callback_events WHERE attemptId = :attemptId;
```

O banco local confirma o comportamento do Ruy; não substitui a confirmação do recebimento na Stone. `MANUAL_COMPROVANTE` identifica atestação do operador; não é consulta online autenticada ao adquirente. A conciliação valida valor/modalidade/quantidade de parcelas; `MERCHANT`/`ISSUER` é conferido operacionalmente no pedido/contrato/comprovante, sem um campo específico dessa escolha no formulário de conciliação atual.

## 10. Dossiê e critérios de liberação

Organizar por caso em armazenamento restrito, com data/modelo/firmware/versão/certificado. Exemplo local ignorado pelo Git: `artifacts/stone-homologacao/`. Guardar metadados do APK, preflight, instalação, capturas de Ajustes, autorização do plano, resultado Stone, extratos/comprovantes permitidos e consulta de estados/movimentos. Não guardar PAN completo, trilhas, PIN, tokens, senhas, chaves privadas ou URIs brutas de clientes. Mascarar dados pessoais também nas capturas.

Evidências locais produzidas nesta preparação: `audit.json`, `apk-badging.txt`, `apk-signature.txt`, `apk-manifest-tree.txt`, `apk-network-security-tree.txt`, `adb-devices.txt`, `remote-head-at-audit.txt` e `preflight.json` nessa pasta. `simulated-preflight/` contém somente fixtures e resultados fictícios do teste auxiliar. Esses arquivos locais não são publicados no Git; o roteiro e o relatório de integração registram os achados que podem ser compartilhados.

Modelo de ficha por caso:

```text
Caso / execução / data-hora-fuso:
Natureza: SIMULADO JVM | HARDWARE SEM TRANSAÇÃO | HARDWARE TRANSAÇÃO AUTORIZADA | INJEÇÃO SIMULADA NO HARDWARE
Modelo / Android / firmware / versão app Stone / referência restrita do serial:
APK SHA-256 / certificado SHA-256 / fonte de distribuição:
Ambiente / autorização / responsável / valor autorizado em centavos:
Pedido / tentativa / referência Stone / paymentId / sessão Caixa:
Resultado esperado / observado / aprovado-reprovado-bloqueado:
Evidência Stone e status de recebimento / ATK permitido:
Antes/depois: pagamento / quantidade de movimentos / saldo / comanda / mesa:
Arquivo das evidências / conferente / pendências / decisão:
```

| Etapa de liberação | Critério objetivo | Situação atual |
| --- | --- | --- |
| Integridade/rastreabilidade local | Commit remoto, package/versão/hash/certificado conferidos | Concluída |
| Testes automatizados da implementação | XML de cinco suítes, 58 testes, zero falhas/erros/ignorados | Concluída em ambiente simulado; relatórios auditados nesta preparação, sem reexecução |
| Política do artefato | Assinatura aceita, permissões/configuração de rede adequadas, requisitos gerais atendidos | **Bloqueada: B02–B04 e B09** |
| Terminal/ambiente | Modelo/vínculo/apps/Ajustes/ID comprovados | **Bloqueada: B01/B05** |
| Plano financeiro | Responsáveis, autorização, valores e instrumentos de teste aprovados | **Bloqueada: B06** |
| Operação e conciliação | Política manual aceita e todos os casos exigidos aprovados por modelo/firmware | **Pendente: B07; zero ensaios no hardware** |
| Escopo de estorno | Exclusão formal do escopo ou fluxo integrado implementado e testado se obrigatório | **Pendente: B08** |
| Homologação Stone | Aceite formal do processo Stone e do responsável Ruy para o artefato exato e escopo testado | **Não concedida** |

Falha ou evidência ausente mantém a etapa bloqueada. A presença dos commits no Git, os 58 testes simulados e uma assinatura criptograficamente válida não equivalem a homologação do terminal. Corrigir o artefato no futuro exigirá identificar novo APK/hash e repetir a validação afetada; esta preparação não fez essa alteração.

## Referências oficiais e precedência

Fontes consultadas em 04/11/2026: [pagamento](https://sdkandroid.stone.com.br/reference/pagamento-deeplink), [configuração](https://sdkandroid.stone.com.br/reference/configuracao-deeplink), [FAQ QR/ID do pedido](https://sdkandroid.stone.com.br/page/deeplink), [pré-requisitos](https://sdkandroid.stone.com.br/reference/pre-requisitos), [assinatura por fabricante](https://sdkandroid.stone.com.br/reference/processo-de-build-de-aplica%C3%A7%C3%A3o), [FAQ POS](https://sdkandroid.stone.com.br/page/pos-android), [segurança/permissões](https://sdkandroid.stone.com.br/reference/boas-pr%C3%A1ticas-e-seguran%C3%A7a), [cancelamento](https://sdkandroid.stone.com.br/reference/cancelamento-deeplink), [ADB](https://developer.android.com/tools/adb) e [apksigner](https://developer.android.com/tools/apksigner).

Algumas páginas gerais descrevem SDK/providers, enquanto a FAQ e a página de inicialização tratam deeplink separadamente. Dúvidas de assinatura, flavors, distribuição, cancelamento obrigatório e aceite da conciliação manual devem ser fechadas com a equipe Stone para o modelo e contrato concretos; não assumir que um exemplo de outra integração autoriza este candidato.
