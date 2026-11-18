# Pokédex Full Stack: Atividade 04

Continuação da **Atividade 03 (Pokédex com React + Vite)**. O arquivo `pokedex-data.json` foi substituído por uma **API REST em Spring Boot** com **MongoDB**, populado a partir do [`Script_Pokedex.sql`](Script_Pokedex.sql) da atividade.

```text
Script_Pokedex.sql ──► MongoDB ──► Spring Boot REST API ──► React + Vite
   (importado na        (pokemons,      (/api/pokemons,        (interface da
    inicialização)       types,          /api/types,            Atividade 03 +
                         trainers)       /api/trainers)         cadastro/edição/exclusão)
```

## Prévia

![Tela principal da Pokédex](docs/pokedex-desktop.png)

| Detalhes com editar e excluir | Formulário de edição |
| --- | --- |
| ![Detalhes do Charizard](docs/pokedex-detalhes.png) | ![Formulário de edição](docs/pokedex-formulario.png) |

[Ver a interface no celular](docs/pokedex-mobile.png).

## Sumário

1. [Tecnologias](#tecnologias)
2. [Estrutura do repositório](#estrutura-do-repositório)
3. [Pré-requisitos](#pré-requisitos)
4. [Configuração do MongoDB](#configuração-do-mongodb)
5. [Executar o backend](#executar-o-backend)
6. [Executar o frontend](#executar-o-frontend)
7. [Modelagem: do SQL para documentos](#modelagem-do-sql-para-documentos)
8. [API REST](#api-rest)
9. [Funcionalidades da interface](#funcionalidades-da-interface)
10. [Testes](#testes)
11. [Variáveis de ambiente](#variáveis-de-ambiente)
12. [Solução de problemas](#solução-de-problemas)
13. [Entrega](#entrega)

## Tecnologias

| Camada | Tecnologias |
| --- | --- |
| Banco de dados | MongoDB 8 (MongoDB Atlas ou Docker local) |
| Backend | Java 21, Spring Boot 4.1, Spring Web MVC, Spring Data MongoDB, Bean Validation, Maven Wrapper |
| Frontend | React 19, Vite 8, JavaScript, CSS |
| Testes | JUnit 5, MockMvc, Testcontainers (MongoDB real), `node:test` |

O backend segue a divisão pedida na atividade:

```text
controller/  →  service/  →  repository/  →  model/
(HTTP/JSON)     (regras)     (Spring Data)    (documentos do MongoDB)
```

## Estrutura do repositório

```text
.
├── Script_Pokedex.sql          # Fonte dos dados (fornecido na atividade)
├── docker-compose.yml          # MongoDB local opcional
├── docs/                       # Capturas da interface
├── backend/                    # API REST Spring Boot
│   ├── pom.xml, mvnw, mvnw.cmd # Maven Wrapper: não precisa instalar o Maven
│   ├── .env.example            # Modelo de configuração (copie para .env)
│   ├── http/                   # Coleção do Postman/Insomnia e arquivo .http
│   └── src/main/java/com/pokedex/api/
│       ├── controller/         # PokemonController, PokemonTypeController, TrainerController
│       ├── service/            # PokemonService, PokemonTypeService, TrainerService
│       ├── repository/         # Repositórios Spring Data + consultas com MongoTemplate
│       ├── model/              # Pokemon, PokemonType, Trainer (@Document)
│       ├── dto/                # PokemonRequest (corpo validado do POST/PUT)
│       ├── exception/          # Erros em Problem Details (RFC 9457), em português
│       ├── seed/               # Leitura do Script_Pokedex.sql e importação
│       └── config/             # CORS, MongoDB e propriedades da aplicação
└── frontend/                   # Pokédex da Atividade 03 integrada à API
    ├── src/services/api.js     # Cliente HTTP da API
    ├── src/hooks/usePokedex.js # Carregamento e operações de cadastro/edição/exclusão
    ├── src/components/         # Componentes da Atividade 03 + PokemonForm, ConfirmDelete…
    ├── src/data/pokedex.js     # Conversão documento ↔ interface, pesquisa, filtros, validação
    ├── tests/                  # Testes do frontend (npm test)
    └── vite.config.js          # Proxy /api → backend
```

## Pré-requisitos

- **Java 21** (JDK). Confira com `java -version`. O Maven **não** precisa ser instalado, pois o projeto traz o `mvnw`.
- **Node.js 22.12 ou superior**, com npm. Confira com `node -v`.
- Um **MongoDB**: conta gratuita no [MongoDB Atlas](https://www.mongodb.com/atlas) (recomendado) **ou** Docker para rodar um MongoDB local.

## Configuração do MongoDB

As credenciais **nunca** vão para o GitHub: o backend lê a conexão da variável de ambiente `MONGODB_URI` ou do arquivo `backend/.env`, que está no `.gitignore`. O repositório traz apenas o modelo `backend/.env.example`.

### Opção A: MongoDB Atlas (recomendado)

1. Crie um cluster gratuito (**M0**) em [cloud.mongodb.com](https://cloud.mongodb.com).
2. Em **Database Access**, crie um usuário com senha e o papel *Read and write to any database*.
3. Em **Network Access**, adicione o seu IP (*Add Current IP Address*). Para testes, `0.0.0.0/0` libera qualquer IP.
4. Em **Connect → Drivers**, copie a *connection string*, que tem o formato `mongodb+srv://<usuario>:<senha>@<cluster>.mongodb.net/?retryWrites=true&w=majority`.
5. Crie o arquivo de configuração a partir do modelo:

   ```bash
   cd backend
   cp .env.example .env        # Windows (PowerShell): Copy-Item .env.example .env
   ```

6. Edite `backend/.env` e preencha a string copiada, com usuário e senha reais:

   ```properties
   MONGODB_URI=mongodb+srv://usuario:senha@cluster0.xxxxx.mongodb.net/?retryWrites=true&w=majority
   MONGODB_DATABASE=pokedex
   ```

   Se a senha tiver caracteres especiais (`@ : / ? # %`), use a versão codificada para URL (ex.: `@` → `%40`).

Também é possível não usar o arquivo e definir a variável só no terminal:

```bash
export MONGODB_URI='mongodb+srv://...'          # Linux/macOS
$env:MONGODB_URI = 'mongodb+srv://...'          # Windows PowerShell
```

### Opção B: MongoDB local com Docker

Na raiz do repositório:

```bash
docker compose up -d        # sobe o MongoDB 8 em localhost:27017
```

O valor padrão de `MONGODB_URI` já é `mongodb://localhost:27017`, então não é preciso criar o `.env`. Para parar: `docker compose down`. Para apagar também os dados: `docker compose down -v`.

### Importação do `Script_Pokedex.sql`

Não é preciso rodar nenhum script manualmente. Ao iniciar, o backend lê o `Script_Pokedex.sql` (empacotado pelo Maven a partir da raiz do repositório), converte as tabelas em documentos e grava as coleções `pokemons`, `types` e `trainers`. O log mostra:

```text
Script_Pokedex.sql importado: 151 Pokémon, 18 tipos, 12 treinadores
```

Por padrão (`POKEDEX_SEED_MODE=if-empty`), cada coleção só é importada se estiver vazia: cadastros, edições e exclusões feitos pela aplicação **continuam salvos** entre reinicializações. Para restaurar os dados originais do script, inicie uma vez com `POKEDEX_SEED_MODE=always`:

```bash
POKEDEX_SEED_MODE=always ./mvnw spring-boot:run                       # Linux/macOS
$env:POKEDEX_SEED_MODE='always'; .\mvnw.cmd spring-boot:run           # Windows PowerShell
```

## Executar o backend

```bash
cd backend
./mvnw spring-boot:run          # Windows: .\mvnw.cmd spring-boot:run
```

A API sobe em **http://localhost:8080**. Abrir esse endereço no navegador mostra a lista de endpoints. A primeira execução baixa as dependências do Maven e pode levar alguns minutos.

Outras formas de executar:

```bash
./mvnw clean package            # gera target/pokedex-api-0.0.1-SNAPSHOT.jar (roda os testes)
java -jar target/pokedex-api-0.0.1-SNAPSHOT.jar

./mvnw spring-boot:test-run     # sobe a API com um MongoDB temporário no Docker (Testcontainers)
```

## Executar o frontend

Em outro terminal, com o backend em execução:

```bash
cd frontend
npm install
npm run dev
```

Abra **http://localhost:5173** (ou http://127.0.0.1:5173). O Vite encaminha as chamadas de `/api` para `http://127.0.0.1:8080`, então não é preciso configurar nada. No PowerShell, se a política de execução bloquear `npm.ps1`, use `npm.cmd install` e `npm.cmd run dev`.

Versão de produção:

```bash
npm run build
npm run preview                 # http://localhost:4173, também com o proxy /api
```

Se o backend não estiver no ar, a interface mostra o aviso **"Não foi possível carregar a Pokédex"** com o botão **Tentar novamente**.

## Modelagem: do SQL para documentos

O script tem cinco tabelas relacionais. Elas foram adaptadas ao modelo de documentos de acordo com a forma como a aplicação lê os dados:

| Tabelas no `Script_Pokedex.sql` | Coleção no MongoDB | Decisão |
| --- | --- | --- |
| `Pokemon` + `Rel_Pokemon_Type` + `Types` | `pokemons` | Atributos e **nomes dos tipos embutidos** no documento: os tipos são poucos, quase não mudam e sempre aparecem junto com o Pokémon, então uma única leitura traz tudo. |
| `Types` | `types` | Catálogo usado para validar cadastros e montar os filtros. |
| `Trainers` + `Rel_Trainer_Pokemon` | `trainers` | Equipe guardada **por referência** (`pokemonIds`): a relação é N:N e o Pokémon é editado ou excluído de forma independente. |

Documentos gerados (o `_id` é o número da Pokédex):

```json
// pokemons
{ "_id": 25, "name": "Pikachu", "hp": 35, "attack": 55, "defense": 30,
  "specialAttack": 50, "specialDefense": 40, "speed": 90, "types": ["Electric"] }

// types
{ "_id": 5, "name": "Electric" }

// trainers
{ "_id": 6, "name": "Koga", "badges": 5, "pokemonIds": [48, 48, 48, 49] }
```

Detalhes do script que foram preservados:

- **Mew** (#151) não aparece em `Rel_Pokemon_Type`, então fica com `types: []`. A interface o mostra como **Sem tipo**, e esse filtro (`Typeless` na API) o encontra.
- **Sombrio** (`Dark`) e **Typeless** existem no catálogo, mas nenhum Pokémon tem esses tipos no script.
- `'Farfetch''d'` usa o escape SQL de aspas e vira `Farfetch'd`.
- As repetições de `Rel_Trainer_Pokemon` são mantidas: Koga tem três Venonat e Lance tem dois Dragonite.
- Ao excluir um Pokémon, a API também o retira das equipes dos treinadores (`$pull`).

A conversão foi conferida contra o `pokedex-data.json` da Atividade 03: os 151 Pokémon, 18 tipos e 12 treinadores servidos pela API são idênticos ao JSON anterior.

## API REST

Base: `http://localhost:8080`

### Endpoints obrigatórios

| Método | Endpoint | Descrição | Respostas |
| --- | --- | --- | --- |
| `GET` | `/api/pokemons` | Lista todos os Pokémon, ordenados pelo número | 200 |
| `GET` | `/api/pokemons/{id}` | Busca um Pokémon pelo número | 200, 404 |
| `POST` | `/api/pokemons` | Cadastra um Pokémon | 201 + `Location`, 400, 409 |
| `PUT` | `/api/pokemons/{id}` | Atualiza um Pokémon | 200, 400, 404, 409 |
| `DELETE` | `/api/pokemons/{id}` | Remove um Pokémon (e o retira das equipes) | 204, 404 |

### Desafios opcionais implementados

| Método | Endpoint | Descrição |
| --- | --- | --- |
| `GET` | `/api/pokemons?name=char` | Busca por trecho do nome, sem diferenciar maiúsculas |
| `GET` | `/api/pokemons?type=Fire&type=Flying` | Consulta por tipo (precisa ter todos os tipos informados; `type=Typeless` traz os sem tipo) |
| `GET` | `/api/pokemons/{id}/trainers` | Treinadores que têm o Pokémon na equipe |
| `GET` | `/api/types` | Catálogo de tipos |
| `GET` | `/api/trainers` | Treinadores persistidos no MongoDB |
| `GET` | `/api/trainers/{id}` | Um treinador |
| `GET` | `/api/trainers/{id}/pokemons` | Equipe do treinador com os dados completos dos Pokémon |

### Corpo do `POST` / `PUT`

```json
{
  "name": "Voltix",
  "hp": 70,
  "attack": 80,
  "defense": 50,
  "specialAttack": 95,
  "specialDefense": 60,
  "speed": 110,
  "types": ["Electric", "Steel"]
}
```

Regras de validação:

- `name` é obrigatório (até 30 caracteres) e único, sem diferenciar maiúsculas.
- Os seis atributos são obrigatórios, inteiros de 1 a 255.
- `types` aceita de 0 a 2 nomes do catálogo, sem diferenciar maiúsculas (`"fire"` é salvo como `"Fire"`). Lista vazia significa Pokémon sem tipo.
- No `POST`, `id` é opcional: sem ele, a API usa o próximo número livre. No `PUT`, o `id` vem da URL e não pode ser alterado.

Os erros seguem o padrão **Problem Details (RFC 9457)**, com mensagens em português e o campo `errors` indicando cada campo inválido:

```json
{
  "status": 409,
  "title": "Conflito",
  "detail": "O nome Pikachu já pertence ao Pokémon 25.",
  "instance": "/api/pokemons",
  "errors": { "name": "O nome Pikachu já pertence ao Pokémon 25." }
}
```

### Testar com Postman, Insomnia ou Thunder Client

Importe [`backend/http/pokedex-api.postman_collection.json`](backend/http/pokedex-api.postman_collection.json). A coleção executa o CRUD em sequência (o `id` cadastrado é reaproveitado no `PUT` e no `DELETE`), as buscas opcionais e os erros esperados, cada requisição com teste de status. Para o REST Client do VS Code ou o HTTP Client do IntelliJ, use [`backend/http/pokedex-api.http`](backend/http/pokedex-api.http).

Exemplos com `curl`:

```bash
curl http://localhost:8080/api/pokemons/25
curl "http://localhost:8080/api/pokemons?type=Grass&type=Poison"
curl -X POST http://localhost:8080/api/pokemons -H "Content-Type: application/json" \
     -d '{"name":"Voltix","hp":70,"attack":80,"defense":50,"specialAttack":95,"specialDefense":60,"speed":110,"types":["Electric"]}'
curl -X PUT http://localhost:8080/api/pokemons/152 -H "Content-Type: application/json" \
     -d '{"name":"Voltix","hp":75,"attack":80,"defense":50,"specialAttack":95,"specialDefense":60,"speed":120,"types":["Electric","Steel"]}'
curl -X DELETE http://localhost:8080/api/pokemons/152
```

## Funcionalidades da interface

Mantidas da Atividade 03, agora com dados vindos da API:

- 🔎 **Pesquisa** por parte do nome ou pelo número (`25`, `025` ou `#025`).
- 🧬 **Filtro por tipo**: até dois tipos, e o Pokémon precisa ter ambos. **Sem tipo** encontra o Mew.
- 📄 **Detalhes** do Pokémon com navegação para o anterior e o próximo e os treinadores que o possuem.
- 📊 **Atributos**: os seis atributos base nos cards e em barras nos detalhes, com total.
- ⭐ Favoritos salvos no navegador, ordenação por número, nome ou atributo, Pokémon aleatório e página de treinadores com insígnias e equipes.

Novas nesta atividade:

- ➕ **Cadastrar**: botão **Novo Pokémon** na barra de resultados.
- ✏️ **Editar**: botão **Editar** nos detalhes. O número da Pokédex fica bloqueado.
- 🗑️ **Excluir**: botão **Excluir** nos detalhes, com confirmação que avisa de quais equipes o Pokémon vai sair.
- O formulário valida os dados no navegador com as mesmas regras da API e também mostra os erros devolvidos pelo backend (ex.: nome repetido).
- Avisos de confirmação após cada operação, estado de carregamento e tela de erro quando a API não responde.

Pokémon cadastrados com número acima de 151 não têm ilustração local e aparecem com o ícone de Pokébola.

## Testes

### Backend: 29 testes

```bash
cd backend
./mvnw test                     # Windows: .\mvnw.cmd test
```

- `SqlScriptParserTest` e `PokedexSqlImporterTest`: leitura do `Script_Pokedex.sql` real (151 Pokémon, 18 tipos, 12 treinadores, 214 relações de tipo, 43 de equipe; Pikachu igual ao exemplo do enunciado).
- `PokemonControllerTest`: contrato HTTP dos cinco endpoints (status, JSON, validação, Problem Details).
- `PokedexApiIntegrationTest`: aplicação completa com um **MongoDB real** em container (Testcontainers), percorrendo o CRUD, as buscas e a atualização das equipes. Esse teste exige Docker e é **ignorado automaticamente** quando o Docker não está disponível.

### Frontend: 19 testes

```bash
cd frontend
npm test
```

Cobrem a conversão entre a API e a interface, a pesquisa, os filtros, a ordenação, a validação do formulário e o cliente HTTP (com `fetch` simulado).

### Verificação manual sugerida

1. A lista inicial mostra os 151 Pokémon vindos da API, com os seis atributos.
2. `Bulba` encontra Bulbasaur; `25`, `025` e `#025` encontram Pikachu.
3. Planta + Veneno encontra Bulbasaur; Fogo + Água não tem resultados; Sem tipo encontra Mew.
4. **Novo Pokémon** cadastra um Pokémon, que aparece na lista, na pesquisa e no filtro de tipo, e continua lá depois de recarregar a página.
5. **Editar** altera nome, atributos e tipos, e a alteração persiste após recarregar.
6. **Excluir** remove o Pokémon da lista e das equipes de treinadores (ex.: excluir Onix deixa Brock só com Geodude).
7. Com o backend desligado, a interface mostra o aviso de erro; ao religar, **Tentar novamente** recarrega os dados.

## Variáveis de ambiente

Backend (variável de ambiente ou `backend/.env`):

| Variável | Padrão | Descrição |
| --- | --- | --- |
| `MONGODB_URI` | `mongodb://localhost:27017` | Conexão com o MongoDB (Atlas ou local) |
| `MONGODB_DATABASE` | `pokedex` | Nome do banco |
| `POKEDEX_SEED_MODE` | `if-empty` | `if-empty`, `always` (reimporta o script) ou `never` |
| `PORT` | `8080` | Porta da API |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://127.0.0.1:5173,http://localhost:4173,http://127.0.0.1:4173` | Origens autorizadas a chamar a API diretamente |

Frontend (opcional, em `frontend/.env`; veja `frontend/.env.example`):

| Variável | Padrão | Descrição |
| --- | --- | --- |
| `API_PROXY_TARGET` | `http://127.0.0.1:8080` | Para onde o Vite encaminha `/api` em `dev` e `preview` |
| `VITE_API_URL` | `/api` | Endereço da API usado pelo navegador (ex.: um backend publicado). Lembre de liberar a origem em `CORS_ALLOWED_ORIGINS`. |

## Solução de problemas

| Sintoma | Causa provável e solução |
| --- | --- |
| Backend encerra com `Não foi possível conectar ao MongoDB` (após ~30 s) | O MongoDB não está acessível. Local: rode `docker compose up -d`. Atlas: confira a `MONGODB_URI` e se o seu IP está em **Network Access**. |
| `Authentication failed` | Usuário ou senha errados na `MONGODB_URI`, ou senha com caracteres especiais sem codificação para URL. |
| `Port 8080 was already in use` | Outro processo usa a porta. Encerre-o ou defina `PORT=8081` e `API_PROXY_TARGET=http://127.0.0.1:8081`. |
| Interface mostra "Não foi possível carregar a Pokédex" | O backend não está rodando ou está em outra porta. |
| `The JAVA_HOME environment variable is not defined correctly` ao rodar o `mvnw` | Instale o JDK 21 e configure a variável `JAVA_HOME` (ou deixe o `java` disponível no `PATH`). |
| Os dados não voltam ao original após reiniciar | É o comportamento esperado (as alterações persistem). Use `POKEDEX_SEED_MODE=always` uma vez para reimportar o script. |

## Imagens

As ilustrações vêm do repositório público [PokeAPI/sprites](https://github.com/PokeAPI/sprites) (`sprites/pokemon/other/official-artwork/{id}.png`) e estão em `frontend/public/images/pokemon/{id}.png`, servidas localmente. Elas complementam apenas a apresentação visual. Os dados vêm do MongoDB, populado pelo `Script_Pokedex.sql`.

Pokémon e seus personagens pertencem aos respectivos titulares. Este projeto foi preparado para uso educacional.

## Entrega

A atividade é feita em dupla com **Pair Programming**, e os dois integrantes participam ativamente.

| Integrante | Contribuições |
| --- | --- |
| _Nome do integrante 1_ | _Preencher_ |
| _Nome do integrante 2_ | _Preencher_ |

- [x] Backend Spring Boot (Controller, Service, Repository, Model) com MongoDB.
- [x] `Script_Pokedex.sql` como fonte dos dados do MongoDB.
- [x] API REST com `GET`, `GET /{id}`, `POST`, `PUT` e `DELETE` em `/api/pokemons`.
- [x] Frontend da Atividade 03 integrado à API (sem o `pokedex-data.json`).
- [x] Cadastro, edição e exclusão pela interface, persistindo no MongoDB.
- [x] Credenciais fora do GitHub (`.env` ignorado; `.env.example` como modelo).
- [x] Desafios opcionais: consulta por tipo, busca por nome, treinadores no MongoDB e relação treinador ↔ Pokémon.
- [ ] Preencher os integrantes e suas contribuições.
- [ ] Configurar o MongoDB Atlas e testar a aplicação com ele.
- [ ] Publicar o repositório no GitHub.
- [ ] **Cada um dos dois integrantes** entregar no Canvas o link do repositório.
