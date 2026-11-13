# Ruy POS System

Estado Android/NFC 0.9.3: consulte o [roteiro de homologação](docs/ANDROID-NFC-RUNBOOK.md) e as [evidências e limitações](docs/ANDROID-NFC-EVIDENCE.md). O APK de homologação usa package e dados separados; pagamentos reais continuam sujeitos à validação autorizada.

As seções históricas abaixo descrevem o protótipo 0.2.0, não todas as funções atuais.


Evolução da versão 0.1: fila local de pedidos com UUID, status PENDING/SYNCED, tela de configuração HTTPS e sincronização manual por API com Idempotency-Key. Preserva o catálogo e carrinho locais.

## Limitações críticas
- **Não cobra cartão, débito, Pix nem confirma dinheiro recebido.** Não integra o SDK Stone.
- **Não está conectado ao Sistema Ruy real**: endpoint é um contrato proposto; é necessário implementar a API no servidor e testar.
- Não baixa estoque e não cria lançamento financeiro; somente transmite pedidos sem pagamento confirmado.
- Não há recuperação automatizada de falhas, login de operador, catálogo remoto, criptografia de token persistente nem testes em maquininha.
- Não usar em caixa real nem liberar produção.

## Testes
1. Abrir projeto no Android Studio/JDK 17, sincronizar Gradle e gerar APK de depuração em ambiente de teste. Não contém APK compilado nem Gradle Wrapper.
2. Registrar pedido; conferir pendência no histórico.
3. Configurar URL HTTPS terminando `/api/pos/orders` e token temporário.
4. Com API de testes que implemente docs/CONTRATO-API.md, sincronizar; repetir requisição e validar que o UUID não duplica pedidos.
5. Simular perda de rede, HTTP 500, resposta inválida e confirmação recebida após timeout; manter pedido para retry seguro.

## Compatibilidade Stone
Requisitos documentados: Android 6.0+, SDK Stone, módulo específico do modelo, packageName de parceiro, aplicativo assinado, homologação e distribuição autorizada. O app ainda não cumpre todos. Confirme modelo exato em "Sobre o dispositivo". O ID de projeto Supabase histórico do Sistema Ruy não é endereço de API.

Links:
- https://sdkandroid.stone.com.br/reference/pre-requisitos
- https://sdkandroid.stone.com.br/reference/preparando-aplicacao
- https://sdkandroid.stone.com.br/reference/processo-de-integracao
