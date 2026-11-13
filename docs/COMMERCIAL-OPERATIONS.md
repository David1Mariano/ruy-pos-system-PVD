# Operação da candidata comercial 0.10.0-rc1

Esta entrega acrescenta Pix estático e acabamento comercial ao app existente. Não é homologação bancária, NFC real nem autorização para distribuir um release definitivo. Use o APK de homologação para o roteiro fictício e preserve a instalação operacional e a T8.

## Ambientes e valores

| Variante | Package | Dados iniciais | Rede e pagamentos |
|---|---|---|---|
| Homologação | `br.com.ruy.pos.homologation` | Exemplos fictícios existentes | Sem INTERNET/sync de produção; faixa permanente; QR bancário/cópia/compartilhamento bloqueados; simulação explícita |
| Release candidato | `br.com.ruy.pos` | Sem carga fictícia | Conta e benefícios exigem configuração explícita; Pix por API e Smart POS bloqueados; APK sem assinatura comercial nesta entrega |

Digite valores em **reais**, por exemplo `23,00`, `100,00` ou `1.234,56`. A entrada `100` significa R$100,00. O app converte para centavos inteiros sem arredondamento financeiro por ponto flutuante. Não use o antigo hábito de digitar `10000` para R$100.

O app mantém Android 6/API23+ para instalação, sem restrição por marca/modelo. Aproximação Stone requer Android 10+, NFC presente/ativo, internet e conta/aplicativo elegíveis. O APK Android não executa em iPhone. Compatibilidade declarada não substitui testes no aparelho.

## Configuração Pix

1. Em **Mais → Configurações → Pagamentos → Conta de recebimento Pix**, confirme o bloqueio de tela do Android. Sem bloqueio de tela configurado, a alteração protegida não é liberada.
2. Informe instituição, identificação da conta, recebedor, cidade e tipo/chave Pix. CPF/CNPJ do recebedor é opcional; quando informado, seus dígitos são validados. São suportados CPF, CNPJ numérico/alfanumérico, e-mail, celular brasileiro internacional `+55` e chave aleatória.
3. Nome/cidade são normalizados para os caracteres suportados no QR, com limites de 25/15 caracteres. Confira a forma exibida. A validação verifica formato, **não consulta o DICT nem prova titularidade**.
4. Confira no banco que a chave está cadastrada e pertence à conta desejada. Identifique o responsável, marque a conferência e revise a confirmação antes de salvar. As alterações ficam auditadas em armazenamento protegido, com responsável, data e fingerprints das configurações. Chave/documento não aparecem nos registros de diagnóstico.
5. Não informe senha bancária, PIN, cartão, CVV ou token bancário. O formulário não solicita essas credenciais. A tela de configuração bloqueia captura de tela; os diagnósticos ficam em Suporte, acessível após autenticação do aparelho. O acesso protegido é encerrado ao deixar o app.

Na homologação, somente e-mail fictício terminado em `.invalid` é aceito na configuração. Nenhum QR bancário é exibido ou compartilhado nessa variante. O exemplo exportado em `artifacts/commercial-candidate/pix-fixture/` destina-se exclusivamente à inspeção de código/CRC, sem valor financeiro.

## Pix estático na venda

1. Finalize o carrinho, selecione **Pix QR Code**, confira o valor em reais e gere o código. Sem conta configurada, a opção de geração fica indisponível com orientação para as configurações.
2. O pedido e a tentativa são persistidos antes de apresentar o QR. O app exibe recebedor, valor, pedido, QR, Pix Copia e Cola e ações de copiar/compartilhar na variante comercial. O valor é incluído no payload. O pagador deve conferir o titular e o valor no banco.
3. Gerar, copiar, compartilhar, alternar aplicativos ou voltar **não confirma recebimento**, não libera mesa nem entra como recebido no caixa. O status continua aguardando.
4. Depois de conferir o crédito diretamente no banco, escolha **Conferir recebimento**. Informe o valor efetivamente conferido, a referência completa e única da transação, o operador e a evidência consultada. Marque a conferência e registre.
5. A confirmação é **manual auditada**. Não é consulta automática ao banco. Valor/pedido divergente ou referência reutilizada são recusados. A baixa de pedido, caixa e comanda ocorre numa única transação Room; repetição da mesma confirmação não gera novo lançamento.
6. Para retornar sem receber, deixe pendente e retome em Pedidos. Para encerrar um registro sem recebimento, confira expressamente o extrato e a ausência de operação em andamento. Um QR **estático não expira e não pode ser revogado no banco pelo Ruy POS**: créditos posteriores ainda precisam ser conferidos, especialmente se o código foi compartilhado.

Uma mudança de conta só afeta novos códigos. Um código já emitido preserva a conta/valor originais em snapshot protegido. O identificador Pix estático possui até 25 caracteres; não é identificador de cobrança dinâmica. CRC verifica integridade de codificação, não autentica a conta nem constitui comprovante.

## Aproximação, cartão externo e parcelamento

1. Em Aproximação no celular, confira o diagnóstico operacional e marque apenas se o aplicativo oficial Stone estiver instalado e o TapStone autorizado naquela conta. Instalação/elegibilidade são conferidas pelo operador; não há detecção confiável de conta ou mecanismo público de lançamento TapStone implementado.
2. Selecione débito ou crédito. Para crédito, informe apenas parcelas efetivamente permitidas pela Stone e confirme suas condições. A documentação pública informa até 18 parcelas; a disponibilidade real pode ser menor. O Ruy POS não inventa taxas, juros, acréscimos ou descontos.
3. Prepare a conferência, abra a Stone manualmente e use **Vender → TapStone**. Informe exatamente o valor/modalidade/parcelas. O pedido local não é enviado à Stone. Não existe botão de cobrança automática.
4. Depois de aprovação e conferência no comprovante/extrato, volte e informe valor, modalidade e parcelas conferidos, referência única, operador e evidência. A mensagem de sucesso identifica **Pagamento confirmado manualmente — TapStone externo**.
5. Recusa, abandono, ausência de internet, fechamento do provedor ou resultado indeterminado não dão baixa. Mantenha a tentativa pendente até conferir o extrato. A opção Não recebido só encerra após declaração explícita de que não houve crédito nem operação em andamento. O registro final sem recebimento não equivale a um estorno bancário.
6. Para cartão em outro equipamento, escolha Débito/Crédito externo e registre somente uma transação já aprovada. Crédito externo aceita o número de parcelas constante do comprovante, dentro do limite técnico do formulário. Sempre confira as condições reais do provedor. A confirmação pede referência, valor, operador e evidência; uma operação repetida não duplica o recebimento.

Não há leitura EMV/NFC genérica, captura de dados sensíveis de cartão nem reutilização do Deeplink Smart POS como se fosse TapStone. Smart POS anterior continua preservado e bloqueado no caminho normal de execução.

## Voucher e benefícios

Em **Configurações → Voucher e benefícios**, após confirmar o bloqueio do aparelho, informe provedor/equipamento, bandeira e apenas as modalidades efetivamente credenciadas: vale-alimentação, vale-refeição ou voucher. Identifique o responsável, confirme a conferência de credenciamento e revise a alteração. A configuração atual refere-se ao provedor/bandeira informados; não é uma lista automática de todas as bandeiras.

Sem habilitação, o checkout informa indisponibilidade e o coordenador de pagamentos recusa voucher. Modalidades habilitadas aparecem como opções externas. Registre apenas após aprovação no aplicativo/equipamento autorizado, com comprovante, valor e operador. A bandeira/provedor usados são preservados no histórico, mesmo após alterar a configuração. Voucher não é parcelado no Ruy POS. A menção da Stone a Ticket no Android não autoriza presumir suporte a toda bandeira ou estabelecimento.

## Caixa, pedidos e relatórios

- Mesa parcialmente paga continua ocupada. Somente quitação integral confirmada fecha a comanda e libera a mesa. Voltar do checkout leva ao pedido persistido ou às mesas; não descarta seus itens. Após reiniciar, retome o pedido pelo histórico e pelo aviso de pagamento pendente.
- O caixa separa dinheiro, Pix manual, Pix por API (indisponível para novos recebimentos nesta versão), débito externo, crédito externo, TapStone, voucher e simulado na homologação. Pendências e cancelados/sem recebimento são exibidos separadamente. Pendências em conferência impedem fechar o caixa.
- O histórico mostra modalidade, status legível, valor, comprovante, parcelas e responsável/data da conferência manual. O nome do operador é informado pelo atendente; não é identidade autenticada por uma sessão D1.
- O fechamento calcula dinheiro físico com abertura + dinheiro + suprimentos − sangrias. Recebimentos por cartão/Pix/voucher não aumentam dinheiro físico esperado. O detalhamento por origem fica no snapshot do fechamento sem mudar o schema do banco.
- PDFs usam A4, paginação, nomenclatura operacional e compartilhamento `content://`. Relatórios antigos com origem incompleta mantêm os valores históricos e indicam ausência de detalhamento. O exportador usa somente a sessão de caixa selecionada.
- Valores são brutos. Não há taxas ou descontos configurados nesta entrega. Estornos/devoluções reais devem ser tratados no provedor e ainda não possuem comando de execução/conciliação financeira nesta versão; um registro local cancelado não devolve dinheiro ao cliente. O relatório não inventa estornos.

## Backup e proteção

Room permanece no schema 8, com as migrações anteriores e sem fallback destrutivo. Não houve acesso às bases das instalações. Os testes locais exercitam snapshots SQLite completos, imagens, hashes, dados antes/depois, reabertura e rollback após interrupção injetada. Isso não comprova resistência a falta real de energia ou esgotamento de armazenamento.

O backup de banco/fotos **não exporta material Android Keystore, conta Pix, permissões de voucher nem segredos de configuração**. Em outro aparelho/reinstalação, reconfigure os dados de recebimento de forma protegida. Se o snapshot protegido de um QR antigo não estiver disponível, ele não será regenerado silenciosamente com uma conta diferente; confira o banco e concilie o pedido pendente. Na restauração no mesmo aparelho, as preferências protegidas existentes são mantidas. Nunca restaure um arquivo de teste na instalação operacional.

Um token legado de sincronização, se utilizado em execução futura, é copiado para armazenamento protegido antes de remover a cópia antiga. Se a proteção falhar, a origem é preservada e o envio é bloqueado/reagendado. Nenhum token operacional foi lido ou migrado nesta entrega.

## Roteiro fictício no Android de testes

1. Não use a T8. Mantenha original e homologação separados. Para instrumentação, use emulador novo ou outro aparelho de laboratório com instalação dedicada; o teste de navegação carrega cadastros da homologação. Não execute essa suíte sobre a instalação existente que deseja preservar. Instale somente o APK `.homologation` mediante ação do responsável; nenhum APK foi instalado durante esta entrega.
2. Confira a faixa permanente, produtos fictícios, Mesa 05 e caixa inicial. Digite valores em reais. Cadastre produto/foto fictícios, registre pedido, consulte histórico, force parada somente da homologação e reabra. Confira persistência e ausência de duplicatas.
3. Mesa 05: duas Coxinhas de R$8 + um Caldo de R$7 = R$23. Feche parcialmente R$16 e depois R$7 em dinheiro fictício; somente a segunda confirmação libera a mesa.
4. Em um ciclo de caixa exclusivo para esse teste: abertura R$100 + venda fictícia R$23 − sangria R$20 = R$103. Feche contando R$103; confira diferença zero, modalidade, histórico e PDF/compartilhamento em aplicativo de teste.
5. Configure Pix com chave fictícia `.invalid` após autenticar o aparelho. Confira indisponibilidade antes da configuração e bloqueio de exibição/cópia do QR bancário na homologação. O parser/CRC/QR e o fluxo Pix pendente/conferido já são exercitados pelos testes automatizados com dados fictícios.
6. Habilite somente um voucher fictício no laboratório, confira a modalidade e a explicação do provedor, depois desabilite. Na homologação, use Simulado para recebimentos de treinamento; nunca cobre em um aplicativo financeiro.
7. Repita os fluxos locais com rede ligada/desligada no aparelho de laboratório. Exercite voltar, recusa/resultado desconhecido, reinício durante checkout e retomada pelo histórico. Sem NFC ou internet, a aproximação deve explicar a indisponibilidade.
8. Faça backup, compartilhe uma cópia, acrescente um registro fictício e restaure. Feche pelo botão e reabra. Compare cadastros, pedidos, pagamentos, caixa e fotos. ZIP corrompido/sem arquivo deve preservar os dados. Não ensaie interrupção física de energia na instalação original.

Instrumentação com seleção explícita, sem limpeza/desinstalação:

```powershell
.\scripts\Test-AndroidHomologation.ps1 -Serial emulator-5554
# Somente em celular de testes isolado, nunca na T8:
.\scripts\Test-AndroidHomologation.ps1 -Serial SERIAL_DO_TESTE -IsolatedTestPhone
```

Consulte as [evidências e limitações](COMMERCIAL-EVIDENCE.md) e o [procedimento de assinatura](COMMERCIAL-SIGNING.md) antes de qualquer preparação para uso real.

## Fontes oficiais consultadas em 13/11/2026

- [BCB — Manual Pix 2.10.0](https://www.bcb.gov.br/content/estabilidadefinanceira/pix/Regulamento_Pix/II_ManualdePadroesparaIniciacaodoPix.pdf), [BR Code](https://cdn-www.bcb.gov.br/content/estabilidadefinanceira/spb_docs/ManualBRCode.pdf) e [contrato da API Pix](https://github.com/bacen/pix-api).
- [Receita — CNPJ alfanumérico e dígitos verificadores](https://www.gov.br/receitafederal/pt-br/centrais-de-conteudo/publicacoes/documentos-tecnicos/cnpj/manual-dv-cnpj.pdf).
- [Stone — TapStone](https://www.stone.com.br/tapstone), [modalidades e condições](https://ajuda.stone.com.br/tapstone/sobre-o-tapstone) e [requisitos](https://ajuda.stone.com.br/compatibilidade-e-requisitos).
- [Efí — conta, credenciais e certificado](https://dev.efipay.com.br/docs/api-pix/credenciais/) e [validação de webhooks](https://dev.efipay.com.br/docs/api-pix/webhooks/); [Inter — APIs PJ](https://ajuda.inter.co/conta-digital-pessoa-juridica/o-inter-disponibiliza-alguma-api-para-minha-conta-digital-pj) e [documentação de integração](https://developers.inter.co/duvidas-frequentes).

Foram examinadas opções oficiais Efí/Inter e o padrão BCB, sem escolher banco ou contratar serviços. A API dinâmica possui somente a fronteira desacoplada para criação/consulta/cancelamento/devolução e um provedor desabilitado por padrão. Nenhum endpoint foi inventado ou chamado. A integração futura requer contrato, backend seguro, credenciais/certificados fora do APK, validação de origem do webhook, correlação de txid/identificador/valor e idempotência transacional, seguida de testes autorizados. Não há consumidor de webhook ou confirmação automática habilitado.

Nas páginas/buscas oficiais consultadas, não foi encontrado contrato público de lançamento TapStone por terceiros. Não se conclui que inexista parceria privada: uma eventual automação depende de documentação e autorização específicas da Stone, sem presumir aplicabilidade do Smart POS.
