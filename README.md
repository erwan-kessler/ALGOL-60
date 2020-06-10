# PCL 2019-2020
# ALGOL 60

## Auteurs

* [Locust2520](https://github.com/Locust2520)
* `<Redacted>`
* [Erwan Kessler](mailto:erwan.kessler@telecomnancy.net)
* [Yann Meyer](https://github.com/YannMey)

## Structure du dépôt

folder     	        | content
--------------------|---------------------------------
`grammar/` 	        | Grammar of algol 60 in LL1 form
`test/` 	        | unit test
`src/` 		        | main code
`asm/`		        | standard library for assembly
`scripts/`          | Scripts to use more easily the jar
`examples/`  		| examples of programs
`report/`  			| contain a report for the project in french

## Disclaimer
The code is provided as it.  
You need a **JVM 11** to run the released Jar

## Curent progress

Currently we have implemented integer, boolean and floating point variables, arithmetic and boolean operations, control flows, function with parameter passed by value (and pointer for arrays), scoping, recursivity, labels and arrays.

Current limitations are mutual recursion, jump to labels outside of a scope and static declaration with the `own` keyword.

## Execution

Compilation and execution of a program

```shell
# Generation of the jar archive
./gradlew fatJar
# Using the jar archive on a file to compile it and run it through the virtual 16bit computer
java -jar build/libs/PCL_RELEASE-1.0.0-all.jar --quick <file>.a60
# Display help
java -jar build/libs/PCL_RELEASE-1.0.0-all.jar --help
```

```md
Usage is java -jar <filename>.jar <file> (options)*
<file> can either be a .al or .a60 file then it will be compiled to assembly into a .src and into binary into a .iup
or be .iup or .piup file that will be run only (need --run option)
    --help or -help show this help
    --run allow to only run a .iup or .piup file, will not compile anything
    --debug-all output all debug with no distinction
    --debug output only simple debug as usual
    --opti activate the register optimization
    --no-folding deactivate constant folding
    --resource use only a resource program as input
    --silent silent the output of .iup, only use when debug_all to silence piupk
    --in-place compile in place the file, do not create the outputCode folder
    --no-compile do not compile assembly to binary
    --quick run quickly, enable silent output, compile file and run it
    --launch launch afterwards the compiled binary file, incomptatible with --no-compile
    --table show table
    --graph show graph
    --debug-compiler debug only the compiler
```



## Gradle commands
There is 3 important groups in the gradle tasks

Task group          | content
--------------------|---------------------------------
`utils/` 	        | utilities to debug or launch quickly
`simple_examples/`  | unit examples
`semantic_serror/`  | unit semantics error
`real_examples//`   | real world examples

Useful utilities:

```shell script
# run quickly the src/main/resources/asm.a60
./gradlew runQuick
# display help for runtime
./gradlew ExecutionHelp
# display the symbol table for src/main/resources/tree.a60 (can be change inside the gui then)
./gradlew DisplayTable
# display the graph for register optimization for src/main/resources/asm.a60
./gradlew DisplayGraph
```

## Using the build scripts

**Warning please always consider using those scripts inside the scripts/ folder**

The scripts are in `scripts/`

```shell
cd scripts
# build the jar
./build.sh
# compile the input file and make the iup output into output_file if provided else input_file.iup
./compile.sh <input_file> (<output_file>)?
# run the compiled binary with microPiupk
./run.sh <input_file>
# clean the current folder (use with caution
./clean.sh
```



## Grammar and Abstract Symbol Tree (AST) 

The grammar made is **LL(1)** which allow the parser and the lexer to do no recursive 
flow while parsing, it also have some limitations which are explained in the file.

Located in `grammar/algol60.g`

```shell script
cd grammar/
nano algol60.g
# Editing the grammar on a visual tool:
java -jar jar/antlrworks-1.5.2-complete.jar
```

## Symbol Table
The symbol table contains all the declaration and can be viewed directly.

```shell script
# Visualiszation of the Symbol Table
./gradlew DisplayTable
```

## Register optimization
Currently there is two mode to allocate register, either by doing it safely by each time 
stacking the register after one to five operations or by using fixed register that use
Chaitin algorithm to determine live range and live outside of a scope, increasing the 
speed tremendously.

```shell script
# Visualiszation of the register graph
./gradlew DisplayGraph
# compile with the optimization
java -jar build/libs/PCL_RELEASE-1.0.0-all.jar --opti <file>.a60
```

## Semantics controls
The semantics controls are done after the parsing of the file through the grammar and ensure 
that even if the program is correct syntaxic wise, it doesn't break the specification

There is examples of semantics error in `src/main/resources`
```shell script
# Générating the jar archive
./gradlew fatJar
# Using the archive on a file
java -jar build/libs/PCL_RELEASE-1.0.0-all.jar <file>.a60
```

## Using the virtual 16bit processor
```shell script
# Usage of microPIUPK
# compile an assembly file to machine code
java -jar jar/microPIUPK.jar -ass <file>.asm
# launch the visual simulation
java -jar jar/microPIUPK.jar -sim
# run a compiled machine code file
java -jar jar/microPIUPK.jar -batch <file>.iup
```

## Unit tests

```shell script
./gradlew test
```

