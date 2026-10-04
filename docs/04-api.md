# 04 · API

## Convenções

| Tema | Regra |
|---|---|
| Base | `/api/v1` (exceto `/actuator` e o Swagger) |
| Formato | JSON em camelCase |
| Autenticação | `Authorization: Bearer <access token>`; o refresh token vai num cookie HttpOnly (ver [arquitetura](05-arquitetura.md#segurança)) |
| Datas | `date` como `2026-10-02`; instantes em UTC, como `2026-10-02T18:30:00Z` |
| Dinheiro | `{ "amount": "46.99", "currency": "BRL" }`. O valor vai como string para não perder precisão no JavaScript |
| Nota | número de 0 a 5 em passos de 0,25 (ex.: `4.75`); `null` = sem nota |
| Paginação | `?page=0&size=20` (máximo 50) |
| Ordenação | `?sort=rating,desc`, só por campos permitidos em cada rota |
| Listas em filtro | separadas por vírgula: `?status=PLAYED,DROPPED` |
| Erros | Problem Details (RFC 9457), `application/problem+json` |
| Documentação | Swagger UI em `/swagger-ui.html`, gerado do código |

### Resposta paginada

Formato estável do Spring Data (`PagedModel`):

```json
{
  "content": [],
  "page": { "size": 20, "number": 0, "totalElements": 42, "totalPages": 3 }
}
```

### Erros

```json
{
  "type": "about:blank",
  "title": "Dados inválidos",
  "status": 422,
  "detail": "A entrada não é válida para o status WISHLIST.",
  "instance": "/api/v1/me/library/1942",
  "code": "INVALID_FIELDS_FOR_STATUS",
  "errors": [
    { "field": "review", "message": "Avaliação só é permitida em Jogando, Jogado ou Abandonado." }
  ]
}
```

| Status | Quando |
|---|---|
| 200 / 201 / 204 | ok / criado (com header `Location`) / sem conteúdo |
| 400 | JSON malformado ou parâmetro de tipo errado |
| 401 | sem token, ou token inválido ou expirado |
| 403 | autenticado, mas sem permissão (ou perfil privado) |
| 404 | recurso não existe |
| 409 | conflito: username ou e-mail já em uso |
| 422 | validação ou regra de negócio (RN02 a RN09) |
| 429 | limite de requisições |
| 502 / 503 | o IGDB falhou ou está indisponível |
| 500 | erro inesperado (logado com detalhes, respondido sem detalhes) |

## Endpoints

**Acesso:** público = sem login; usuário = token válido; admin = papel `ADMIN`. A coluna **F** indica a fase.

### Autenticação

| Método | Rota | Acesso | Descrição | F |
|---|---|---|---|---|
| POST | `/auth/register` | público | Cria a conta; 201 com access token e cookie de refresh | 1 |
| POST | `/auth/login` | público | Login com username ou e-mail; 5 erros seguidos do mesmo IP para o mesmo login bloqueiam por 15 minutos (429) | 1 |
| POST | `/auth/refresh` | cookie | Troca o refresh token (rotação) e devolve um novo access token | 1 |
| POST | `/auth/logout` | cookie | Revoga a sessão; 204 | 1 |
| POST | `/auth/password/forgot` | público | Envia o link de redefinição por e-mail | 2 |
| POST | `/auth/password/reset` | público | Redefine a senha com o token do e-mail | 2 |
| POST | `/auth/email/verify` | público | Confirma o e-mail | 2 |

### Minha conta

| Método | Rota | Acesso | Descrição | F |
|---|---|---|---|---|
| GET | `/me` | usuário | Perfil e configurações | 1 |
| PATCH | `/me` | usuário | Nome, bio, gênero (`FEMALE`, `MALE`, `NON_BINARY`, `OTHER` ou `null`), username. JSON Merge Patch: só o que vier muda, e `null` limpa | 1 |
| PATCH | `/me/settings` | usuário | Perfil privado, mostrar gastos, moeda padrão | 1 |
| PUT | `/me/password` | usuário | Troca a senha; exige a senha atual e encerra todas as sessões (é preciso entrar de novo) | 1 |
| DELETE | `/me` | usuário | Exclui a conta; exige a senha no corpo (`{ "password": "..." }`); 204 | 1 |
| GET | `/me/profile` | usuário | O cabeçalho do meu perfil, no formato de `/users/{username}`, completo mesmo com o perfil privado | 2 |
| GET | `/me/followers` e `/me/following` | usuário | Quem me segue e quem eu sigo, como nas rotas públicas, mas valendo com o perfil privado | 2 |
| GET | `/me/export` | usuário | Exporta todos os dados | 2 |

### Minha biblioteca

A entrada é identificada por **usuário + jogo**. Por isso o `PUT` cria ou substitui e nunca gera duplicado (RN01).

| Método | Rota | Acesso | Descrição | F |
|---|---|---|---|---|
| GET | `/me/library` | usuário | Lista com filtros `status`, `favorite`, `genreId`, `platformId` (onde a pessoa jogou), `minRating`, `recommends`, `completed` (zerou), `reviewed` (com texto de avaliação), `q`; ordena por `createdAt`, `updatedAt` (padrão, mais recentes primeiro), `rating`, `title`, `finishedOn` | 1 |
| GET | `/me/library/counts` | usuário | Quantos jogos em cada status, favoritos e avaliações (os contadores do perfil) | 1 |
| GET | `/me/library/{gameId}` | usuário | Minha entrada para o jogo; 404 se não existir | 1 |
| PUT | `/me/library/{gameId}` | usuário | Cria (201) ou substitui (200) a entrada inteira | 1 |
| PATCH | `/me/library/{gameId}` | usuário | Altera só os campos enviados (ex.: status ou favorito); `null` limpa o campo (JSON Merge Patch, RFC 7396) | 1 |
| DELETE | `/me/library/{gameId}` | usuário | Remove a entrada; 204 | 1 |
| GET | `/me/stats` | usuário | Estatísticas completas, inclusive gastos; com `?year=`, contam os jogos terminados no ano e as compras feitas no ano | 1 |

As estatísticas trazem `total`, `byStatus`, `finishedByYear`, os 10 primeiros gêneros (`byGenre`, sem a lista de desejos) e plataformas em que a pessoa jogou (`byPlatform`), `ratingDistribution` (uma faixa a cada meia estrela), `averageRating`, `hoursPlayed` e `spending` por moeda, sem conversão, com totais por loja e por ano (RN05).

Corpo do `PUT /me/library/{gameId}`:

```json
{
  "status": "PLAYED",
  "favorite": true,
  "review": {
    "rating": 4.75,
    "recommends": true,
    "text": "Exploração incrível e chefes difíceis na medida.",
    "hasSpoilers": false
  },
  "playthrough": {
    "platformId": 6,
    "hoursPlayed": 42,
    "startedOn": "2026-08-01",
    "finishedOn": "2026-09-10",
    "completed": true
  },
  "acquisition": {
    "method": "PURCHASED",
    "storeId": 1,
    "price": { "amount": "46.99", "currency": "BRL" },
    "acquiredOn": "2026-07-20"
  }
}
```

Na resposta, a entrada volta com o resumo do jogo (`id`, `slug`, `title`, `coverUrl`, `releaseYear`) e com `createdAt` e `updatedAt`. Partes vazias (como `"review": {}`) valem como ausentes.

Erros de regra voltam com 422 e um `code`:

| `code` | Quando |
|---|---|
| `INVALID_FIELDS_FOR_STATUS` | algum campo não cabe no status (RN02) |
| `INVALID_FIELDS` | nota fora dos passos de 0,25, término antes do início, valor pago sem compra ou moeda desconhecida (RN03, RN05, RN06) |
| `UNKNOWN_REFERENCE` | plataforma ou loja que não existe no catálogo |

### Perfis públicos

Respondem igual para qualquer pessoa, inclusive o dono, que vê os próprios dados privados em `/me`. Nunca incluem loja e valor pago, a menos que o dono ative "mostrar gastos" (RN10); o método e a data da aquisição continuam.

| Método | Rota | Acesso | Descrição | F |
|---|---|---|---|---|
| GET | `/users/{username}` | público | Cabeçalho (nome, bio, gênero se informado, `memberSince`) e `counts` de cada aba, de seguidores (`followers`) e de seguidos (`following`); num perfil privado, só `username`, `displayName` e `"private": true` | 1 |
| GET | `/users/{username}/library` | público | Mesmos filtros de `/me/library`; 403 se o perfil for privado | 1 |
| GET | `/users/{username}/favorites` | público | Favoritos | 1 |
| GET | `/users/{username}/reviews` | público | Entradas com texto de avaliação, das editadas por último | 1 |
| GET | `/users/{username}/stats` | público | Estatísticas, sem gastos (salvo se o dono permitir); 403 se o perfil for privado | 1 |
| GET | `/users/{username}/followers` e `/following` | público | Seguidores e seguidos (`username`, `displayName`, `followedAt`), dos mais recentes para os mais antigos; 403 se o perfil for privado | 2 |
| GET / PUT / DELETE | `/users/{username}/follow` | usuário | Se eu sigo a pessoa (204 ou 404), seguir e deixar de seguir. PUT e DELETE são idempotentes e respondem 204; seguir o próprio perfil dá 422 com `CANNOT_FOLLOW_SELF` (RN15) | 2 |
| GET | `/users/{username}/lists` | público | Listas personalizadas | 2 |

### Catálogo

A busca devolve só jogos já salvos no banco, com id próprio. Quando o resultado local é fraco, a API consulta o IGDB, salva o que encontrou e então responde.

| Método | Rota | Acesso | Descrição | F |
|---|---|---|---|---|
| GET | `/games` | público | `q`, `genreId`, `platformId`, `year` e `sort`: `relevance` (padrão quando há `q`), `popular` (padrão sem `q`; por enquanto usa o número de avaliações no IGDB e, quando a biblioteca existir, a presença nas bibliotecas), `rating`, `release`, `title`; `trending` (mais adicionados a bibliotecas públicas em 7 dias) | 1 |
| GET | `/games/{slug}` | público | Detalhes, com desenvolvedoras, publicadoras, franquias, temas, modos, perspectivas e nota do IGDB (0 a 100), + números da comunidade (RF23) | 1 |
| GET | `/games/{slug}/reviews` | público | Avaliações públicas (perfis públicos, com texto), das mais recentes para as mais antigas (Fase 2: também as mais curtidas) | 1 |
| GET | `/games/{slug}/similar` | público | Jogos parecidos | 3 |
| GET | `/reviews` | público | Avaliações recentes do site todo (página inicial), com o autor e o resumo do jogo | 1 |
| GET | `/genres`, `/platforms`, `/stores` | público | Listas para filtros e formulários (em cache) | 1 |

Números da comunidade em `GET /games/{slug}`:

```json
"community": {
  "averageRating": 4.3,
  "ratingsCount": 128,
  "ratingDistribution": [
    { "stars": 3.5, "count": 15 },
    { "stars": 4.0, "count": 30 },
    { "stars": 4.5, "count": 40 },
    { "stars": 5.0, "count": 30 }
  ],
  "recommendPercent": 94,
  "playersCount": 210,
  "wantToPlayCount": 75
}
```

A distribuição tem uma faixa a cada meia estrela, de 0 a 5 (11 faixas; o exemplo mostra só algumas). Uma nota 4,75 conta na faixa 4,5. `playersCount` conta Jogando, Jogado e Abandonado; `wantToPlayCount`, Quero jogar e Lista de desejos. Sem respostas, `averageRating` e `recommendPercent` vêm `null`.

### Social (Fase 2)

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| GET | `/me/feed` | usuário | Atividade de quem eu sigo, das mais recentes para as mais antigas, só de perfis públicos (RN16). Cada item tem `type` (`STATUS`, `REVIEW` ou `FAVORITE`), `user`, `game`, `status` (em `STATUS`, o daquele momento), `completed` em `STATUS` e `review` em `REVIEW`, como está agora. Nunca traz loja nem valor pago |
| PUT / DELETE | `/reviews/{entryId}/like` | usuário | Curtir e descurtir |
| POST | `/reviews/{entryId}/reports` | usuário | Denunciar |
| GET / POST | `/me/lists` | usuário | Minhas listas |
| GET / PATCH / DELETE | `/me/lists/{listId}` | usuário | Uma lista |
| PUT | `/me/lists/{listId}/items` | usuário | Define os itens e a ordem |

### Recomendações (Fase 3)

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| GET | `/me/recommendations` | usuário | Sugestões com motivo; cache de até 24 h |
| POST | `/me/recommendations/feedback` | usuário | `{ "gameId": 1942, "type": "NOT_INTERESTED" }` ou `ALREADY_PLAYED` |

### Administração

| Método | Rota | Acesso | Descrição | F |
|---|---|---|---|---|
| POST | `/admin/games/import` | admin | `{ "igdbId": 1942 }`: importa ou atualiza | 1 |
| POST | `/admin/games/{id}/sync` | admin | Ressincroniza com o IGDB | 1 |
| PATCH | `/admin/games/{id}` | admin | Correção manual | 2 |
| GET | `/admin/reports` | admin | Denúncias pendentes | 2 |
| PATCH | `/admin/reports/{id}` | admin | Resolver: manter ou remover a avaliação | 2 |

### Operação

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| GET | `/actuator/health` | público | Saúde da aplicação e do banco |
| GET | `/swagger-ui.html`, `/v3/api-docs` | público | Documentação |
