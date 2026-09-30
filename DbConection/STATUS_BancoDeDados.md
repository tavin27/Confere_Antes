# Relatório de Status: Banco PostgreSQL

**Data:** 2026-09-30  
**Status geral:** Código preparado para PostgreSQL; criação e validação contra uma instância PostgreSQL ainda pendentes.

## Implementações concluídas

- Schema PostgreSQL em `DataBase.sql`, com tabelas `users`, `transactions`, `scam_patterns`, `alerts`, `consents`, `privacy_requests` e `audit_log`.
- Constraints de domínio, chaves estrangeiras, índices de consulta, atualização automática de `users.updated_at` e índices de busca em `INDEX.sql`.
- Bootstrap atualizado: `CreateDB.py` aplica `DataBase.sql` e `INDEX.sql` em uma transação. `--dry-run` verifica/mostra os arquivos sem conectar ao banco.
- Verificação de transação vinculada ao usuário informado, com lock de linha, expiração e limite de tentativas. `CreateCodeVerifyEntry.py` gera HMAC-SHA256 com `VERIFICATION_PEPPER` e não armazena o código em claro.
- Papéis PostgreSQL e permissões: `app_rw` não recebe `DELETE`; a função de anonimização requer um hash BCrypt gerado para um segredo aleatório descartado.
- Auditoria de alterações nas tabelas principais, consentimentos e solicitações de privacidade. O campo de ator registra o login PostgreSQL (`session_user`), não a pessoa usuária final quando há uma conta de banco compartilhada.
- Backend configurado para o driver PostgreSQL, URL por variável de ambiente e `ddl-auto=validate`. A entidade `User` mapeia `active`, e o login rejeita contas inativas.
- Ambiente de testes isolado com H2 e segredo JWT somente de teste.

## Testes executados

- `mvnw.cmd clean test -q`: **22 testes aprovados, 0 falhas e 0 erros**, usando H2 em memória. Isso verifica o backend, mas não executa o schema PostgreSQL.
- `python CreateDB.py --dry-run`: aprovado; encontrou os dois arquivos SQL sem precisar conectar.
- Verificação sintática dos scripts Python: aprovada.
- DDL e funções PostgreSQL: **ainda não executados**. Não há `psql`, `psycopg2` nem instância PostgreSQL configurada neste ambiente.

## Para conectar e validar em PostgreSQL

1. Instalar/iniciar PostgreSQL e criar a database `confere_antes` (o bootstrap aplica schema em uma database existente; não cria a database).
2. Instalar a dependência do bootstrap: `python -m pip install psycopg2-binary`.
3. Configurar `DATABASE_URL` como DSN PostgreSQL de uma conta de setup com permissão para criar extensões e papéis. Essa conta executará o bootstrap.
4. Na pasta `DbConection`, executar primeiro `python CreateDB.py --dry-run` e depois `python CreateDB.py`.
5. Criar um login PostgreSQL para a aplicação e associá-lo ao papel `app_rw`. Configurar o backend com `DB_URL` (formato JDBC), `DB_USERNAME` e `DB_PASSWORD`. Não usar o superusuário PostgreSQL como credencial normal da aplicação.
6. Configurar `JWT_SECRET` com pelo menos 32 bytes. Para o helper Python de códigos de verificação, configurar também `VERIFICATION_PEPPER` com pelo menos 32 caracteres; manter ambos os segredos fora do repositório.
7. Executar um teste de integração usando o PostgreSQL real e confirmar que o Hibernate valida a tabela `users` e que as funções, extensões, índices e grants do script foram criados.

> `DataBase.sql` é um bootstrap inicial, não uma migração repetível. Em uma database já existente, usar migrações versionadas com Flyway; não reaplicar o bootstrap.

## Trabalho restante para uso funcional completo

- O backend Java ainda possui entidade JPA apenas para `users`. Para persistir via JPA as tabelas `transactions`, `scam_patterns`, `alerts`, `consents` e `privacy_requests`, faltam entidades, repositórios e serviços correspondentes.
- `CreateCodeVerifyEntry.py` é um helper Python separado; o fluxo Java ainda não chama a função PostgreSQL `verify_transaction_code`.
- A anonimização está protegida pelo papel `privacy_officer`, mas falta integrar um fluxo operacional que gere um BCrypt válido a partir de um segredo aleatório descartado e invoque `anonymize_user`.
- A auditoria no banco identifica a conta PostgreSQL. Para registrar com confiança a pessoa usuária final em conexões compartilhadas, falta definir uma fonte de identidade confiável na camada da aplicação.
- Revisar a concessão de `expire_pending_transactions()` ao papel `app_rw`; para produção, considerar um papel separado para o job periódico.
- O seed de `scam_patterns` não é idempotente; uma segunda execução do bootstrap falhará, como esperado para o script de criação inicial.
