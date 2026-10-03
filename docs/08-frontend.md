# 08 · Front-end (Vue 3)

> O código está no repositório **`game-log-frontend`** (pasta irmã deste projeto). Explorar e Página do jogo já usam a API; as outras telas mostram "Em construção" até a vez delas.

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

Entram quando forem usados: VeeValidate + Zod (formulário "Registrar jogo"), Playwright (testes E2E) e `openapi-typescript` (tipos gerados do Swagger).

## Estrutura de pastas

```text
src/
├── api/          cliente HTTP e um arquivo por recurso
├── components/   AppWindow, TaskBar, TabPanel, StarRating, GameCard, PixelStar, UnderConstruction
├── lib/          rótulos em português dos enums da API
├── router/       rotas, abas e guarda de login
├── stores/       Pinia: sessão
├── styles/       tokens.css e base.css
├── types/        tipos da API (escritos à mão por enquanto)
└── views/        uma view por rota
```

## Rotas

| Rota | View | Login |
|---|---|---|
| `/` | Início | não |
| `/games` | Explorar | não |
| `/games/:slug` | Página do jogo | não |
| `/u/:username` | Perfil · Visão geral | não |
| `/u/:username/played`, `/playing`, `/backlog`, `/wishlist`, `/favorites`, `/reviews`, `/stats` | Abas do perfil (rotas filhas, para cada aba ter link próprio) | não |
| `/login`, `/signup` | Entrar e criar conta | não |
| `/settings/profile`, `/account`, `/privacy`, `/data` | Configurações | sim |
| `/_design` | Guia de estilos (só em desenvolvimento) | não |
| `/feed` | Feed (Fase 2) | sim |
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
| `ReviewCard` | Avaliação pública com autor, nota e "recomenda"; o spoiler fica escondido até o clique |
| `UnderConstruction` | Aviso de "em construção" para telas que ainda não existem |

## Sessão e chamadas à API

- O access token fica só em memória, nunca em `localStorage`.
- Ao receber 401, o cliente chama `POST /auth/refresh` uma vez e repete a requisição.
- Ao abrir o app, ele tenta um refresh para restaurar a sessão.
- Erros da API viram `ApiError`, com o Problem Details da resposta.
- Em desenvolvimento, o proxy do Vite encaminha `/api` para `http://localhost:8080`. Em produção, o rewrite do Vercel faz o mesmo. Assim, front e API ficam na mesma origem, e o cookie do refresh funciona ([arquitetura](05-arquitetura.md#segurança)).

## Próximos passos

- **Explorar** (`/games`): busca, filtros e ordenação ficam na URL; a busca só dispara no Enter, porque cada busca com poucos resultados consulta o IGDB.
- **Página do jogo** (`/games/:slug`): dados do IGDB, números da comunidade e avaliações públicas.
- A seguir: entrar e criar conta, registrar jogo na biblioteca, perfil com abas e estatísticas, e a página inicial com `sort=trending` e `GET /reviews` (a API de todas já existe).
