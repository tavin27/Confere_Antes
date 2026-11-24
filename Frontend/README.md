# Confere Antes — Front-end

Aplicação web (React + Vite) do **Confere Antes**, mantida como **projeto separado** do back-end
Spring Boot: os dois são construídos, versionados e publicados de forma independente e rodam em
paralelo, conversando só pela API HTTP.

```
Navegador ──► front-end (nginx / Vite)  ──/api/*──►  back-end Spring Boot (JWT)  ──► PostgreSQL
```

O front-end chama sempre `/api/...` no próprio domínio. O Vite (desenvolvimento) ou o nginx (produção)
remove o prefixo `/api` e repassa a chamada ao back-end. Por isso **não é preciso configurar CORS** no Spring Boot.

## Telas
Login · Criar conta · Interface (início) · Analisar contato (chat) · Histórico · Perfil · Dados pessoais · Configurações.
Visual fiel ao protótipo (paleta vermelha estilo Santander, cartões sobrepostos, menu inferior, chat com sugestões).

## Rodar em desenvolvimento
Pré-requisito: Node 18+ e o back-end rodando (padrão `http://localhost:8080`).
```bash
npm install
cp .env.example .env      # ajuste DEV_API_TARGET se o back-end estiver em outro endereço
npm run dev               # http://localhost:5173
npm test                  # testes unitários (Vitest)
npm run build             # gera dist/
```

## Publicar (Docker / Render)
```bash
docker build -t confere-antes-frontend .
docker run -p 8080:8080 \
  -e API_UPSTREAM=https://SUA-API.onrender.com \
  -e API_HOST=SUA-API.onrender.com \
  confere-antes-frontend
```
No Render use o `render.yaml` (define `API_UPSTREAM` e `API_HOST` no painel).

## Estrutura
```
src/
  lib/        http.js (cliente + erros), api.js (endpoints), risk.js, storage.js, format.js
  auth/       AuthContext (login/sessão/JWT) e Guards (RequireAuth, PublicOnly)
  components/ Screen, BottomNav, Field, Risk (chip/medidor), SessionRow, Icon, Loader
  pages/      Login, Register, Home, Chat, History, Profile, PersonalData, Settings
  styles/     app.css (tokens e componentes)
```

## Contrato com o back-end (lido do código Java)
| Uso | Chamada |
|---|---|
| Login | `POST /auth/login` `{email, password}` → `{token, type}` |
| Cadastro | `POST /users` `{username, email, phone?, password(8–72)}` |
| Usuário logado | `GET /users/me` |
| Nova análise | `POST /alerts` `{contactChannel: LIGACAO\|SMS\|WHATSAPP\|EMAIL, contactValue?}` → `{id, overallRiskScore, createdAt…}` |
| Mensagens | `GET` e `POST /alerts/{id}/messages` (`sender`: `USUARIO` ou `CONTATO`) |

Erros do Spring (`{message}`, mapa de campos no HTTP 400 e texto puro no 409) são traduzidos em `lib/http.js`.

## O que o back-end ainda não oferece (e como o front trata)
| Lacuna | Tratamento atual | Para completar |
|---|---|---|
| Listar alertas do usuário | Histórico e score da Home ficam **neste dispositivo** (`localStorage`, por e-mail) | `GET /alerts` → trocar `sessionsStore` por chamada de API |
| Avaliação de risco da conversa | O chat mostra `overallRiskScore` quando vier preenchido; senão informa que não há avaliação | Back-end calcular/atualizar o score e expor `GET /alerts/{id}` |
| Confirmação em duas etapas no **login** | Não implementada; a chave aparece desativada ("Em breve") | Endpoint de envio/validação do código no login |
| Preferências (Configurações) | Salvas só neste dispositivo | `GET/PUT` de preferências |
| Perfil: contatos de confiança, segurança da conta, edição de dados | Marcados como "Em breve" | Endpoints correspondentes |
| Análise de ligações (`/alerts/{id}/calls`) | Fora desta versão (exige URL pública do áudio) | Tela de ligação |

As faixas de risco (Baixo < 30 ≤ Médio < 60 ≤ Alto < 80 ≤ Crítico) estão em `src/lib/risk.js`; ajuste se a regra do servidor for outra.

## Segurança
- O JWT fica em `sessionStorage` e é enviado em `Authorization: Bearer`. Token rejeitado (401) leva ao login.
- Nenhuma chave de API vai para o navegador; a chave da IA existe só no back-end.
- O chat avisa para nunca informar senha, token ou código.
