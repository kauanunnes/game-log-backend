# 08 · Front-end (Vue 3)

> O código está no repositório **`game-log-frontend`** (pasta irmã deste projeto). Todas as telas da Fase 1 já usam a API.

## Identidade visual

| Item | Decisão |
|---|---|
| Direção | Interface do Windows 95/98 com acentos vaporwave e pixel art |
| Paleta base | Cores do Windows 95: cinza `#C0C0C0` nas janelas, azul-marinho `#000080` → `#1084D0` na barra de título, verde-água `#008080` na área de trabalho |
| Acentos | Rosa `#FF6EC7` / `#C2185B` nas janelas de destaque, ciano `#00FFFF` como marca-texto, amarelo `#FFD800` nas estrelas, gradiente pastel rosa → lilás → ciano |
| Estados | Erro `#C00000`, sucesso `#008000`, fita amarela e preta para "em construção" |
| Tema | Claro, como o Windows 95; o tema escuro fica para depois |
| Layout | Duas janelas: a da conta à esquerda, parada enquanto a principal rola; no celular, ela vai para depois do conteúdo |
| Tipografia | Press Start 2P (logo e destaques), Pixelify Sans (interface e títulos), IBM Plex Mono (textos longos e números) |
| Componentes | CSS próprio com tokens, sem biblioteca: botão, campo e fieldset nativos ganham o visual do Win95 |
| Ícones | Pixel art em SVG feita no projeto (ex.: a estrela da nota) |
| Inspirações | Pasta `refs/` deste repositório (local, fora do Git): Instagram.exe (perfil), CD Player e Paint (barras de ferramentas e de status), diálogos de erro, Word rosa, Miku e Kirby (pastel), tbd (amarelo, preto e marca-texto), Seed Club (layout em painéis) |

As cores, sombras e fontes ficam em `src/styles/tokens.css`. Em desenvolvimento, a rota `/_design` mostra a paleta, a tipografia e os componentes.

## Stack

| Camada | Escolha |
|---|---|
| Base | Vue 3 + TypeScript + Vite |
| Pacotes | pnpm |
| Rotas | Vue Router |
| Estado da sessão | Pinia |
| Dados da API | TanStack Query (cache, carregamento e erro; não repete erro 4xx) |
| Testes | Vitest + Vue Test Utils |
| Qualidade | ESLint + Oxlint + Prettier |
| Fontes | Fontsource (servidas pelo próprio app) |
| Deploy | Vercel, com rewrite de `/api/*` para a API |

Entram quando forem usados: Playwright (testes E2E) e `openapi-typescript` (tipos gerados do Swagger). Os formulários usam a validação do próprio HTML e as mensagens 422 da API, sem biblioteca.

## Estrutura de pastas

```text
src/
├── api/          cliente HTTP e um arquivo por recurso
├── components/   janelas, abas, cards, nota em estrelas, modal da biblioteca...
├── lib/          rótulos dos enums, formatação, filtros da URL, tabela da RN02 e composables
├── router/       rotas, abas e guarda de login
├── stores/       Pinia: sessão
├── styles/       tokens.css e base.css
├── types/        tipos da API (escritos à mão por enquanto)
└── views/        uma view por rota; as abas do perfil e das configurações em subpastas
```

## Rotas

| Rota | View | Login |
|---|---|---|
| `/` | Início | não |
| `/games` | Explorar | não |
| `/games/:slug` | Página do jogo | não |
| `/u/:username` | Perfil · Visão geral | não |
| `/u/:username/played`, `/playing`, `/backlog`, `/wishlist`, `/favorites`, `/reviews`, `/lists`, `/stats` | Abas do perfil (rotas filhas, para cada aba ter link próprio) | não |
| `/u/:username/followers`, `/following` | Seguidores e seguidos (fora da faixa de abas; abrem pelos contadores do cabeçalho) | não |
| `/u/:username/lists/:listId` | Uma lista; a dona edita ali mesmo (`?editar=1` abre o editor) | não |
| `/login`, `/signup` | Entrar e criar conta | não |
| `/forgot-password`, `/reset-password`, `/verify-email` | Recuperar a senha e confirmar o e-mail pelos links do e-mail | não |
| `/settings/profile`, `/account`, `/privacy`, `/data` | Configurações | sim |
| `/admin/reports` | Moderação (só admins; os outros voltam para o Início) | admin |
| `/_design` | Guia de estilos (só em desenvolvimento) | não |
| `/feed` | Feed | sim |
| `/for-you` | Para você (Fase 3) | sim |

## Componentes

| Componente | Faz |
|---|---|
| `AppWindow` | Janela com barra de título (azul ou rosa) e barra de status opcional |
| `TaskBar` | Barra de tarefas fixa com menu Iniciar (navegação), janela atual e relógio |
| `TabPanel` | Abas no estilo Win95, ligadas às rotas filhas, com o painel embaixo |
| `StarRating` | Mostra e edita a nota de 0 a 5 em passos de 0,25, com preenchimento parcial da estrela. Funciona no teclado (setas mudam 0,25; Home, End e Delete) |
| `GameCard` | Capa (ou padrão pontilhado sem capa), título, ano, nota, status e favorito |
| `UserWindow` | Janela da conta: convite para entrar (que volta para a página atual) ou nome, contadores por status, "Jogando agora" e atalhos para perfil, configurações e sair |
| `GameCardSkeleton` | Card pontilhado que pisca enquanto os jogos carregam |
| `ErrorMessage` | Diálogo de erro do Win95 com "Tentar de novo" |
| `PageNav` | Anterior e próxima, pela `?page=` da rota atual |
| `RatingHistogram` | Distribuição das notas, uma barra a cada meia estrela |
| `ReviewCard` | Avaliação pública com autor (ou o jogo, no perfil), nota, "recomenda" e o botão de curtir; o spoiler fica escondido até o clique |
| `FeaturedEditor` | Escolhe até 5 destaques entre os favoritos e os ordena com ▲ ▼ |
| `ListCard` | Cartão da lista: mosaico 2×2 com as capas dos quatro primeiros jogos, título e contagem |
| `ListEditor` | Edita título, descrição, privacidade e jogos: busca para adicionar, ▲ ▼ para reordenar (funcionam no teclado), nota por jogo e excluir |
| `ReportButton` | Denunciar, com motivo e detalhes numa caixa de diálogo; some sem sessão e na própria avaliação |
| `LikeButton` | Curtir e descurtir, com a contagem; sem sessão leva ao login, e na própria avaliação só mostra a contagem. Quais a pessoa curtiu vem de uma chamada só por página (`useLikedReviews`) |
| `FollowButton` | Seguir e deixar de seguir, como botão de alternar do Win95; sem sessão, leva ao login |
| `GameRow` | Capa pequena à esquerda e o conteúdo ao lado (avaliações do Início e feed) |
| `ActivityItem` | Item do feed: a frase ("Bia zerou Hollow Knight · há 2 horas") e, nas avaliações, nota, recomendação e texto |
| `LibraryActions` | Bloco "Sua biblioteca" da página do jogo: convite para entrar, "Adicionar" ou o status atual com "Editar" |
| `LibraryEntryDialog` | Modal de registrar e editar um jogo na biblioteca, com as seções que o status aceita |
| `BarList` | Barras horizontais das estatísticas (status, ano, gêneros, plataformas) |
| `UnderConstruction` | Aviso de "em construção" para telas que ainda não existem |

## Sessão e chamadas à API

- O access token fica só em memória, nunca em `localStorage`.
- Ao receber 401, o cliente chama `POST /auth/refresh` uma vez e repete a requisição.
- Ao abrir o app, ele tenta um refresh para restaurar a sessão.
- Erros da API viram `ApiError`, com o Problem Details da resposta.
- Em desenvolvimento, o proxy do Vite encaminha `/api` para `http://localhost:8080`. Em produção, o rewrite do Vercel faz o mesmo. Assim, front e API ficam na mesma origem, e o cookie do refresh funciona ([arquitetura](05-arquitetura.md#segurança)).

## Telas

- **Início** (`/`): os jogos mais adicionados na semana (`sort=trending`, que também aparece no Explorar) e as avaliações recentes (`GET /reviews`). Com sessão, o atalho "Criar conta" vira "Jogando agora".
- **Explorar** (`/games`): busca, filtros e ordenação ficam na URL; a busca só dispara no Enter, porque cada busca com poucos resultados consulta o IGDB.
- **Página do jogo** (`/games/:slug`): dados do IGDB, números da comunidade, avaliações públicas (das mais recentes ou das mais curtidas, pela URL) e o bloco "Sua biblioteca".
- **Registrar jogo** (modal): cada seção aparece só nos status que a aceitam; a tabela da [RN02](01-requisitos.md#regras-de-negócio) fica espelhada em `lib/entryRules.ts`. Ao trocar para um status que não aceita algo já preenchido, o modal avisa o que vai sair.
- **Perfil** (`/u/:username`): o dono usa `/me`, que traz loja e valor pago e funciona mesmo com o perfil privado; visitantes usam as rotas públicas. As abas de jogos são um componente só, com ordenação e página na URL; "Jogados" filtra Todos, Zerados e Abandonados, e o dono edita cada jogo dali. O cabeçalho mostra seguidores e seguidos, que levam às listas, e o botão Seguir para quem visita. A Visão geral abre com os favoritos em destaque, que a dona escolhe ali mesmo.
- **Listas** (aba do perfil e `/u/:username/lists/:listId`): a dona cria pela aba e cai direto no editor; salvar manda os dados da lista e todos os itens na nova ordem.
- **Moderação** (`/admin/reports`, só admins): as avaliações denunciadas com as denúncias delas; "Manter" ou "Remover texto" vale para todas as denúncias da avaliação, e remover pede confirmação.
- **Feed** (`/feed`): o que fizeram as pessoas que eu sigo, com o tempo relativo; atalhos no menu Iniciar e na janela da conta.
- **Links por e-mail**: "Esqueci minha senha" no login leva ao pedido do link; a senha nova encerra a sessão deste navegador também e volta ao login com um aviso. O link de confirmação confirma ao abrir, e a aba Conta mostra se o e-mail foi confirmado, com "Reenviar confirmação".
- **Configurações** (`/settings`): o perfil vai como JSON Merge Patch só com o que mudou. Trocar a senha encerra todas as sessões, então a tela entra de novo com a senha nova. Exportar baixa o JSON da API como arquivo, com o mesmo nome que ela sugere. Excluir a conta pede a senha e uma confirmação e volta ao Início com um aviso.

## Próximos passos

- Deploy no Vercel, com o rewrite de `/api/*` para a API publicada (depende da 1.8).
- Testes E2E com Playwright nos fluxos principais: cadastro, registrar jogo e perfil.
- Tela da Fase 2: o catálogo no painel de administração.
