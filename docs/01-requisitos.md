# 01 · Requisitos

## Visão

Para quem joga e quer lembrar o que jogou, o que achou e quanto gastou, o Game Log é um diário de jogos com perfil público. Diferente de uma planilha, ele já traz o catálogo pronto (capas, gêneros, plataformas) e mostra estatísticas. No futuro, também sugere o próximo jogo com base no gosto de cada um.

## Atores

| Ator | O que faz |
|---|---|
| Visitante | Navega pelo catálogo, pelas páginas de jogos e pelos perfis públicos, sem login |
| Usuário | Tudo o que o visitante faz, mais manter a própria biblioteca, as avaliações e as configurações |
| Administrador | Mantém o catálogo e modera denúncias |

## Escopo por fase

| Fase | Entrega |
|---|---|
| 0 · Fundação | Projeto atualizado, banco com migrations, CI, base de testes |
| 1 · MVP | Contas, catálogo (IGDB), biblioteca com avaliação e compra, favoritos, perfil público com abas, estatísticas, deploy |
| 2 · Social | Seguir, feed, curtidas, denúncias, listas personalizadas, e-mail, exportação de dados |
| 3 · IA | Jogos parecidos, "Você poderá gostar" com RAG, feedback das sugestões |

## Requisitos funcionais

A coluna **Fase** indica quando o requisito entra.

### Conta e autenticação

| ID | Requisito | Fase |
|---|---|---|
| RF01 | Cadastrar conta com username, e-mail e senha | 1 |
| RF02 | Entrar com username **ou** e-mail e senha | 1 |
| RF03 | Manter a sessão com refresh token e sair (revogar a sessão) | 1 |
| RF04 | Ver e editar o próprio perfil: nome de exibição, bio e gênero (opcional, ver RN14) | 1 |
| RF05 | Trocar a senha informando a atual | 1 |
| RF06 | Configurar a privacidade: perfil público ou privado, mostrar ou não os gastos | 1 |
| RF07 | Definir a moeda padrão (BRL, se nada for escolhido) | 1 |
| RF08 | Excluir a própria conta | 1 |
| RF09 | Verificar o e-mail e recuperar a senha por e-mail | 2 |
| RF10 | Exportar todos os meus dados (JSON ou CSV) | 2 |

### Catálogo

| ID | Requisito | Fase |
|---|---|---|
| RF20 | Buscar jogos pelo nome, tolerando acentos e erros de digitação | 1 |
| RF21 | Filtrar por gênero, plataforma e ano; ordenar por popularidade, nota e lançamento | 1 |
| RF22 | Ver a página do jogo: capa, resumo, gêneros, plataformas, desenvolvedora, lançamento | 1 |
| RF23 | Ver os números da comunidade no jogo: nota média, distribuição das notas, % que recomendam, quantos jogaram e quantos querem jogar | 1 |
| RF24 | Ler as avaliações públicas do jogo, das mais recentes para as mais antigas (Fase 2: também as mais curtidas) | 1 |
| RF25 | Trazer do IGDB, sem o usuário perceber, um jogo que ainda não está no banco quando ele aparece na busca | 1 |
| RF26 | Administrador importa ou ressincroniza um jogo pelo id do IGDB | 1 |
| RF27 | Administrador corrige os dados de um jogo manualmente | 2 |

### Biblioteca (o núcleo)

| ID | Requisito | Fase |
|---|---|---|
| RF30 | Adicionar um jogo à biblioteca com um status: **Lista de desejos**, **Quero jogar**, **Jogando**, **Jogado** ou **Abandonado** | 1 |
| RF31 | Mudar o status de um jogo (ex.: de Quero jogar para Jogando) | 1 |
| RF32 | Avaliar: nota de 0 a 5 estrelas, com frações de 0,25 (ex.: 4,75), texto opcional, se recomendaria (sim/não) e aviso de spoiler | 1 |
| RF33 | Registrar a jogatina (opcional): plataforma, horas jogadas, início, término e se zerou | 1 |
| RF34 | Registrar a aquisição (opcional): como conseguiu (compra, presente, assinatura, gratuito), loja, valor pago, moeda e data | 1 |
| RF35 | Marcar e desmarcar como favorito | 1 |
| RF36 | Remover um jogo da biblioteca | 1 |
| RF37 | Listar a própria biblioteca com filtros (status, favorito, gênero, plataforma, nota, recomenda) e ordenação (adicionado, atualizado, nota, título, término) | 1 |
| RF38 | Escolher até 5 favoritos em destaque, em ordem, para o topo do perfil | 2 |

### Perfil público

| ID | Requisito | Fase |
|---|---|---|
| RF40 | Abrir o perfil de qualquer usuário em `/u/{username}`, sem login | 1 |
| RF41 | Abas do perfil: Visão geral, Jogados, Jogando, Quero jogar, Lista de desejos, Favoritos, Avaliações, Estatísticas | 1 |
| RF42 | Num perfil privado, quem não é o dono vê só o cabeçalho | 1 |
| RF43 | Estatísticas: jogos por status, por ano, por gênero e por plataforma, distribuição de notas, horas jogadas; gastos (total, por loja, por ano) só se o dono permitir | 1 |

### Social

| ID | Requisito | Fase |
|---|---|---|
| RF50 | Seguir e deixar de seguir usuários; ver seguidores e seguidos | 2 |
| RF51 | Feed com a atividade de quem eu sigo | 2 |
| RF52 | Curtir avaliações | 2 |
| RF53 | Denunciar uma avaliação; o administrador modera | 2 |
| RF54 | Criar listas personalizadas e ordenáveis ("Top 10 RPGs") | 2 |

### Recomendações (IA)

| ID | Requisito | Fase |
|---|---|---|
| RF60 | "Jogos parecidos" na página do jogo | 3 |
| RF61 | "Você poderá gostar": sugestões personalizadas, sem jogos que já estão na biblioteca | 3 |
| RF62 | Cada sugestão traz um motivo curto, citando jogos que o usuário curtiu | 3 |
| RF63 | Dar feedback na sugestão: "não tenho interesse" ou "já joguei" | 3 |
| RF64 | Usuário novo escolhe alguns jogos que ama para começar a receber sugestões | 3 |
| RF65 | (Opcional) Busca em linguagem natural: "terror curto para jogar em dupla" | 3 |

## Regras de negócio

**RN01 · Uma entrada por jogo.** Cada usuário tem no máximo uma entrada por jogo. O status muda; o jogo não se repete.

**RN02 · Campos permitidos por status.** Vale o estado final da entrada. Se algum campo não for permitido no status escolhido, a API recusa com 422 em vez de apagar o dado sem avisar.

| Campo | Lista de desejos | Quero jogar | Jogando | Jogado | Abandonado |
|---|:-:|:-:|:-:|:-:|:-:|
| Nota, texto, recomenda, spoiler | — | — | ✓ | ✓ | ✓ |
| Plataforma, horas, início | — | — | ✓ | ✓ | ✓ |
| Término | — | — | — | ✓ | ✓ |
| Zerou | — | — | — | ✓ | — |
| Favorito | — | — | ✓ | ✓ | — |
| Aquisição (loja, valor, data) | — | ✓ | ✓ | ✓ | ✓ |

Exemplo: um jogo avaliado só volta para Lista de desejos depois que a avaliação for removida. Para rejogar, use **Jogando**; a avaliação continua.

**RN03 · Nota.** De 0 a 5 estrelas, em passos de 0,25: 4,75 vale, 4,8 não. "Sem nota" (`null`) é diferente de nota zero.

**RN04 · Texto da avaliação.** Opcional, até 2.000 caracteres, texto puro (sem HTML).

**RN05 · Valor pago.** Só existe quando a aquisição é **compra**. É maior ou igual a zero, tem duas casas decimais e sempre vem com a moeda (ISO 4217). Os totais de gastos são somados por moeda, sem conversão.

**RN06 · Datas.** Início, término e compra não podem estar no futuro, e o término não pode ser anterior ao início.

**RN07 · Plataforma jogada.** Pode ser qualquer plataforma do catálogo, não só as oficiais do jogo (retrocompatibilidade e emulação existem).

**RN08 · Username.** De 3 a 20 caracteres: letras minúsculas, números e `_`. É único sem diferenciar maiúsculas de minúsculas. Palavras reservadas são bloqueadas (`admin`, `api`, `me`, `settings`, `login`, `signup`, `games`, `users`...).

**RN09 · Senha.** De 8 a 64 caracteres, porque o BCrypt só considera os primeiros 72 bytes.

**RN10 · Privacidade.** Num perfil privado, os visitantes veem só o username e o nome. Loja e valor pago nunca aparecem em respostas públicas, a menos que o dono ative "mostrar gastos".

**RN11 · Números da comunidade.** Nota média, % que recomenda e contagens consideram só perfis públicos. Com poucos usuários, incluir os privados poderia expor a nota de alguém.

**RN12 · Exclusão de conta.** Apaga os dados pessoais, a biblioteca e as avaliações.

**RN13 · Sugestões.** Nunca sugerem um jogo que já está na biblioteca (em qualquer status) nem um jogo marcado como "não tenho interesse".

**RN14 · Gênero do usuário.** Opcional, com as opções Feminino, Masculino, Não binário e Outro. "Prefiro não informar" equivale a não ter gênero definido (`null`), que é o padrão. Pode ser alterado ou removido a qualquer momento. Se preenchido, aparece no cabeçalho do perfil público (num perfil privado, não aparece). Não é usado nas recomendações nem nos números da comunidade.

**RN15 · Seguir.** Qualquer perfil pode ser seguido, inclusive um privado, menos o próprio. Seguir não dá acesso a nada: num perfil privado, as listas de seguidores e de seguidos ficam escondidas como as abas (RN10).

## Requisitos não funcionais

| ID | Requisito |
|---|---|
| RNF01 | API REST versionada (`/api/v1`), em JSON, documentada com OpenAPI/Swagger |
| RNF02 | Erros no formato Problem Details (RFC 9457), com a lista de campos inválidos |
| RNF03 | Toda listagem é paginada, com no máximo 50 itens por página |
| RNF04 | Senhas com BCrypt, JWT de curta duração, CORS restrito e limite de tentativas no login |
| RNF05 | O usuário só altera os próprios dados: escrita sempre em `/me/...`, nunca pelo id de outro usuário |
| RNF06 | PostgreSQL com migrations versionadas (Flyway); nada de `ddl-auto` fora dos testes |
| RNF07 | Leituras comuns abaixo de 300 ms (p95), com índices adequados e sem N+1 |
| RNF08 | Testes unitários e de integração com PostgreSQL real (Testcontainers); cobertura mínima de 80% nas regras de negócio |
| RNF09 | CI no GitHub Actions a cada PR e deploy automático a partir da `main` |
| RNF10 | Configuração por variáveis de ambiente, sem nenhum segredo no repositório |
| RNF11 | Health check, métricas e logs estruturados |
| RNF12 | LGPD: coletar o mínimo, permitir exportar e excluir os dados, gastos privados por padrão |
| RNF13 | Respeitar os limites e os termos da API do IGDB (4 req/s), com cache e crédito ao IGDB |
| RNF14 | A aplicação funciona sem a IA: se o provedor cair, as sugestões viram "populares nos seus gêneros" |
