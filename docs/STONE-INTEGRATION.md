# Stone no Ruy POS — integração local e conciliação

Data da entrega: 04/11/2026. Versão Android: **0.9.2 (20)**. Banco Room: **8**.

**Não homologada no equipamento. Nenhuma cobrança real foi realizada.** Os testes são simulados com JUnit/Robolectric; não executam o aplicativo Stone, adquirente ou backend.

## Estado encontrado

- Branch `feature/premium-ui-redesign`, HEAD inicial `88dbf77`, sem commits locais à frente do upstream informado pelo Git.
- O código encontrado tinha `StonePaymentGateway.isAvailable() = false`, sem Intent de pagamento, filtro de retorno ou tratamento em `PosActivity`. A descrição de um deeplink funcional não correspondia aos fontes disponíveis.
- As alterações anteriores de Repository/DAOs para Caixa e status de comanda foram preservadas e incorporadas ao fluxo transacional.
- Caches, APKs, builds, logs e configuração local eram rastreados. O commit `8d70535` retira esses arquivos do índice e adiciona `.gitignore`, preservando os arquivos no disco. O histórico anterior não foi reescrito.
- A interface existente foi mantida, com controles de configuração, saldo parcial, recuperação e conciliação nos módulos atuais.

## Retorno e persistência

`PosActivity` recebe `ACTION_VIEW` em `onCreate` e `onNewIntent`. Manifest com `singleTask` e retorno `ruypos-stone://pay-response`. O processador valida scheme, host, caminho, parâmetros repetidos, limites e código numérico.

`PaymentCoordinator` busca a tentativa e seu pagamento/pedido no Room, confere referência, centavos, modalidade e parcelas e grava evidências. O retorno `0` com campos coerentes indica aprovação **informada**, encaminhada à conciliação. O código `2` informa encerramento do aplicativo; `-6` com `reason=1016` e `success=false` informa recusa. Outros erros, campos ausentes e divergências ficam indeterminados. Nenhum desses códigos gera recebimento automaticamente.

Erros sem referência não são atribuídos como resultado financeiro definitivo. Referências desconhecidas e endpoints inválidos são auditados sem baixar outro pedido. Callbacks equivalentes são deduplicados por SHA-256 de campos permitidos e ordenados. A auditoria descarta PAN, nome do portador e mensagens livres; a URI completa não é armazenada nem registrada em logs.

Checkout, lista de pedidos e movimentações do Caixa observam `Flow` do Room. O status financeiro não depende de Toast ou de variáveis em memória.

## Identificadores e prevenção de duplicidade

- UUID do pedido permanece interno. Cada tentativa tem seu próprio UUID e `PaymentEntity`.
- `stoneOrderId` é uma sequência SQLite `INTEGER PRIMARY KEY AUTOINCREMENT`, positiva e compatível com Long, associada ao pedido e à tentativa. Não há hash/truncamento de UUID. A unicidade é local ao banco persistido; exclusão dos dados/reinstalação exige revisão do histórico no terminal.
- Tentativa e pagamento são gravados **antes** da abertura do aplicativo Stone. A reivindicação do lançamento é atômica e só pode ocorrer uma vez.
- Índice único de `activeSlot` mantém uma tentativa em andamento por terminal. Cliques concorrentes e outros meios de pagamento também são bloqueados enquanto houver pendência. ATK tem controle de unicidade e conferência contra recebimentos anteriores.
- Falha comprovada de abertura encerra como `NAO_INICIADO`. Falha ambígua mantém bloqueio e conciliação. Uma falha tardia não sobrescreve callback já persistido.
- Ausência de retorno por dois minutos de relógio local marca resultado indeterminado, mantendo o bloqueio. Esse prazo é uma política do Ruy POS, sem inferir cancelamento Stone.
- Reiniciar ou interromper o app preserva pedido/pagamento/tentativa. A tentativa ativa é recuperada no checkout; também há acesso por Pedidos e pelo aviso de pagamento pendente. A recuperação nunca relança a cobrança automaticamente.

## Disponibilidade e Ajustes Stone

`<queries>` declara a Intent `payment-app://pay`, incluindo visibilidade no Android 11+. O gateway verifica um único handler e `resolveActivity()`, abre um componente explícito e trata `ActivityNotFoundException`, bloqueio de segurança e falha ambígua. Consulta sem resultado é descrita como resolução indisponível, incluindo possível visibilidade; não é prova de aplicativo inexistente.

O operador precisa confirmar em **Conferir Ajustes Stone**:

1. Uso de POS Android fornecido pela Stone com aplicativo Pagamento oficial.
2. ID do pedido habilitado no aplicativo Ajustes Stone.
3. Suporte Pix/QR e opções de parcelamento efetivamente disponíveis e acordadas, quando usados.

O padrão é bloqueado. Sem confirmação de ID do pedido, o gateway não envia a Intent. As confirmações são vinculadas ao modelo/firmware e precisam ser revistas após alterações nos Ajustes. Não existe consulta automática dessa opção no contrato de pagamento consultado; a confirmação do operador não é uma detecção por SDK nem homologação.

## Conciliação guiada

O deeplink usa um scheme customizado e não autentica o remetente. Por isso, nenhum callback sozinho é prova suficiente para liquidar uma transação.

O operador abre o histórico/comprovante/extrato Stone e reintroduz referência, valor, modalidade, parcelas e, para recebimento, ATK/Stone ID e autorização do cartão. Registra seu nome, identificador/data do documento consultado e declaração explícita de conferência. Não preencher a evidência com dados de cartão.

- **Recebido:** valida os dados contra o pagamento salvo, verifica ATK e saldo e registra `MANUAL_COMPROVANTE` com operador/documento.
- **Não recebido:** exige conferir ausência de recebimento **e** de transação em andamento no extrato antes de encerrar a tentativa e permitir outra cobrança.
- Apenas exibir QR Code, fechar o aplicativo, não receber callback ou atingir timeout não libera nova tentativa.

Essa conciliação é **manual**, baseada na declaração auditada do operador. Não foi criada uma API fictícia de consulta nem adicionadas credenciais. Uma futura conciliação online depende de serviço oficial/contrato, credenciais e ambiente definidos com a Stone; o `TransactionDAO` documentado é do SDK e não foi tratado como um endpoint deeplink acessível a este app.

Pagamentos pendentes de versões anteriores também bloqueiam cobranças. Em Pedidos → Retomar checkout → Recuperar pagamento anterior, eles podem ser conciliados sem relançamento. Sua nova referência é expressamente **local**, nunca presumida como enviada à Stone. O recebimento confirmado é regularizado no Caixa aberto, com aviso ao operador e auditoria da origem. Vínculos antigos de mesa/comanda ausentes não são inventados.

## Caixa e mesas

Após conciliação de recebimento, uma única transação Room:

1. Marca o pagamento como `PAGO` e vincula ATK ao pagamento/pedido.
2. Insere movimento `payment:<paymentId>` com chave única na sessão de Caixa capturada antes da cobrança.
3. Calcula o saldo com todos os pagamentos confirmados e atualiza pedido para `PARCIAL` ou `PAGO`.
4. Somente quando totalmente quitado, fecha a comanda vinculada e libera a mesa.

Recusa, cancelamento informado, retorno inválido, indeterminado ou ausência de recebimento não geram receita nem liberam mesa. Falhas de consistência fazem rollback de toda a baixa. O Caixa não pode fechar com tentativas/recebimentos pendentes; o fechamento é transacional e usa os movimentos confirmados, separando dinheiro, Pix, débito e crédito. Cartões/Pix não aumentam o saldo físico esperado em dinheiro.

`ensureTables()` agora insere apenas mesas inexistentes: reabrir o módulo não sobrescreve mesas ocupadas. Itens ficam protegidos durante uma tentativa; fora dela, um pedido de comanda ainda aberto pode receber itens com atualização atômica do total e dos preços históricos.

## Pix e crédito

São reconhecidos `CREDIT`, `DEBIT`, `VOUCHER`, `INSTANT_PAYMENT` e `PIX`, com aliases localizados no retorno. O checkout mantém Dinheiro, Pix, Débito e Crédito. `PIX` e `INSTANT_PAYMENT` não são considerados intercambiáveis automaticamente.

Valores seguem em centavos inteiros, positivos, com limite de 999999999 e edição desabilitada no terminal. Crédito à vista envia `NONE` e omite quantidade; crédito parcelado exige de 2 a 99 parcelas e `MERCHANT`/`ISSUER` explicitamente suportado. Os demais tipos não enviam parâmetros de crédito. Nenhuma taxa é inventada ou calculada no Ruy POS.

O comportamento Pix/QR do equipamento e sua diferença em relação ao QR de outros meios precisam ser conferidos com a Stone; não se presume que exibir um QR finalize pagamento.

## Verificação automatizada e build

Comando de validação final:

```powershell
.\gradlew.bat clean testDebugUnitTest assembleDebug --console=plain
git diff --check
git diff --cached --check
```

Resultado executado nesta entrega: **58 testes, zero falhas, zero erros e zero ignorados**. Build limpo concluído com `BUILD SUCCESSFUL` e 48 tarefas executadas. `git diff --check` e a conferência do índice passaram. APK verificado pelo `apksigner` com assinaturas v1/v2 válidas.

APK: `artifacts/RuyPOS-0.9.2-Stone-debug.apk` (17.428.180 bytes).

SHA-256:

```text
b1c7b9bd9f87494a43a6f4d9834a0524d5101c094c01c6e27dfd900a822ceea0
```

As cinco suítes `*SimulatedTest` cobrem protocolo/centavos/referências/modalidades, Intents e falhas de abertura, retornos frio/quente da Activity, migração 7→8 com validação completa do Room e dados preservados, idempotência/concorrência, morte simulada do processo por fechamento/reabertura de SQLite, timeout sem retorno, conciliação, Caixa contabilizado uma vez, proteção contra overflow no fechamento e quitação parcial/integral de mesa. Android 23 (fluxo de recebimentos/fechamento), 28 e 34 são simulados. Reabertura de banco não substitui ensaio real de interrupção do sistema operacional no POS.

O APK de debug e seu SHA-256 ficam em `artifacts/` e fora do Git. É um build de desenvolvimento; assinatura/distribuição específica do terminal depende do processo Stone. Relatório JUnit em `app/build/reports/tests/testDebugUnitTest/index.html`.

## Pendências de homologação no equipamento

Nenhum dispositivo foi listado por `adb devices -l` nesta sessão. Com autorização e ambiente de homologação, verificar:

- Modelo/firmware fornecido pela Stone, aplicativo Pagamento, assinatura aceita e instalação do APK.
- ID do pedido habilitado de fato nos Ajustes e sua presença/eco no retorno e no comprovante.
- Resolver/abrir a Intent no Android do terminal e conferir os parâmetros reais recebidos.
- Débito, crédito à vista, parcelamentos contratados e fluxo específico Pix/QR.
- Comprovantes/ATK/autorização e eventuais nomes/formatos de retorno diferentes dos exemplos documentados.
- Aprovação, recusa, encerramento, callback repetido, interrupção/reinício, retorno ausente, conciliação antes de retry e ausência de dupla cobrança.
- Saldo parcial, vínculo ao pedido, movimento único no Caixa, fechamento correto e mesa liberada somente na quitação.
- Conferência dos registros legados sem associações de mesa e definição operacional de quem pode atestar conciliação manual.

## Fontes oficiais consultadas

- [Pagamento por deeplink Stone](https://sdkandroid.stone.com.br/reference/pagamento-deeplink)
- [FAQ Stone: ID do pedido, crédito e QR Code](https://sdkandroid.stone.com.br/page/deeplink)
- [Disponibilidade do deeplink em POS fornecidos pela Stone](https://sdkandroid.stone.com.br/reference/explicacao-deeplink)
- [Pré-requisitos de terminais e distribuição](https://sdkandroid.stone.com.br/reference/pre-requisitos)
- [TransactionDAO do SDK](https://sdkandroid.stone.com.br/reference/transaction-dao)
- [Android: visibilidade de pacotes](https://developer.android.com/training/package-visibility)
- [Android: queries no Manifest](https://developer.android.com/guide/topics/manifest/queries-element)
