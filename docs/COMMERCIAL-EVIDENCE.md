# Evidências — candidata comercial 0.10.0-rc1

Data: **13/11/2026**. Branch: **`commercial/pix-nfc-voucher-release`**, criada sobre `3b45187070e86cdf75f9a8ecc36ac209a6742e27`, com árvore inicialmente limpa. Repositório: `David1Mariano/ruy-pos-system-PVD`.

**Implementação e validação local concluídas. Não representa produção financeira homologada, teste bancário real, certificação NFC ou release assinado para distribuição.**

## Implementado

1. Acabamento da experiência existente: valores em reais, nomenclatura legível, mensagens operacionais, alertas de pendência/envio mantidos e diagnósticos numa área protegida pelo bloqueio de tela do Android. Não se reconstruíram navegação, tema ou arquitetura do app.
2. Conta Pix em Configurações/Pagamentos: instituição, nome da conta, recebedor, CPF/CNPJ opcional, chave/tipo, cidade, estado da configuração, confirmação de registro no banco e auditoria da alteração. Formatos incluem CNPJ alfanumérico. Configuração usa Android Keystore/EncryptedSharedPreferences; não há fallback para preferências em claro.
3. Gerador desacoplado `payment/pix/`: TLV BR Code, moeda/país, valor exato, txid estático, normalização e CRC16; QR/copia e cola/copiar/compartilhar na variante comercial, somente com conta configurada. Homologação bloqueia exibição/cópia/compartilhamento de QR bancário e só admite conta de exemplo `.invalid`.
4. Pix emitido fica persistido como aguardando, sem receita no caixa/liberação de mesa. Snapshot protegido preserva os dados de um QR emitido mesmo se a conta for alterada. Conferência manual exige comprovante, valor, operador e declaração de conferência no banco. Não há aprovação automática.
5. TapStone assistido com mensagens de disponibilidade, abertura manual da Stone, débito/crédito e registro de parcelas efetivamente aprovadas. Valor/modalidade/parcelas conferidos precisam coincidir. A aprovação é manual auditada; trocar de aplicativo não confirma pagamento. Smart POS continua bloqueado.
6. Voucher externo habilitado somente mediante conferência explícita do credenciamento e configuração de provedor/bandeira/modalidades. Padrão desabilitado. O coordenador também aplica essa restrição; não depende apenas de esconder um botão. Histórico preserva provedor/bandeira da venda.
7. Idempotência e conferência: repetição de confirmação/operação, comprovante reutilizado inclusive com variação de maiúsculas/minúsculas, caixa fechado, pedido cancelado e saldo parcial são tratados transacionalmente. Referência completa do comprovante é obrigatória nos novos registros externos.
8. Caixa/relatórios distinguem dinheiro, Pix manual, Pix por API (categoria futura não habilitada), cartão externo, TapStone, voucher e simulado; pendentes/cancelados ficam separados. Ticket médio não duplica o denominador em pagamento dividido. PDF limita os dados à sessão de fechamento selecionada e trata históricos sem detalhamento de origem.
9. Segurança adicional: provável PAN numérico/control characters são recusados em campos de auditoria; callback legado continua com allowlist, sem persistir URI bruta/campos desconhecidos. Não há EMV genérico nem coleta de dados de cartão. Um token legado de sync só é removido da origem após gravação protegida bem-sucedida, em execução futura; nenhum token operacional foi lido nesta entrega.
10. Release separado da homologação, configuração de assinatura protegida por ambiente e procedimentos locais de geração/custódia/build, sem criar chave privada no Git ou usar debug como release definitivo.

## Testes executados

Comando final, concluído com `BUILD SUCCESSFUL`:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:testHomologationUnitTest :app:testReleaseUnitTest `
  :app:assembleHomologation :app:assembleRelease :app:bundleRelease `
  :app:assembleHomologationAndroidTest :app:lintHomologation :app:lintRelease --console=plain
```

| Variante | Casos | Aprovados | Skips | Falhas/erros |
|---|---:|---:|---:|---:|
| Debug | 100 | 98 | 2 | 0 |
| Homologação | 101 | 99 | 2 | 0 |
| Release | 100 | 98 | 2 | 0 |
| **Total de execuções** | **301** | **295** | **6** | **0** |

Há testes repetidos nas três variantes; não são 295 testes distintos. Os skips são os mesmos dois testes FileProvider por variante, incompatíveis com os caminhos Windows do host Robolectric. As contagens por suíte estão em [COMMERCIAL-TEST-RESULTS.json](COMMERCIAL-TEST-RESULTS.json).

Testes novos/estendidos executados verificam:

- Entrada monetária em reais/centavos exatos, rejeição de valores inválidos e limites de inteiro.
- Chaves/documentos válidos e inválidos, incluindo exemplo CNPJ alfanumérico oficial; confirmação obrigatória da configuração; persistência/auditoria em armazenamento **injetado de laboratório**, sem chave Pix no texto de auditoria.
- Exemplo BR Code do BCB com CRC `1D3D`, vetor CRC conhecido, valor de R$0,01/R$23,00, normalização, rejeição de corrupção/referência inválida e roundtrip QR encoder/decoder ZXing.
- Pix pendente sem caixa/mesa liberados, reabertura SQLite, retorno arbitrário sem baixa, comprovante/valor/pedido divergentes, confirmação concorrente/repetida, quitação parcial, alteração de conta sem alterar QR emitido, ausência de recebimento com declaração explícita e caixa fechado.
- Provedor Pix dinâmico indisponível e recusando operações; não existe endpoint de banco inventado nos testes.
- TapStone local: abandono, reinício, recibo divergente/ausente, pagamento repetido, parcelas de crédito divergentes, débito não parcelável e condições mínimas de NFC/Android/internet. Não é teste com cartão ou app Stone real.
- Voucher desabilitado/modalidade não autorizada, captura de bandeira/provedor/operador, operação repetida, falta de comprovante e caixa fechado. Variação de caixa da referência não permite contabilizar o mesmo recibo novamente.
- Fechamento misto com Pix, cartão, TapStone e voucher, sem aumentar dinheiro físico esperado; separação de recebidos/pendentes/cancelados e ticket médio por pedido recebido.
- Rejeição de número de cartão sintético em auditoria/callback; falha na gravação protegida de token preservando a cópia de origem, com preferências **injetadas de laboratório**, sem token real.
- Suíte anterior de Room, migração v7→v8, concorrência, pedidos/mesas/caixa, backup SQLite completo, imagens, restauração/reabertura, arquivo corrompido/ausente, traversal e rollback após interrupção injetada.

Lints homologação e release: **0 erros, 21 avisos em cada**. Builds APK de homologação, APK comercial sem assinatura, AAB sem assinatura e APK de instrumentação aprovados. Scripts PowerShell analisados sem erros sintáticos. Quatro guardas dos scripts de assinatura foram executadas e recusaram ação indevida antes de gerar chave/pedir senha. `git diff --check` sem erros.

## Payload Pix fictício validado

Chave **fictícia** `demo@example.invalid`, recebedor fictício, R$23,00, txid de teste. Esse exemplo não é conta configurada do proprietário, não é comprovante e não deve ser usado para recebimento. Nenhum aplicativo bancário foi aberto.

```text
00020126420014br.gov.bcb.pix0120demo@example.invalid520400005303986540523.005802BR5913LOJA FICTICIA6009SAO PAULO62150511FICTICIO1236304800B
```

CRC `800B`. Artefatos locais gerados pelos testes a partir do gerador real: `artifacts/commercial-candidate/pix-fixture/pix-ficticio-copia-cola.txt`, `pix-ficticio-validado.svg` e instrução de uso fictício. O teste codifica/decodifica o conteúdo e compara o payload. Não houve leitura desse QR em banco nem validação de registro no DICT.

## Bloqueios observados

- ADB sem aparelho conectado. O script seguro, direcionado exclusivamente a `emulator-5554`, parou no pré-check com **`device 'emulator-5554' not found`**, antes de instalar/executar testes. `emulator -accel-check` retornou código 6, driver de hypervisor ausente. Não se alteraram drivers nem serviços Windows. As tentativas anteriores por software já não haviam disponibilizado o Android.
- **Sete métodos instrumentados preparados/compilados, zero executados nesta etapa**: navegação/recriação de Activity, operação local/PDF/compartilhamento, permissões/sync, migração nativa e três ensaios novos de Android Keystore/conta Pix/token/Room. Os testes de dados usam bases/pastas/preferências de fixture. O ensaio de navegação abre a Activity da homologação e carrega suas tabelas; deve ser executado somente em instalação laboratorial dedicada, não na homologação existente que se deseja preservar.
- Criptografia real no Android Keystore, prompt de autenticação do aparelho, seletor de fotos, compartilhamento entre apps, geração/renderização/paginação nativa de PDF e inspeção visual no Samsung ainda precisam de ensaio no Android. Testes de preferências simuladas não comprovam criptografia nativa. Reabertura de SQLite local não equivale a reinício completo do processo Android.
- Alternância real da rede, NFC físico, aplicativo Stone instalado, elegibilidade/credenciamento, parcelas/bandeiras efetivamente disponíveis e pagamento/recusa real não foram testados. Os diagnósticos e controles locais não constituem certificação do provedor.
- Banco/conta real do proprietário não foram configurados. Não houve consulta de titularidade, chamada de API Pix, webhook, estorno real, pagamento, login D1 ou leitura/modificação de dados empresariais.
- Nenhum keystore comercial foi identificado na busca; nenhum novo foi gerado. O release permanece sem assinatura, por solicitação do proprietário. O procedimento seguro está em [COMMERCIAL-SIGNING.md](COMMERCIAL-SIGNING.md).
- Target SDK preservado em 34. O AAB não foi declarado pronto para submissão atual ao Google Play; são necessárias assinatura apropriada e atualização/QA do target exigido pelo canal. Nenhum artefato foi publicado ou distribuído como release definitivo.

## Dados, compatibilidade e segurança

O banco segue schema **8**, sem nova migração, sem `fallbackToDestructiveMigration`. Novas modalidades/auditorias usam estruturas existentes; o detalhamento do caixa é snapshot no campo de observação existente. Fotos e registros antigos permanecem nas estruturas anteriores. A migração v7→v8 com fixture passou nas três suítes. Migrações históricas anteriores sem fixture real não foram homologadas.

Não houve instalação/desinstalação, limpeza de dados, importação em banco operacional ou ensaio na T8. Nenhuma base da instalação principal ou homologação no aparelho foi acessada. A preservação física de antes/depois continua sem verificação em aparelho; a entrega não executou comandos sobre essas instalações.

Configurações e snapshots Pix protegidos não são exportados no backup Room/fotos, para não portar material de criptografia/segredos. Restaurar em outro aparelho exige reconfiguração protegida; um QR antigo indisponível não é reemitido silenciosamente para outra conta. Pendências precisam de conferência no banco.

Busca de padrões de chave privada/tokens conhecidos no código/scripts/docs sem ocorrências e nenhum keystore versionado. Isso é revisão de código/artefatos, não auditoria externa ou prova universal de ausência de segredo. Campos de operador são declaratórios; a trilha manual não é autenticação bancária ou identidade de sessão D1. Não se implementou automação financeira nem se coletaram senha bancária, PIN, CVV ou PAN por NFC.

## Versões, artefatos e hashes

| Artefato | Package / versão | Assinatura |
|---|---|---|
| `RUY-POS-0.10.0-rc1-homologacao-vc22.apk` | `br.com.ruy.pos.homologation` / `0.10.0-rc1-homologacao`, código 22 | Debug existente, verificação aprovada |
| `RUY-POS-0.10.0-rc1-COMERCIAL-UNSIGNED-NAO-INSTALAR.apk` | `br.com.ruy.pos` / `0.10.0-rc1`, código 22 | Ausente, candidato não instalável |
| `RUY-POS-0.10.0-rc1-COMERCIAL-UNSIGNED.aab` | Variante release, código 22 | Sem assinatura de upload |
| `RUY-POS-0.10.0-rc1-homologacao-tests.apk` | `br.com.ruy.pos.homologation.test` | Apenas instrumentação de laboratório |

Local: `artifacts/commercial-candidate/`, ignorado pelo Git. SHA-256 completos:

```text
8a2fe244cf897fcc261646d27044ee4fbdf0033f738de8237df7d24e19aad0e9  RUY-POS-0.10.0-rc1-homologacao-vc22.apk
9b6c3b3d133bf7a89fdcf655161d5bfbb3973b6169f17214503f0a6bb351fccf  RUY-POS-0.10.0-rc1-COMERCIAL-UNSIGNED-NAO-INSTALAR.apk
fd790385ad2ab0253e62a6bb34f115d680810565d042561630cd80158df51a6d  RUY-POS-0.10.0-rc1-COMERCIAL-UNSIGNED.aab
e3412537f295cca1389ce6b941eac44e7b810b2ca93daedbc8da183b88832ba4  RUY-POS-0.10.0-rc1-homologacao-tests.apk
```

Certificado SHA-256 da homologação/debug existente: `cc33982765d3f045a34a89f484016327da7afe39ed693233646511a19171c1ec`. Não é certificado comercial nem comprova a assinatura instalada no aparelho. O APK release falhou em verificação de assinatura **como esperado por estar sem assinatura**; não foi usado debug como certificado comercial. O AAB não contém blocos de assinatura JAR.

Logs locais: `build-commercial-delivery.log`, `build-commercial-instrumented.log`, `lint-results-*`, verificação de assinatura e guardas de assinatura na mesma pasta de artefatos.

## Histórico de commits e continuidade

1. `5e39e67` — acabamento e suporte protegido.
2. `f45dd68` — conta Pix e auditoria.
3. `c2d6849` — QR Pix, persistência e confirmação manual.
4. `33090f9` — NFC assistido e parcelas conferidas.
5. `83f948a` — voucher e idempotência externa.
6. `0829d09` — caixa/relatórios/histórico por origem.
7. Commit final desta entrega — segurança, testes, versão candidata, procedimento de assinatura e estas evidências (identificável por `git log`/mensagem).

O [roteiro operacional](COMMERCIAL-OPERATIONS.md) explica cada fluxo e seus ensaios manuais. Uso financeiro real continua dependendo de conta/chave registrada, credenciamento e aplicativos autorizados, conferência humana, dispositivo validado e assinatura/distribuição aprovadas. A API bancária dinâmica permanece explicitamente não habilitada. Não se deve declarar cobranças integradas prontas com base apenas na compilação ou nos testes locais.
