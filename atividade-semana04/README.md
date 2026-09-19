# Atividade Semana 04 — Teste Funcional com Playwright

## Identificação

- João Miguel Silva Salvalagio
- Eduardo Barella

## Origem

Projeto baseado no exemplo do professor João Choma Neto
(`aulas/SEMANA04/exemplo-playwright` em
https://github.com/JoaoChoma/teste_software_2026), reproduzido aqui com a
aplicação didática original (login, idade, frete, senha) e os specs-gabarito
de login e idade, acrescido dos testes pedidos na atividade proposta.

## O que foi entregue

Conforme a prática proposta no README do exemplo original:

1. Testes funcionais para a interface de **frete** (`tests/frete.spec.ts`);
2. Testes funcionais para a interface de **senha** (`tests/senha.spec.ts`);
3. Cobertura de caminhos válidos, classes inválidas e valores-limite,
   descritos nas próprias páginas.

## Casos de teste — Frete

Regra: CEP deve ter 8 dígitos numéricos; valor do pedido deve ser numérico e
positivo. Frete grátis para valor ≥ R$ 200,00; senão R$ 15,00 se o CEP
começar com "8", ou R$ 25,00 nos demais casos.

| CEP         | Valor      | Classe                                             | Resultado esperado     |
|-------------|------------|-----------------------------------------------------|-------------------------|
| 80000000    | 150,00     | CEP inicia com 8, abaixo de R$ 200                  | Frete: R$ 15,00         |
| 04000000    | 150,00     | CEP não inicia com 8, abaixo de R$ 200              | Frete: R$ 25,00         |
| 04000000    | 200,00     | valor no limite mínimo do frete grátis              | Frete grátis            |
| 04000000    | 199,99     | valor logo abaixo do limite de frete grátis         | Frete: R$ 25,00         |
| 8000000     | 150,00     | CEP com 7 dígitos (abaixo do limite)                | Dados inválidos         |
| 800000000   | 150,00     | CEP com 9 dígitos (acima do limite)                 | Dados inválidos         |
| 0400000A    | 150,00     | CEP com caractere não numérico                      | Dados inválidos         |
| 04000000    | 0          | valor não positivo                                  | Dados inválidos         |
| 04000000    | abc        | valor em formato inválido                           | Dados inválidos         |
| (vazio)     | (vazio)    | campos vazios                                       | Dados inválidos         |

## Casos de teste — Senha

Regra: 8 a 20 caracteres, com ao menos 1 maiúscula, 1 minúscula, 1 dígito,
sem espaços, e confirmação idêntica à senha.

| Senha                    | Classe                                          | Resultado esperado          |
|--------------------------|--------------------------------------------------|-------------------------------|
| Abcde123                 | limite mínimo de tamanho (8)                     | Senha cadastrada             |
| Abcdefghij1234567890     | limite máximo de tamanho (20)                    | Senha cadastrada             |
| Abc12345                 | tamanho intermediário válido                     | Senha cadastrada             |
| Abcde12                  | tamanho abaixo do mínimo (7)                      | Senha fora do padrão         |
| Abcdefghij12345678901    | tamanho acima do máximo (21)                     | Senha fora do padrão         |
| abcdefg1                 | sem letra maiúscula                              | Senha fora do padrão         |
| ABCDEFG1                 | sem letra minúscula                              | Senha fora do padrão         |
| Abcdefgh                 | sem dígito numérico                              | Senha fora do padrão         |
| Abcde 12                 | contém espaço                                    | Senha fora do padrão         |
| Abcde123 / Abcde124      | formato válido, confirmação divergente           | As senhas não coincidem      |

## Como executar

```bash
npm install
npm run browsers
npm test
```

Acompanhar visualmente:

```bash
npm run test:headed
```

Relatório HTML:

```bash
npm run report
```

## Estrutura

- `server.js` / `public/` — aplicação didática (login, idade, frete, senha),
  reproduzida do exemplo do professor.
- `tests/login.spec.ts`, `tests/idade.spec.ts` — specs-gabarito do professor.
- `tests/frete.spec.ts`, `tests/senha.spec.ts` — testes desenvolvidos para
  esta atividade.
