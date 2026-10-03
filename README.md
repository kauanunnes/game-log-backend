# Game Log · API

[![CI](https://github.com/kauanunnes/gamelog-backend/actions/workflows/ci.yml/badge.svg)](https://github.com/kauanunnes/gamelog-backend/actions/workflows/ci.yml)

API do Game Log, um diário de jogos com perfil público: cada pessoa registra o que jogou, a nota, se recomenda e quanto pagou, e organiza o que quer jogar. O front-end fica no repositório `game-log-frontend`.

## Stack

Java 25 · Spring Boot 4.1 · Spring Data JPA · PostgreSQL 18 · Flyway · Testcontainers · springdoc-openapi

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

## Testes e qualidade

```bash
./mvnw verify
```

Roda os testes contra um PostgreSQL 18 em container (Testcontainers), confere a formatação (Spotless) e gera o relatório de cobertura em `target/site/jacoco/index.html`. Para corrigir a formatação: `./mvnw spotless:apply`.

## Produção

Defina `SPRING_PROFILES_ACTIVE=prod` e as variáveis listadas em [`.env.example`](.env.example).

## Documentação

Requisitos, telas, modelo de dados, API, arquitetura e roadmap ficam em [`docs/`](docs/README.md).

## Licença

[MIT](LICENSE)
