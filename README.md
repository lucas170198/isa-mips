# minimips assemby (clojure)

Implementação da *primeira* fase do emulador ISA MIPS de 32 bits. Projeto proposto para a disciplina de **Arquitetura de computadores**, ministrada por **Emilio Francesquini**.

______________
## Autor
**Nome** : Lucas Silva Amorim

**Github** : [lucas170198](https://github.com/lucas170198/)
 ______________
## Sobre o código
1) A linguagem escolhida para implementação foi o **clojure**, por questões de afinidade com a linguagem e por técnicas interessantes que o paradigma funcional adiciona a essa solução.

2) Apesar do clojure ser uma linguagem não tipada, a tipagem facilita a manipulação das instruções lidas. Por isso foi utilizada uma [libary](https://github.com/plumatic/schema) que possibilita a criação de schemas para a aplicação.

3) As características do paradigma funcional facilitaram a construção de um código com uma boa legibilidade, e de fácil extensão. Para executar as instruções por exemplo, foram utilizados `mapas` contendo as informações de cada instrução, utilizando-se das funções como cidadãs de primeira classe (como um valor):

```clojure
(s/def r-table
  {"100000" {:str "add" :action add!}
   "100001" {:str "addu" :action addu! :unsigned true}
   "101010" {:str "slt" :action set-less-than!}
   "001000" {:str "jr" :action jump-register! :jump-instruction true}
   "000000" {:str "sll" :action shift-left! :shamt true}
   "000010" {:str "srl" :action shift-right! :shamt true}})
```
## Arquitetura
### core
Contém a função `main`, que inicializa a execução do programa.

### handler
Namespace que recebe os arquivos de entrada, e baseado no modo de operação escolhido, delega a execução para a `controller` responsável (utilizando [defmethod](https://clojuredocs.org/clojure.core/defmethod))

### controllers
Funcionam como orquestradores para a aplicação, executando as `instruções atuais` ou construindo as `strings` para serem printadas.
- **i_ops** : executa as instruções do formato I
- **j_ops** : executa as instruções do formato J
- **r_ops** : executa as instruções do formato R
- **fr_ops** : executa as instruções de ponto flutuante (formato FR)
- **syscall** : executa uma syscall baseado no valores contido em `$v0` ou `$f12`/`$f13` (para as syscalls de ponto flutuante)

### logic
Camada responsável por executar algumas funções puras de lógica, como por exemplo: extensão de sinal; soma binária; construir a instrução baseado no array de 32bits...

### storage
Para evitar duplicação de código, e prover uma api que gerência os estados da memória (tanto para o processador principal quanto para o coprocessador) o gerenciamento dos dados de memória é implementado por protocolos. 

- **Protocols** : O protocolo `IStorageClient` define as funções para operações na memória, como uma interface que será implementada para os diferentes tipos de memória.
- **Componentes**: Define a implementações da interface `IStorageClient` e contém o setup inicial do estado de ambas as memórias (através das funções `new-register-storage` e `new-coproc1-storage`)

### db
Camada que simula a memória, e oferece uma API para escritas e leituras. Para o gerenciamento de estados, foram utilizados [atoms](https://clojuredocs.org/clojure.core/atom)
- **coproc1**: Chamada das funções de `storage-client` para o `storage` de pontos flutuantes, e gerenciamento dos atoms de `hi`/`lo`.
- **registers**: Chamada das funções de `storage-client` para o `storage` da memória principal, e gerenciamento do atom de `pc`. 

### models
Contém os schemas utilizados na aplicação, a definição usa a macro `defschema` da libary [plumatic](https://github.com/plumatic/schema).

### adapters
Responsáveis pela conversão entre tipos, como por exemplo a função `byte-array->binary-instruction-array` que converte os arrays de bytes lidos na entrada, para o formato adequado das instruções **MIPS**

______________
## Uso

Caso possua o [leiningen](https://leiningen.org/) instalado, o seguinte comando irá iniciar a aplicação:

    $ lein run [operation-mode] resources/<some-file-input>

Para inciar a aplicação sem o uso do leiningen, é necessário executar o arquivo *.jar*:

    $ java -jar target/uberjar/isa-mips-0.1.0-SNAPSHOT-standalone.jar [operation-mode] resources/<some-file-input>

Para buildar aplicação (criar um arquivo .jar), utilize o seguinte comando:

    $ lein uberjar
______________

## Modos de operação
- **decode** : decodificação arquivo binário de entrada e printa na tela o código assemby equivalente

- **run** : inicia o emuladador baseado no arquivo binário de entrada

______________

## Examplo

### Entrada

    $ lein run run resources/08.sort

### Saída

```
Vetor antes: 2 1 5 9 7 8 4 0 3 6 
Vetor depois: 0 1 2 3 4 5 6 7 8 9 
```
______________


## Pontos de melhoria
#### - **Loop interativo executando as instruções**
A função `controllers.runner/run-program!` possuí um loop infinito para executar as instruções contidas na sessão de texto

```clojure
(s/defn run-program!
  [storage coproc-storage]
  (while true                                               ;Stops in the exit syscall
    (run-instruction! storage coproc-storage)
    (when-let [jump-addr @db.registers/jump-addr]              ;Verifies if the last instruction was a jump one
      (run-instruction! (+ 4 @db.registers/pc) storage coproc-storage)                ;run slotted delay instruction
      (db.registers/set-program-counter! jump-addr)
      (db.registers/clear-jump-addr!))
    (db.registers/inc-program-counter!)))
```
Esse método dificulta a execução de métodos de `jump` em geral, já que nesse caso, a instrução `jump` levaria o program counter para o valor desejado, que logo em seguida seria incrementado. Para evitar este problema, as instruções de `jump` devem ser executadas levando em conta essa característica, exemplo:

```clojure
(s/defn ^:private jump!
  [addr :- s/Str
   _]
  (let [next-inst     (a.number-base/binary-string-zero-extend (+ @db.registers/pc 4) 32)
        complete-addr (a.number-base/bin->numeric (str (subs next-inst 0 4) addr "00"))]
    (db.registers/set-jump-addr! (- complete-addr 4)))) ;Subtrai 4 do endereço de destino para tratar o problema sitado
```

#### - **Criação de testes**

Testes unitários e de aceitação não foram construídos para o código. Isso dificulta a implementação de novas features e refatorings com a garantia de não estar causando `bugs` no código.

### - **Maior abstração nas instruções do formato FR**
Algumas instruções de ponto flutuante possuem versões para `words` e `doubles`. A função `format-extension` define a extensão, e entrada para um `condp` que decide qual ação será executada. Exemplo:
```clojure
(s/defn ^:private mul!
  [destiny-reg :- s/Str
   reg :- s/Str
   regular-reg :- s/Str
   fmt :- s/Str
   _
   coproc-storage :- p-storage/IStorageClient]
  (condp = (format-extension fmt)
    :w (multiply-single! destiny-reg reg regular-reg coproc-storage)
    :d (multiply-double! destiny-reg reg regular-reg coproc-storage)))
```
Essa abordagem pode tornar a extensão do código mais complexa, além da repetição de código causada (a mesma checkagem é realizada para todas as instruções).