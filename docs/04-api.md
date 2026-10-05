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
| POST | `/auth/password/forgot` | público | `{ "email": "..." }`: envia o link de redefinição. Responde 204 exista a conta ou não (RN21) | 2 |
| POST | `/auth/password/reset` | público | `{ "token": "...", "newPassword": "..." }`: redefine a senha, encerra todas as sessões e confirma o e-mail; 204. Link vencido ou já usado dá 422 com `INVALID_TOKEN` | 2 |
| POST | `/auth/email/verify` | público | `{ "token": "..." }`: confirma o e-mail; 204, ou 422 com `INVALID_TOKEN` | 2 |
| POST | `/me/email/verification` | usuário | Manda o link de confirmação de novo; 409 se o e-mail já foi confirmado, 429 se o último saiu há menos de um minuto | 2 |

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
| GET | `/me/reviews` | usuário | Minhas avaliações no formato público, das editadas por último, mesmo com o perfil privado | 2 |
| GET | `/me/likes?entryIds=1,2,3` | usuário | Quais destas avaliações eu curti (até 100 ids); as listas de avaliações não dizem isso, porque respondem igual para todos | 2 |
| GET | `/me/export` | usuário | Tudo o que a conta guardou, para baixar (`Content-Disposition: attachment; filename="game-log-<username>-<data>.json"`): `account`, `library` (com loja e valor pago), `featured`, `lists` (inclusive as privadas, com os itens), `following`, `followers` e `likedReviews` (RN22) | 2 |

### Minha biblioteca

A entrada é identificada por **usuário + jogo**. Por isso o `PUT` cria ou substitui e nunca gera duplicado (RN01).

| Método | Rota | Acesso | Descrição | F |
|---|---|---|---|---|
| GET | `/me/library` | usuário | Lista com filtros `status`, `favorite`, `genreId`, `platformId` (onde a pessoa jogou), `minRating`, `recommends`, `completed` (zerou), `reviewed` (com texto de avaliação), `q`; ordena por `createdAt`, `updatedAt` (padrão, mais recentes primeiro), `rating`, `title`, `finishedOn` | 1 |
| GET | `/me/library/counts` | usuário | Quantos jogos em cada status, favoritos e avaliações (os contadores do perfil) | 1 |
| GET | `/me/library/{gameId}` | usuário | Minha entrada para o jogo; 404 se não existir | 1 |
| PUT | `/me/library/{gameId}` | usuário | Cria (201) ou substitui (200) a entrada inteira | 1 |
| PUT | `/me/library/featured` | usuário | `{ "gameIds": [1942, 7] }`: até 5 favoritos em destaque, na ordem do array; lista vazia tira todos. Um jogo que não é favorito dá 422 com `NOT_A_FAVORITE`, e nada muda (RN20) | 2 |
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
| GET | `/users/{username}` | público | Cabeçalho (nome, bio, gênero se informado, `memberSince`) e `counts` de cada aba, de seguidores (`followers`) e de seguidos (`following`), e os favoritos em destaque (`featured`, em ordem); num perfil privado, só `username`, `displayName` e `"private": true` | 1 |
| GET | `/users/{username}/library` | público | Mesmos filtros de `/me/library`; 403 se o perfil for privado | 1 |
| GET | `/users/{username}/favorites` | público | Favoritos | 1 |
| GET | `/users/{username}/reviews` | público | Avaliações da pessoa no formato público (com `id` e `likes`, nunca loja nem valor pago), das editadas por último; 403 se o perfil for privado | 1 |
| GET | `/users/{username}/stats` | público | Estatísticas, sem gastos (salvo se o dono permitir); 403 se o perfil for privado | 1 |
| GET | `/users/{username}/followers` e `/following` | público | Seguidores e seguidos (`username`, `displayName`, `followedAt`), dos mais recentes para os mais antigos; 403 se o perfil for privado | 2 |
| GET / PUT / DELETE | `/users/{username}/follow` | usuário | Se eu sigo a pessoa (204 ou 404), seguir e deixar de seguir. PUT e DELETE são idempotentes e respondem 204; seguir o próprio perfil dá 422 com `CANNOT_FOLLOW_SELF` (RN15) | 2 |
| GET | `/users/{username}/lists` | público | Só as listas públicas, das mexidas por último, com `itemCount` e os quatro primeiros jogos (`preview`); 403 se o perfil for privado | 2 |
| GET | `/users/{username}/lists/{listId}` | público | Uma lista pública com os itens em ordem (`position`, `game`, `note`); 404 se for privada | 2 |

### Catálogo

A busca devolve só jogos já salvos no banco, com id próprio. Quando o resultado local é fraco, a API consulta o IGDB, salva o que encontrou e então responde.

| Método | Rota | Acesso | Descrição | F |
|---|---|---|---|---|
| GET | `/games` | público | `q`, `genreId`, `platformId`, `year` e `sort`: `relevance` (padrão quando há `q`), `popular` (padrão sem `q`; por enquanto usa o número de avaliações no IGDB e, quando a biblioteca existir, a presença nas bibliotecas), `rating`, `release`, `title`; `trending` (mais adicionados a bibliotecas públicas em 7 dias) | 1 |
| GET | `/games/{slug}` | público | Detalhes, com desenvolvedoras, publicadoras, franquias, temas, modos, perspectivas e nota do IGDB (0 a 100), + números da comunidade (RF23) | 1 |
| GET | `/games/{slug}/reviews` | público | Avaliações públicas (perfis públicos, com texto), das mais recentes para as mais antigas, ou das mais curtidas com `sort=likes`. Cada uma traz o `id` da entrada e quantas curtidas tem (`likes`) | 1 |
| GET | `/games/{slug}/similar` | público | `{ byContent, byIgdb }` (RF60): até 12 jogos parecidos pelo conteúdo, os vizinhos nos embeddings sem as expansões e edições do próprio jogo (`null` se o jogo não tem vetor), e os `similar_games` do IGDB que estão no catálogo, na ordem do IGDB, para comparar | 3 |
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
| PUT / DELETE | `/reviews/{entryId}/like` | usuário | Curtir e descurtir; os dois são idempotentes e respondem 204. Só avaliações que aparecem nas listas (404 nas outras) e nunca a própria (422 com `CANNOT_LIKE_OWN_REVIEW`, RN17) |
| POST | `/reviews/{entryId}/reports` | usuário | Denunciar: `{ "reason": "SPAM" }` (`SPAM`, `OFFENSIVE`, `SPOILER` ou `OTHER`) e `details` opcional, até 500 caracteres; 201. Só avaliações que aparecem nas listas (404 nas outras), nunca a própria (422 com `CANNOT_REPORT_OWN_REVIEW`) e uma denúncia aberta por pessoa (409) |
| GET / POST | `/me/lists` | usuário | Minhas listas, públicas e privadas; criar com `{ "title": "Top 10 RPGs", "description": null, "visibility": "PUBLIC" }` (201) |
| GET / PATCH / DELETE | `/me/lists/{listId}` | usuário | Uma lista minha; o PATCH é JSON Merge Patch sobre título, descrição e visibilidade. A lista de outra pessoa responde 404 |
| PUT | `/me/lists/{listId}/items` | usuário | `{ "items": [{ "gameId": 1942, "note": "..." }] }`: define os itens e a ordem de uma vez, até 100. Jogo repetido dá 422 com `DUPLICATE_GAME`; jogo que não existe, `UNKNOWN_REFERENCE` |

### Recomendações (Fase 3)

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| GET | `/me/recommendations` | usuário | `{ suggestions: [{ game, reason }], personalized }` (RF61): até 20 sugestões calculadas na hora a partir da biblioteca, cada uma com um motivo que cita um jogo da pessoa ("Parecido com Hollow Knight, que você favoritou."). Com menos de 3 jogos que dizem do gosto, os populares completam a lista; `personalized` é `false` quando a biblioteca não diz nada. Com a chave da API, um modelo escolhe até 10 entre os candidatos e escreve o motivo (3.5), e `curator` diz qual (`"Gemini"` ou `"Claude"`; `null` sem a chave): enquanto ele escolhe, em segundo plano, a resposta traz a busca com `source: "SEARCH"` e `curating: true`; depois, `source: "AI"`, que vale 24 h ou até a biblioteca mudar |
| POST | `/me/recommendations/feedback` | usuário | `{ "gameId": 1942, "type": "NOT_INTERESTED" }` ou `ALREADY_PLAYED` |

### Administração

| Método | Rota | Acesso | Descrição | F |
|---|---|---|---|---|
| POST | `/admin/games/import` | admin | `{ "igdbId": 1942 }`: importa ou atualiza | 1 |
| POST | `/admin/games/{id}/sync` | admin | Ressincroniza com o IGDB | 1 |
| PATCH | `/admin/games/{id}` | admin | Correção manual | 2 |
| GET | `/admin/reports` | admin | Denúncias abertas, agrupadas por avaliação (`review` e `reports`), as mais denunciadas primeiro | 2 |
| PATCH | `/admin/reports/{id}` | admin | `{ "decision": "KEEP" }` ou `REMOVE` (tira o texto; a nota fica). Fecha todas as denúncias abertas da avaliação; 204, ou 409 se já estava resolvida | 2 |

### Operação

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| GET | `/actuator/health` | público | Saúde da aplicação e do banco |
| GET | `/actuator/metrics`, `/actuator/metrics/{nome}` | admin | Métricas do Micrometer: rotas, chamadas ao IGDB e caches ([arquitetura](05-arquitetura.md#observabilidade)) |
| GET | `/swagger-ui.html`, `/v3/api-docs` | público | Documentação |
