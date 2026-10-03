# Game Log — documentação

> Esta pasta descreve **o que** vamos construir e **em que ordem**. O código vem aos poucos, seguindo o [roadmap](07-roadmap.md).

## A ideia

Uma API para o jogador registrar os jogos que jogou, com nota, uma avaliação curta, se recomenda, onde comprou e quanto pagou. Ela também organiza o que ele quer jogar e o que deseja comprar. Cada usuário tem um **perfil público** com essas listas. Numa fase futura, a seção **"Você poderá gostar"** usa IA (RAG) para sugerir jogos parecidos com o que a pessoa curte.

Referências de produto: Letterboxd (filmes), Backloggd e HowLongToBeat (jogos).

## Documentos

| Documento | Conteúdo |
|---|---|
| [01 · Requisitos](01-requisitos.md) | Visão, escopo por fase, requisitos funcionais e não funcionais, regras de negócio |
| [02 · Telas e abas](02-telas.md) | Telas do front-end, abas do perfil e os endpoints de cada uma |
| [03 · Modelo de dados](03-modelo-de-dados.md) | Entidades, diagrama ER, restrições e índices |
| [04 · API](04-api.md) | Convenções (erros, paginação, autenticação) e endpoints |
| [05 · Arquitetura](05-arquitetura.md) | Stack, módulos, segurança, integração com o IGDB, testes, CI/CD e deploy |
| [06 · Recomendações com IA](06-recomendacoes-ia.md) | Como o "Você poderá gostar" vai funcionar e o que o MVP já precisa guardar |
| [07 · Roadmap](07-roadmap.md) | Fases e tarefas pequenas, cada uma com critério de pronto |
| [08 · Front-end](08-frontend.md) | Identidade visual, stack, rotas e componentes do front em Vue 3 |

## Decisões

**Proposta** = minha recomendação, aguardando sua confirmação. Quando decidir, troque para **aceita** ou registre a alternativa escolhida.

| # | Decisão | Recomendação | Alternativa | Status |
|---|---|---|---|---|
| D1 | Linguagem | Java 25 (LTS) | — | aceita |
| D2 | Framework | Spring Boot 4.1; a linha 3.5 saiu do suporte gratuito em 30/06/2026 | — | aceita |
| D3 | Fonte do catálogo | IGDB (Twitch), com cópia local no PostgreSQL | — | aceita |
| D4 | Escala de nota | 0 a 5 estrelas, com frações de 0,25 (ex.: 4,75), guardada como `numeric(3,2)` | — | aceita |
| D5 | Modelo da biblioteca | Uma entrada por usuário + jogo, com status (desejo → quero jogar → jogando → jogado) | Uma tabela por lista | proposta |
| D6 | Privacidade dos gastos | Loja e valor pago visíveis só para o dono, por padrão | Públicos por padrão | proposta |
| D7 | Números da comunidade | Calculados só com perfis públicos | Incluir perfis privados de forma agregada | proposta |
| D8 | Sessão | Access token JWT curto + refresh token em cookie HttpOnly, com front e API no mesmo site | Refresh token no corpo da resposta | proposta |
| D9 | Front-end | Vue 3 + TypeScript com pnpm, no repositório separado `game-log-frontend`; visual Windows 95/98 com acentos vaporwave ([08](08-frontend.md)) | — | aceita |
| D10 | Hospedagem | Neon (PostgreSQL) + Render ou Cloud Run (API) + Vercel (front) | Railway, Fly.io | proposta |
| D11 | IA (Fase 3) | Spring AI 2.0 + pgvector; Claude Opus 5.5 para gerar as sugestões | Embeddings e modelo definidos na Fase 3 | adiada |
| D12 | Gênero do usuário | Opcional: Feminino, Masculino, Não binário, Outro ou não informado ([RN14](01-requisitos.md#regras-de-negócio)) | — | aceita |
| D13 | Nome | Game Log (`game-log` em identificadores técnicos) | — | aceita |

## Convenções

- Código, tabelas e JSON em inglês; documentação e mensagens ao usuário em português.
- Cada item do roadmap vira uma issue e um PR pequeno.
- Commits no padrão Conventional Commits (`feat:`, `fix:`, `docs:`, `refactor:`, `test:`).
