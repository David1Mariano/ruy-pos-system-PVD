# RUY POS — homologação Android e aproximação

Não representa homologação de pagamento real. Use apenas dados fictícios no APK de homologação. Nunca instale nem teste na Stone T8 comercial.

## Aparelhos e ambientes

O app mantém minSdk 23 (Android 6), sem restrição por fabricante ou modelo. Isso é compatibilidade declarada de instalação, não certificação em todos os celulares. TapStone exige Android 10+, hardware NFC, NFC habilitado, internet e aplicativo/conta Stone autorizados. Dispositivos sem esses requisitos continuam com os fluxos locais e pagamentos externos. iPhone não executa este APK Android.

| Ambiente | Package | Rede/sincronização | Pagamento |
|---|---|---|---|
| Operacional, preservado | `br.com.ruy.pos` | Configuração existente | Dinheiro, confirmação externa e TapStone assistido; Smart POS bloqueado |
| Homologação | `br.com.ruy.pos.homologation` | Sem permissão INTERNET; envio/worker bloqueados | Dinheiro fictício e SIMULADO; cobrança TapStone desabilitada |

A variante usa o mesmo schema Room v8 em sandbox Android distinto, FileProvider e callback próprios. Não há migração nova nem fallback destrutivo. O banco operacional não é copiado para homologação. Categorias Salgados/Bebidas, Coxinha 800 centavos, Caldo de Cana 700 centavos, Mesa 05 e caixa fictício de 10000 centavos são criados uma vez. O caixa inicial já vem aberto; após fechar, a abertura seguinte é manual. Há faixa permanente e identificação nos PDFs. Pedidos fictícios recebem `syncStatus=HOMOLOGATION`.

## Executar os testes

Na raiz, com JDK 17 e Android SDK:

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:testHomologationUnitTest
.\gradlew.bat :app:assembleHomologation :app:assembleHomologationAndroidTest :app:lintHomologation
```

Com emulador pronto, use um serial explícito. O script instala somente os packages de homologação e não desinstala nem limpa dados:

```powershell
.\scripts\Test-AndroidHomologation.ps1 -Serial emulator-5554
# Ou um celular de testes com a variante isolada (nunca a T8):
.\scripts\Test-AndroidHomologation.ps1 -Serial SERIAL_DO_CELULAR -IsolatedTestPhone
```

Confira o resultado final `OK`/`FAILURES` da instrumentação. Não trate só o código de saída do ADB como aprovação dos testes. O comando Gradle `connectedHomologationAndroidTest` só deve ser usado quando TODOS os dispositivos conectados forem de teste; o script acima seleciona exatamente um dispositivo.

Instrumentados preparados: navegação Compose e identificação após recriar Activity; migração histórica v7→v8 com validação Room; cadastro/foto, Mesa 05, quitação parcial/integral, caixa R$103, PDF A4 nativo, URI compartilhável, backup/restauração/reabertura e ausência de permissão de rede. Os testes de dados usam outro banco e outra pasta dentro da própria homologação. Recriar Activity não equivale a morte completa do processo; execute também o roteiro abaixo.

## Roteiro manual sem cobrança

1. Antes de instalar, registre a versão/package/certificado da instalação original por leitura. Se houver assinatura diferente, não tente atualizá-la. Instale **somente** o APK `.homologation`, mantendo o original. Confirme os dois ícones e a faixa `RUY POS — HOMOLOGAÇÃO`.
2. Confirme os exemplos e caixa de R$100. Cadastre um produto fictício e selecione uma foto pelo seletor Android. Em Vendas, registre pedido; consulte Pedidos. Force a parada **somente da homologação**, reabra e confira produtos, foto, pedido e quantidade de registros.
3. Abra Mesa 05. Adicione duas Coxinhas e um Caldo de Cana. Confira R$23 e comanda. Abra checkout e volte: mesa deve permanecer ocupada e sem recebimento.
4. No checkout, use dinheiro **fictício**: 2300 centavos a pagar/recebidos. Confirme uma vez; tente repetir pelo histórico. Confira um pedido, um recebimento, mesa livre e comanda fechada. Para testar parcial, use outra comanda: 1600 primeiro e 700 depois; somente a última quitação libera a mesa.
5. No caixa da venda de R$23, registre sangria fictícia de 2000 centavos. Feche contando 10300. Confira saldo esperado R$103, diferença zero, total dinheiro R$23 e histórico. Exporte PDF, abra e compartilhe em um aplicativo de teste; confirme A4, faixa fictícia, páginas adicionais para listas extensas e ausência de cortes.
6. Exporte e compartilhe ZIP. Cadastre um registro fictício posterior. Restaure o ZIP; confirme a mensagem de preparação, feche pelo botão e reabra pelo ícone. Compare registros e bytes/fotos anteriores; o registro posterior deve desaparecer. O ZIP original deve ser guardado fora do cache.
7. Tente ZIP corrompido, sem banco, sem imagem referenciada e com checksum alterado: recusar e preservar os dados. Interrupção da substituição é coberta por injeção nos testes locais; falta real de energia/armazenamento ainda exige teste no aparelho de laboratório. Nunca realize esse ensaio na instalação original.
8. Repita cadastro, vendas, mesas, caixa e consulta/reinício com Wi-Fi/dados ligados e desligados no dispositivo de testes. A variante continua sem acesso à rede nos dois casos. Registre o resultado de cada cenário; testes locais de banco não comprovam alternância real da rede.
9. Verifique que Aproximação no celular mostra requisitos e não oferece cobrança real na homologação. Use `Pagamento simulado — dados fictícios` em outro pedido; confira modalidade `SIMULADO`, sem acrescentar dinheiro físico esperado ao caixa.
10. Reabra o aplicativo original e confira a versão e os mesmos registros operacionais com o responsável. Essa conferência física permanece pendente até acesso ao aparelho.

## TapStone: procedimento operacional assistido

Na variante operacional futura, após revisão e testes autorizados, a opção `Aproximação no celular` mostra Android/NFC/rede. Não há lista de modelos permitidos. Instalação do app oficial e autorização da conta são conferidas pelo operador; o diagnóstico local não comprova elegibilidade Stone.

1. Salve o pedido e mantenha o caixa aberto. Selecione débito ou crédito à vista e marque a conferência de acesso à Stone. `Preparar conferência Stone externa` persiste uma tentativa; não inicia cobrança.
2. Anote o UUID local e o valor. Abra manualmente a Stone, `Vender > TapStone`, e informe exatamente valor e modalidade. O UUID/referência local não é enviado à Stone.
3. Somente após aprovação efetiva, confira comprovante/extrato. Volte ao Ruy POS e informe pedido local, valor em centavos, modalidade `TAPSTONE_DEBIT` ou `TAPSTONE_CREDIT`, referência única do comprovante, operador e evidência consultada. Não informe dados do cartão.
4. A confirmação é **manual auditada**, sem consulta à adquirente. Uma transação Room atualiza recebimento, livro do caixa, pedido, comanda e mesa. Confirmações repetidas iguais não duplicam lançamentos. Referência divergente/reutilizada é recusada.
5. Recusa, abandono, retorno sem comprovante, internet interrompida ou resultado indeterminado mantêm a tentativa pendente. Não cobre outra vez. Retome em Pedidos, mesmo após reiniciar. Para encerrar sem recebimento, confira expressamente no extrato que não houve crédito nem transação em andamento. Pagamento parcial mantém a mesa ocupada.

Não há SDK/Intent TapStone implementado, botão de cobrança automática, leitura EMV, captura NFC genérica ou armazenamento de dados de cartão. Parcelamento TapStone não é oferecido nesta implementação. Pix externo, cartão externo, dinheiro e simulado permanecem modalidades distintas. Smart POS anterior permanece no código e nos testes de contrato, mas `SMART_POS_REAL_ENABLED=false` impede seu lançamento normal; não é habilitável pelos Ajustes Stone.

## Backup e recuperação

O formato 2 contém banco SQLite consolidado a partir de snapshot DB/WAL, fotos das pastas atuais e antigas, manifesto de package/ambiente/schema e SHA-256 por arquivo. Verifica `integrity_check`, `foreign_key_check`, versão/identidade Room, tabelas, existência das imagens referenciadas e decodificação das imagens. Hashes detectam corrupção; não são assinatura criptográfica de autenticidade.

A extração limita nomes, duplicatas, quantidade (2000 entradas) e tamanho descomprimido (256 MiB). O arquivo é preparado em pasta privada; o banco aberto não é substituído. Na próxima inicialização, antes de Room e fora da thread de interface, cria cópia de rollback de DB/WAL/SHM e fotos, substitui, verifica integridade e marca commit. Interrupção anterior ao commit recupera o conjunto antigo na próxima inicialização. Falha irrecuperável de armazenamento impede abertura em vez de usar banco parcialmente substituído. Isso não substitui cópia externa nem comprova resistência a falha física de energia.

Formato 2 de outro package/ambiente é recusado. ZIP legado sem manifesto só é aceito na variante operacional, com migração/validação de uma cópia e integridade/fotos completas; nunca é aceito na homologação. Nem todo backup antigo é recuperável: o exportador anterior não incluía a pasta atual `product_images`; arquivos com fotos referenciadas ausentes devem ser recusados. Migrações antigas sem fixture histórico não devem ser declaradas homologadas.

## Documentação oficial consultada em 13/11/2026

- [TapStone — produto](https://www.stone.com.br/tapstone)
- [Sobre o TapStone](https://ajuda.stone.com.br/tapstone/sobre-o-tapstone)
- [Compatibilidade e requisitos](https://ajuda.stone.com.br/compatibilidade-e-requisitos)
- [Venda dentro do aplicativo Stone](https://ajuda.stone.com.br/pt_BR/tapstone/vender-com-tapstone)
- [Devcenter e programa de parceiros](https://www.stone.com.br/devcenter)
- [Requisitos SDK Android para POS](https://sdkandroid.stone.com.br/reference/pre-requisitos)
- [Deeplink de Smart POS](https://sdkandroid.stone.com.br/page/deeplink)

Nas páginas e buscas públicas consultadas (`TapStone SDK`, `TapStone deeplink`, `TapStone integração API` nos domínios oficiais), não foi encontrado contrato público para iniciar TapStone por terceiros. Isso não prova inexistência de parceria privada. O SDK/Deeplink de POS e as APIs online não autorizam presumir integração TapStone. Para automatizar, solicitar à Stone confirmação escrita de compatibilidade, contrato técnico, credenciais de sandbox, correlação/verificação confiável de transações e processo específico de homologação/liberação. Não houve contato enviado à Stone nem transação real nesta entrega.
