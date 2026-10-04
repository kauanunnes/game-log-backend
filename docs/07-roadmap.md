# 07 · Roadmap

Cada item numerado é uma issue e um PR pequeno. Marque `[x]` conforme for concluindo.

## Definição de pronto (vale para toda tarefa)

- Testes do caminho feliz e dos erros principais.
- Migration do Flyway, se a tarefa mexeu no banco.
- Endpoint documentado no Swagger, com exemplo.
- CI verde.
- Docs atualizados, se o comportamento mudou.

## Fase 0 · Fundação

### 0.1 Sincronizar o que está local

- [x] Commitar ou descartar as mudanças pendentes em `application.properties`, `application-dev.properties` e `WebConfig`.
- [x] Enviar os 6 commits que estão só na sua máquina (`git push`). Antes do push, atenção: o `application-dev.properties` desses commits tem usuário e senha do PostgreSQL local. Aponta só para localhost, mas, se essa senha for usada em outro lugar, troque-a.
- [x] Corrigir o pacote do `WebConfig`: ele está em `config`, fora de `com.kauan.games_list`. O Spring só procura componentes dentro do pacote da aplicação, então a classe nunca é carregada e o CORS não é aplicado.

**Pronto quando:** `git status` está limpo e o GitHub está igual ao local.

### 0.2 Atualizar a base

- [x] Spring Boot 3.5.5 → 4.1.x e Java 21 → 25 ([D1, D2](README.md#decisões)). O Boot 4 traz Spring Framework 7, Spring Security 7, Hibernate 7 e Jackson 3; com ~15 classes, a migração é barata agora.
- [x] Atualizar ou remover o `system.properties` (usado por buildpacks do Heroku/Railway).
- [x] Remover do `pom.xml` o `maven-resources-plugin 3.1.0` fixo (resquício do curso). O H2 ficou para a 0.4: até o Testcontainers entrar, é ele que dá banco aos testes.
- [x] Trocar o `@Autowired` em campo por injeção via construtor.
- [x] Renomear o back-end para game-log: `artifactId` e `name` no `pom.xml`, `spring.application.name`, pacote `com.kauan.gamelog` (classe `GameLogApplication`). O repositório no GitHub também já se chama `game-log-backend`.

**Pronto quando:** `./mvnw verify` passa no Boot 4.

### 0.3 Banco e configuração

- [x] `compose.yaml` com PostgreSQL na imagem `pgvector/pgvector:pg18` (mesma versão do Neon), que já serve para a Fase 3, e o suporte a Docker Compose do Spring Boot (o banco sobe junto com a aplicação).
- [x] Flyway com a migration inicial; o `import.sql` virou um seed (`db/seed`) que só roda no perfil `local`, e o Hibernate passou a só validar o schema (`ddl-auto=validate`).
- [x] Perfis `local` e `prod` lendo variáveis de ambiente, `.env.example` versionado e credenciais do banco local fora do `application-dev.properties`.

**Pronto quando:** clonar o repositório e rodar `./mvnw spring-boot:run` sobe banco e aplicação sem editar nenhum arquivo.

### 0.4 Base de qualidade

- [x] Prefixo `/api/v1` nos controllers da aplicação; Swagger e Actuator ficam na raiz.
- [x] Tratamento global de erros com Problem Details: id inexistente responde 404 (antes, `findById(id).get()` respondia 500).
- [x] springdoc-openapi (Swagger UI) e Actuator (`/actuator/health`).
- [x] Testcontainers com `@ServiceConnection` e o primeiro teste de integração; remover o H2 (os testes passam a usar PostgreSQL).
- [x] Spotless (formatação) e JaCoCo (cobertura).
- [x] GitHub Actions rodando `./mvnw verify` em cada PR, mais o Dependabot.
- [x] README do projeto (o que é, como rodar, link para `docs/`) e LICENSE.

**Pronto quando:** um PR de teste fica verde no CI e o Swagger abre localmente.

### 0.5 Estrutura por módulos

- [x] Reorganizar o código por funcionalidade (ver [arquitetura](05-arquitetura.md#organização-do-código)): `catalog/` (jogos), `lists/` (listas do curso, base da tarefa 2.5) e `shared/` (CORS, prefixo da API e erros). A dependência entre módulos vai num sentido só: `lists` → `catalog`.

**Pronto quando:** os pacotes estão separados por funcionalidade.

## Fase 1 · MVP

### 1.1 Catálogo local · RF20–RF22

- [x] Migration `V2` com `games`, `genres`, `platforms`, `stores` e as tabelas de junção (as tabelas do curso saem aqui). A coluna `games.metadata` já existe, mas só é preenchida e mapeada na 1.2.
- [x] `GET /games` com busca por trigramas tolerante a acentos e erros de digitação, filtros, ordenação e paginação; `GET /games/{slug}`; `GET /genres`, `/platforms` e `/stores`.
- [x] Seed de desenvolvimento com 16 jogos reais (só no perfil `local`).

**Pronto quando:** buscar "witcher" encontra "The Witcher 3: Wild Hunt".

### 1.2 Integração com o IGDB · RF25, RF26

- [x] App na Twitch; token com cache e renovação.
- [x] Cliente HTTP com limite de 4 req/s, retry e timeout, testado com WireMock.
- [x] Importação por id do IGDB (upsert de gêneros, plataformas e metadados). As rotas de admin que a usam entram na 1.3, junto com os papéis.
- [x] Busca com fallback: se o resultado local for fraco, consulta o IGDB, importa e devolve.
- [x] Job de importação inicial dos jogos mais populares (começando com ~2 mil).

**Pronto quando:** a primeira busca por um jogo fora do banco traz o resultado do IGDB, e a segunda já sai do banco.

### 1.3 Contas e autenticação · RF01–RF08, RF26

- [x] Tabelas `users` e `refresh_tokens`; cadastro validado (RN08, RN09).
- [x] Login, refresh com rotação e logout; Spring Security com JWT.
- [x] `GET` e `PATCH /me` (nome, bio e gênero opcional, RN14), `PATCH /me/settings`, `PUT /me/password`, `DELETE /me`.
- [x] Limite de tentativas no login; CORS configurado por variável de ambiente.
- [x] `POST /admin/games/import` e `POST /admin/games/{id}/sync`, só para admin (RF26).

**Pronto quando:** há testes para 401 (sem token), 403 (sem permissão) e para o caso de um refresh reutilizado, que deve revogar a sessão.

### 1.4 Biblioteca · RF30–RF37

- [x] Tabela `library_entries` com as constraints; `Review`, `Playthrough` e `Acquisition` como `@Embeddable`.
- [x] Regras da RN02 num lugar só, com teste parametrizado por status.
- [x] `GET`, `PUT`, `PATCH` e `DELETE /me/library/{gameId}`, e `GET /me/library` com filtros.
- [x] Publicar o evento `LibraryEntryChanged`, ainda sem ouvintes (prepara as Fases 2 e 3).

**Pronto quando:** todas as células da tabela da RN02 têm teste.

### 1.5 Perfil público · RF40–RF42

- [x] `GET /users/{username}` e as abas `library`, `favorites` e `reviews`.
- [x] Perfil privado e omissão dos dados de aquisição (RN10).

**Pronto quando:** um teste garante que o valor pago nunca aparece numa rota pública quando "mostrar gastos" está desligado.

### 1.6 Estatísticas · RF43

- [x] `GET /me/stats` e `GET /users/{username}/stats`, com consultas agregadas.

**Pronto quando:** os números batem com um cenário de teste conhecido.

### 1.7 Jogo na comunidade · RF23, RF24

- [x] Nota média, distribuição, % que recomenda e contagens, só com perfis públicos (RN11).
- [x] `GET /games/{slug}/reviews` e `GET /reviews`.

**Pronto quando:** a página do jogo mostra os números da comunidade e as avaliações públicas.

### 1.8 Deploy

- [x] Imagem Docker (Dockerfile multi-stage), publicada no GHCR pelo CI a cada push na `main` depois dos testes.
- [ ] Banco no Neon e API no Render ou no Cloud Run, com deploy automático a partir da `main`.
- [ ] Swagger público e o link da demo no "About" do repositório.

**Pronto quando:** a API está no ar e o README tem o link.

### 1.9 Front-end do MVP (outro repositório)

- [x] Identidade visual definida a partir das referências em `refs/` ([08 · Front-end](08-frontend.md)).
- [x] Esqueleto em Vue 3 + TypeScript no repositório `game-log-frontend`.
- [x] Telas da Fase 1 de [02 · Telas](02-telas.md).

A partir da 1.4, as telas podem ser feitas em paralelo com o back-end, usando o Swagger como contrato.

## Fase 2 · Social e polimento

- [x] 2.1 Seguir, seguidores e seguidos (RF50)
- [x] 2.2 Feed de atividade a partir do evento `LibraryEntryChanged` (RF51)
- [x] 2.3 Curtidas em avaliações e ordenação por mais curtidas (RF52)
- [x] 2.4 Denúncias e tela de moderação (RF53)
- [x] 2.5 Listas personalizadas com reordenação, sobre a tabela `games` (RF54). O código de listas do curso ficou no histórico, no commit `932d3d4`, como referência.
- [x] 2.6 Favoritos em destaque, em ordem (RF38)
- [x] 2.7 E-mail: verificação e recuperação de senha (RF09)
- [x] 2.8 Exportação de dados (RF10)
- [x] 2.9 Cache com Caffeine e números da comunidade pré-calculados
- [x] 2.10 Logs estruturados com traceId e métricas

## Fase 3 · IA

Detalhes em [06 · Recomendações com IA](06-recomendacoes-ia.md).

- [x] 3.1 Catálogo maior (~10 mil jogos) com metadados completos
- [x] 3.2 pgvector, embeddings dos jogos e job de reindexação por hash (com um modelo local, por enquanto só para teste)
- [ ] 3.3 Jogos parecidos (RF60), comparados com o `similar_games` do IGDB
- [ ] 3.4 "Você poderá gostar" só com busca (RF61)
- [ ] 3.5 O Claude reordena e explica, com saída estruturada e fallback (RF62)
- [ ] 3.6 Feedback nas sugestões (RF63) e tela de primeiros passos (RF64)
- [ ] 3.7 Avaliação offline (Recall@10) e métricas de uso
- [ ] 3.8 (Opcional) Busca em linguagem natural (RF65)

## Ideias para depois

- Importar a biblioteca da Steam: a Steam Web API traz os jogos e as horas jogadas, e o IGDB mapeia os ids da Steam.
- Alerta de preço para a lista de desejos.
- Inicialização mais rápida da JVM (AOT cache do Java 25 ou imagem nativa com GraalVM), útil em hospedagem que hiberna.
- Metas anuais ("zerar 20 jogos em 2027") e retrospectiva do ano.
