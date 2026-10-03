# Game Log · API

[![CI](https://github.com/kauanunnes/game-log-backend/actions/workflows/ci.yml/badge.svg)](https://github.com/kauanunnes/game-log-backend/actions/workflows/ci.yml)

API do Game Log, um diário de jogos com perfil público: cada pessoa registra o que jogou, a nota, se recomenda e quanto pagou, e organiza o que quer jogar. O front-end fica no repositório `game-log-frontend`.

## Stack

Java 25 · Spring Boot 4.1 · Spring Data JPA · PostgreSQL 18 · Flyway · Testcontainers · WireMock · springdoc-openapi

## Rodando

Requisitos: JDK 25 e Docker Desktop aberto.

```bash
./mvnw spring-boot:run
```

O Spring sobe o PostgreSQL do `compose.yaml` (porta 5433), aplica as migrations e o seed de exemplo. A API responde em `http://localhost:8080/api/v1`.

| Endereço | O quê |
| --- | --- |
| `http://localhost:8080/swagger-ui.html` | Documentação interativa da API |
| `http://localhost:8080/actuator/health` | Saúde da aplicação e do banco |

### Catálogo do IGDB

Os dados dos jogos vêm do [IGDB](https://www.igdb.com). Sem credenciais, a API usa só os jogos que já estão no banco. Para consultar o IGDB, crie um app no [console da Twitch](https://dev.twitch.tv/console/apps) e coloque as credenciais num `.env` na raiz, que o perfil local lê:

```dotenv
IGDB_CLIENT_ID=...
IGDB_CLIENT_SECRET=...
```

Ao subir, a aplicação confere as credenciais e mostra no log `IGDB ligado` ou o motivo de estar desligado. Daí em diante, uma busca com poucos resultados também consulta o IGDB e guarda o que encontrar. Para trazer de uma vez os jogos mais populares:

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments=--game-log.igdb.bootstrap-limit=2000
```

### Contas

O cadastro e o login devolvem um access token (15 minutos) e deixam o refresh token num cookie `HttpOnly`. No Swagger, use o botão **Authorize** com o access token. Não existe rota que crie administrador: o papel é dado direto no banco, e o token só sai com ele depois de entrar de novo.

```sql
UPDATE users SET role = 'ADMIN' WHERE username = 'seu_username';
```

## Testes e qualidade

```bash
./mvnw verify
```

Roda os testes contra um PostgreSQL 18 em container (Testcontainers) e um IGDB simulado (WireMock), confere a formatação (Spotless) e gera o relatório de cobertura em `target/site/jacoco/index.html`. Para corrigir a formatação: `./mvnw spotless:apply`.

## Produção

A imagem roda com o perfil `prod` e lê a configuração das variáveis listadas em [`.env.example`](.env.example). Sem `JWT_PRIVATE_KEY`, ela não sobe.

```bash
docker build -t game-log-api .
docker run -p 8080:8080 --env-file .env.prod game-log-api
```

A cada push na `main`, o CI roda os testes e publica a imagem em `ghcr.io/kauanunnes/game-log-backend`.

### Deploy no Render com o banco no Neon

1. No Neon, copie a connection string da branch de produção. A URL do JDBC é a mesma sem o usuário e a senha, que vão em variáveis próprias: `jdbc:postgresql://<host>/<banco>?sslmode=require`.
2. No Render, crie um Web Service a partir da imagem `ghcr.io/kauanunnes/game-log-backend:latest`, com health check em `/actuator/health`.
3. Defina `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `JWT_PRIVATE_KEY`, `IGDB_CLIENT_ID`, `IGDB_CLIENT_SECRET` e `CORS_ORIGINS`.
4. Copie o deploy hook do serviço para o segredo `RENDER_DEPLOY_HOOK_URL` do repositório. Daí em diante, cada push na `main` que passar nos testes vira um deploy.
5. Para encher o catálogo na primeira vez, suba uma vez com `GAME_LOG_IGDB_BOOTSTRAP_LIMIT=2000` e depois remova a variável.

## Documentação

Requisitos, telas, modelo de dados, API, arquitetura e roadmap ficam em [`docs/`](docs/README.md).

## Licença

[MIT](LICENSE)
