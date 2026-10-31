# Contrato proposto de integração — NÃO implementado no servidor

POST https://<dominio-privado>/api/pos/orders
Authorization: Bearer <token POS de escopo mínimo>
Idempotency-Key: <UUID do pedido>
Content-Type: application/json

Corpo: `{ "id":"UUID", "createdAt": 0, "totalCents": 1500, "paymentStatus":"UNPAID", "syncStatus":"PENDING", "items":[{"name":"Coxinha","qty":1,"unitCents":1500}] }`.

Resposta 200/201 após gravação durável: `{ "id":"mesmo UUID", "persisted":true }`. Para requisições repetidas com o mesmo UUID e corpo, retornar o mesmo aceite sem inserir duplicata. Mesmo UUID com conteúdo diferente -> 409. Não enviar sucesso antes de gravar.

**Segurança e integridade:** autenticar terminal; autorizar organização/estabelecimento; limitar escopo do token a pedidos; validar tamanho, preços e produtos no servidor (nomes e preços locais não são fonte confiável); usar TLS válido; não expor PostgreSQL nem credenciais administrativas no POS; registrar auditoria sem armazenar dados sensíveis de cartão. Recomendável identidade estável de produto, assinatura/autorização por dispositivo e controle de versões de catálogo. O servidor NUNCA marca pedido como pago apenas porque `paymentStatus` local veio como `PAID`.

**Estados:** pedido pendente -> gravado no servidor -> pagamento aprovado Stone (após validação) -> movimento financeiro idempotente -> baixa de estoque idempotente. Cancelamento e estorno devem produzir eventos compensatórios. Pedidos locais sem confirmação de pagamento devem ir para fila de conferência, nunca direto à receita de vendas nem à baixa de estoque.

**Não está integrado:** API real do Sistema Ruy, PostgreSQL, catálogo, conciliação financeira e SDK Stone. O campo `syncStatus=SYNCED` significa exclusivamente que a API respondeu `persisted=true` com o mesmo UUID.

**Atenção:** token atualmente existe apenas em memória até fechar o app. A URL (não secreta) fica em SharedPreferences. Não usar em produção antes de autenticação robusta e testes.
