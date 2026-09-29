# Fava — Compilador e Máquina Virtual

Compilador completo para **Fava**, uma linguagem imperativa com tipagem estática, desenvolvido em Java com ANTLR 4. O projeto cobre todas as fases de um compilador: análise léxica e sintática, análise semântica (verificação de tipos), geração de código para uma máquina de pilha e uma máquina virtual que interpreta o bytecode gerado.

Projeto desenvolvido em **dupla** no âmbito da unidade curricular de **Compiladores**.

> **Nota sobre o histórico de versões**
>
> Este repositório não tem histórico de desenvolvimento. O projeto foi feito durante a universidade, e as entregas intermédias e a entrega final foram submetidas e avaliadas na plataforma **Mooshak**, sem usar Git.
>
> O código aqui publicado é a **versão final** do projeto, tal como foi entregue.

## Arquitetura

```
código-fonte .fava
      │
      ▼
┌──────────────┐   ┌──────────────┐   ┌──────────────┐   ┌──────────────┐   ┌──────────────┐
│    Lexer     │ → │    Parser    │ → │  TypeCheck   │ → │   CodeGen    │ → │      VM      │
│   (ANTLR)    │   │   (ANTLR)    │   │  (semântica) │   │  (bytecode)  │   │ (execução)   │
└──────────────┘   └──────────────┘   └──────────────┘   └──────────────┘   └──────────────┘
                                                                │
                                                                ▼
                                                          bytecodes.bc
```

| Fase | Ficheiro | Descrição |
|------|----------|-----------|
| Gramática | [src/Fava.g4](src/Fava.g4) | Regras léxicas e sintáticas da linguagem |
| Lexer / Parser | [src/Fava/](src/Fava/) | Código gerado pelo ANTLR 4.13.2 |
| Erros sintáticos | [src/MyErrorListener.java](src/MyErrorListener.java) | Contagem e reporte de erros léxicos e sintáticos |
| Análise semântica | [src/CodeGenerator/TypeCheck.java](src/CodeGenerator/TypeCheck.java) | Tabelas de símbolos, âmbitos, verificação de tipos e de retornos |
| Geração de código | [src/CodeGenerator/CodeGen.java](src/CodeGenerator/CodeGen.java) | Emissão de instruções, *constant pool* e serialização do bytecode |
| Máquina virtual | [src/VM/vm.java](src/VM/vm.java) | Descodificação e execução do bytecode numa máquina de pilha |
| Conjunto de instruções | [src/VM/OpCode.java](src/VM/OpCode.java) | Cerca de 60 *opcodes* |
| Ponto de entrada | [src/FavaCompileAndRun.java](src/FavaCompileAndRun.java) | Encadeia todas as fases |

## A linguagem Fava

- **Tipos:** `integer`, `real`, `string`, `bool`
- **Variáveis globais e locais**, com âmbitos aninhados em blocos
- **Funções** com parâmetros, tipo de retorno opcional (`-> tipo`) e **recursividade**
- **Controlo de fluxo:** `if`, `if/else`, `while`, `return`
- **Operadores:** aritméticos (`+ - * / mod`), relacionais (`< > <= >= = <>`), lógicos (`and or not`) e concatenação de strings (`||`)
- **Promoção implícita** de `integer` para `real`
- Palavras-chave e identificadores **case-insensitive**
- Comentários `// ...` e `/* ... */`
- É obrigatório existir uma função `main()`

### Exemplo

```
integer total;

function fatorial(integer n) -> integer {
    if (n <= 1)
        return 1;
    else
        return n * fatorial(n - 1);
}

function main() {
    integer i;
    i := 1;
    total := 0;
    while (i <= 5) {
        print "fatorial(" || i || ") = " || fatorial(i);
        total := total + fatorial(i);
        i := i + 1;
    }
    print "soma = " || total;
}
```

## Análise semântica

O verificador de tipos deteta, entre outros:

- variáveis ou funções não declaradas ou declaradas em duplicado
- atribuições e operações entre tipos incompatíveis
- condições de `if`/`while` que não são `bool`
- chamadas com número ou tipo de argumentos errado
- funções com tipo de retorno que não devolvem valor em todos os caminhos
- `return` com valor em funções sem tipo de retorno (e vice-versa)
- valor de retorno de uma função ignorado
- ausência de `main()`

Os erros são reportados ordenados por linha, por exemplo:

```
error in line 7: operator := is invalid between integer and string
```

## Máquina virtual

A VM é uma **máquina de pilha** que:

- lê um *constant pool* (inteiros, reais e strings) seguido da sequência de instruções
- gere variáveis globais (`galloc`, `gload`, `gstore`) e locais através de *frames* com *frame pointer* (`lalloc`, `lload`, `lstore`)
- suporta chamadas de funções e retorno de valores (`call`, `ret`, `retval`)
- tem instruções específicas por tipo (`iadd`, `dadd`, `sconcat`, `itod`, `itos`, ...)
- reporta erros de execução (ex.: divisão por zero, acesso a valor nulo)

O bytecode é também guardado em `bytecodes.bc`.

## Requisitos

- Java **17** ou superior
- [ANTLR 4.13.2](https://www.antlr.org/download.html) (`antlr-4.13.2-complete.jar`)

## Compilar e executar

```bash
git clone https://github.com/FelipeVassiliadis/Fava.git
cd Fava

# (opcional) regenerar o lexer/parser a partir da gramática
java -jar antlr-4.13.2-complete.jar -visitor -package Fava -o src/Fava -Xexact-output-dir src/Fava.g4

# compilar
javac -cp antlr-4.13.2-complete.jar -d out $(find src -name "*.java")

# executar um programa
java -cp "out:antlr-4.13.2-complete.jar" FavaCompileAndRun exemplo.fava
```

> No Windows, use `;` em vez de `:` no classpath.

Sem argumentos, o programa lê o código-fonte da entrada padrão.

O output inclui o *constant pool*, as instruções geradas e, após `*** VM output ***`, o resultado da execução.

## Tecnologias

Java · ANTLR 4 · Padrão Visitor · Máquinas de pilha · IntelliJ IDEA
