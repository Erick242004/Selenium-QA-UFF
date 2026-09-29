# aulaSelenium-01 — Testes E2E com Selenium

Testes end-to-end (Java + Selenium 4 + JUnit 5) do site https://automationexercise.com, cobrindo:

- **Test Case 3** — Login com e-mail e senha incorretos (`LoginIncorretoTest`)
- **Test Case 1** — Registrar usuário (`RegistrarUsuarioTest`)

Os dados de entrada foram escolhidos com **particionamento em classes de equivalência (CE)** e **análise de valor limite (VL)**.

## Como executar

Pré-requisitos: JDK 11+, Maven e Google Chrome instalados.

```bash
mvn test                      # abre o Chrome
mvn test -Dheadless=true      # sem interface gráfica
mvn test -Dtest=LoginIncorretoTest
```

O `WebDriverManager` baixa o chromedriver automaticamente.

## Estrutura

```
src/test/java/br/com/aulaSelenium/
├── BaseTest.java              # setup do driver e passos reutilizáveis
├── LoginIncorretoTest.java    # Test Case 3
└── RegistrarUsuarioTest.java  # Test Case 1
```

## Test Case 3 — Login com dados incorretos

Resultado esperado quando o servidor rejeita: mensagem **"Your email or password is incorrect!"**.
Quando a entrada é inválida para o navegador (HTML5), o formulário não é enviado e o usuário permanece em `/login`.

| ID  | Partição / limite | E-mail | Senha | Resultado esperado |
|-----|-------------------|--------|-------|--------------------|
| CE1 | E-mail válido, não cadastrado | `naocadastrado@teste.com` | `SenhaErrada123` | Mensagem de erro |
| VL1 | Limite inferior | `a@b.co` | `1` | Mensagem de erro |
| VL2 | Limite superior (local 64 / senha 128) | 64×`a` + `@teste.com` | 128×`S` | Mensagem de erro |
| VL3 | Acima do limite (local 65) | 65×`a` + `@teste.com` | `SenhaErrada123` | Mensagem de erro |
| CE2 | Caracteres especiais | `usuario+tag@sub.dominio.com.br` | `P@ssw0rd!#$%&*` | Mensagem de erro |
| CE3 | Texto tipo SQL | `naocadastrado@teste.com` | `' OR '1'='1` | Mensagem de erro |
| CE4 | E-mail vazio | (vazio) | `Senha123` | Formulário não enviado |
| CE5 | E-mail sem `@` | `usuario.semarroba.com` | `Senha123` | Formulário não enviado |
| CE6 | E-mail sem domínio | `usuario@` | `Senha123` | Formulário não enviado |
| CE7 | E-mail sem parte local | `@teste.com` | `Senha123` | Formulário não enviado |
| CE8 | Senha vazia | `valido@teste.com` | (vazio) | Formulário não enviado |

## Test Case 1 — Registrar usuário

Fluxo completo (passos 1 a 18 do enunciado) para as entradas válidas; a conta é excluída ao final.

| ID  | Partição / limite | Nome | Senha | Resultado esperado |
|-----|-------------------|------|-------|--------------------|
| VL1 | Nome com 1 caractere | `A` | `Senha@12345` | Conta criada e excluída |
| VL2 | Nome com 2 caracteres | `Jo` | `Senha@12345` | Conta criada e excluída |
| CE1 | Nome típico | `Maria da Silva` | `Senha@12345` | Conta criada e excluída |
| CE2 | Acentos e hífen | `José Ávila-Souza` | `Senha@12345` | Conta criada e excluída |
| VL3 | Nome com 50 caracteres | 10×`Aluno` | `Senha@12345` | Conta criada e excluída |
| VL4 | Senha com 1 caractere | `Maria da Silva` | `1` | Conta criada e excluída |
| VL5 | Senha com 64 caracteres | `Maria da Silva` | 32×`S1` | Conta criada e excluída |

Entradas inválidas na etapa de Signup (não deve avançar para "Enter Account Information"):

| ID  | Partição | Nome | E-mail |
|-----|----------|------|--------|
| CE4 | Nome vazio | (vazio) | e-mail único válido |
| CE5 | E-mail vazio | `Maria` | (vazio) |
| CE6 | E-mail sem `@` | `Maria` | `maria.semarroba.com` |
| CE7 | E-mail sem domínio | `Maria` | `maria@` |
| CE8 | E-mail sem parte local | `Maria` | `@teste.com` |
| CE9 | E-mail já cadastrado | `Usuario Duplicado` | mesmo e-mail de uma conta existente → "Email Address already exist!" |

## Observações

- Os limites máximos (50 caracteres no nome, 64 na senha, 64/65 na parte local do e-mail) são **hipóteses de valor limite**: o site não documenta os tamanhos máximos. Se algum caso falhar por esse motivo, isso é um achado do teste e deve ser registrado.
- O site exibe anúncios que podem interceptar cliques; `BaseTest.clicar` remove os anúncios e usa clique via JavaScript como alternativa.
- Cada execução gera e-mails únicos, então os testes podem rodar várias vezes.
