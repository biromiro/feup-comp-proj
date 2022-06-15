# Compilers Project

## GROUP: 8d

- **João Baltazar**
  - NR: 201905616 
  - GRADE: 20
  - CONTRIBUTION: 25%
- **Nuno Costa**
    - NR: 201906272
    - GRADE: 20
    - CONTRIBUTION: 25%
- **Pedro Gonçalo Correia**
    - NR: 201905348
    - GRADE: 20
    - CONTRIBUTION: 25%
- **Rita Mendes**
    - NR: 201907877
    - GRADE: 20
    - CONTRIBUTION: 25%


- **GLOBAL Grade of the project:** 20

## SUMMARY 

For this project, we have developed a compiler for the Java-- language which generates a `.class` file with java bytecode
from a `.jmm` file with Java-- code.

We implemented all required features, as well as extra features (3 simple optimizations and function overloading), which
are described in more detail in the "PROS" section. We tested the code and made our own unit tests to help verify that our
implementation is correct.

## SEMANTIC ANALYSIS

For the semantic analysis phase, we have implemented two visitors: one to generate the symbol table and another to
check the semantics of the code.

The first visitor being run constructs the symbol table, extracting all imports (ignoring duplicates but raising an error if
two different imports have the same last part of the path, as that would create ambiguity), the class and super class names,
the fields, the methods signatures and parameters, and the local variables of each method. No two fields are allowed to have
the same name, nor two methods the same signature. In a given method, parameters and local variables cannot have the same name.

The second visitor implements additional semantic checks:
- A class cannot extend itself, nor another class that is not imported
- An identifier must refer to a local variable, parameter or field (in that order of priority).
  - Fields are not allowed in the main method.
  - An identifier can exceptionally be allowed if it is `String`, an imported class, or the class name, and is used to call a static method.
- `this` cannot be used in `main`.
- A static method can only be called in a `String`, imported class or the class of this file (if it extends another and doesn't
define a (non-static) method with the same signature).
- A method cannot be called on an `int`, `boolean` or `void`.
- A non-static method call on an instance of the class of this file must have a signature compatible with one and only one defined method.
  (or none, if the class extends another).
- The `.length` call can only be used on an array.
- Indexing can only be done on an array and with an `int` index.
- A new object created with `new SomeObject();` syntax cannot be an array, `int`, `boolean` or `void`.
- A new array can only be created with the size given by an `int`.
- The not operator can only be aplied to a `boolean`.
- The operators `+`, `-`, `*`, `/` and `<` can only be applied to two `int`s. `&&` can only be applied to two `boolean`s.
- An assignment can only be done if both sides have the same type or a compatible type (as in, it is possible that the type of 
the right-hand side extends the type of the left-hand side).
- The condition of an `if` statement or a `while` loop must be a `boolean`.
- The value returned by a method must be the same type as the return type of the method or a compatible type (as in, it is possible
that the type of the returned value extends the return type).
- A used type that isn't a primitive or the class of the file must be imported.

The special "#UNKNOWN" pseudo-type is used to represent the type of a variable that is not yet known, either because
there was a semantic error, or because a method of another class was called. "#UNKNOWN" is optimistically assumed to always be a valid type
(if it resulted from an error, the program will still abort at the end of the stage and it doesn't need to report the same error twice; 
if it results from a method of another class, it is now yet known and should be assumed to be correct).

## CODE GENERATION
(describe how the code generation of your tool works and identify the possible problems your tool has regarding code generation.)


## PROS

Our tool passes the provided public unit tests, as well as custom unit tests we made that try to
catch most bugs and test most cases that could occur (both with and without optimizations).
Because of this, we are confident we implemented all the required features for the compiler
in a correct way. The fixtures for the custom tests are located in `test/fixtures/custom`, and the 
unit tests themselves in `test/pt/up/fe/comp/custom`.

When the input j-- file has multiple semantic errors, the compiler will print
the error message for each error and only abort at the end of the analysis, instead of printing the first error
and aborting immediately. An example file with multiple errors to test this feature is located in 
`test/fixtures/custom/HelloWorld.fail.jmm`.

With the debug flag (`-d`) activated, the compiler will print the AST, symbol table, ollir code and jasmin code
generated during the compilation phases.

As an extra, we implemented overloading of functions, allowing multiple methods with the same name but different arity
to be defined (or same arity but with at least one parameter requiring a different type). This feature is tested in
`test/fixtures/custom/Overloading1.jmm`, `test/fixtures/custom/Overloading2.jmm` and `test/fixtures/custom/Overloading3.fail.jmm`
(the `.fail` in the fixture name means that the test is supposed to fail, in this case because there are two functions
with the same signature).

We implemented all mandatory optimizations (register allocation, constant propagation and elimination of unnecessary gotos),
as well as three optional optimizations (constant folding, optimized JVM instructions, simple dead code elimination).
- For constant folding and constant propagation, we tested them with the fixtures `test/fixtures/custom/ConstFold[1-5].jmm`,
`test/fixtures/custom/ConstProp[1-10].jmm` and `test/fixtures/custom/ConstFoldAndProp[1-2].jmm`. The `.noDiff` in
some tests means that the test doesn't expect any difference in the output when the `-o` flag is enabled (it is testing
that the optimization isn't changing code where it isn't supposed to).
- For optimized JVM instructions, we tried to choose the most efficient instructions for each operation in the given context, 
and made sure that our compiler passes the given public tests of the CFP delivery.
- For simple dead code elimination, we tested it with the fixtures `test/fixtures/custom/DeadCode[1-11].jmm`. This optimization
removes code (in if/else/while) that is unreachable because the condition in the if statement is a `true` or `false` constant, or
the condition in the while statement is a `false` constant.
  - As an extra, we also implemented dead code elimination of unused variables, tested in `test/fixtures/custom/UnusedVariable[1-8].jmm`.
- For elimination of unnecessary gotos, we tested it with the fixtures `test/fixtures/custom/UnnecessaryGoto[1-7].jmm`. 
This optimization tries to minimize the number of branch instructions in the code, by using an efficient template for while loops that
is further optimized if it is known that at least one iteration will always run.

## CONS
(Identify the most negative aspects of your tool)

----

For this project, you need to install [Java](https://jdk.java.net/), [Gradle](https://gradle.org/install/), and [Git](https://git-scm.com/downloads/) (and optionally, a [Git GUI client](https://git-scm.com/downloads/guis), such as TortoiseGit or GitHub Desktop). Please check the [compatibility matrix](https://docs.gradle.org/current/userguide/compatibility.html) for Java and Gradle versions.

## Project setup

There are three important subfolders inside the main folder. First, inside the subfolder named ``javacc`` you will find the initial grammar definition. Then, inside the subfolder named ``src`` you will find the entry point of the application. Finally, the subfolder named ``tutorial`` contains code solutions for each step of the tutorial. JavaCC21 will generate code inside the subfolder ``generated``.

## Compile and Running

To compile and install the program, run ``gradle installDist``. This will compile your classes and create a launcher script in the folder ``./build/install/comp2022-00/bin``. For convenience, there are two script files, one for Windows (``comp2022-00.bat``) and another for Linux (``comp2022-00``), in the root folder, that call tihs launcher script.

After compilation, a series of tests will be automatically executed. The build will stop if any test fails. Whenever you want to ignore the tests and build the program anyway, you can call Gradle with the flag ``-x test``.

## Test

To test the program, run ``gradle test``. This will execute the build, and run the JUnit tests in the ``test`` folder. If you want to see output printed during the tests, use the flag ``-i`` (i.e., ``gradle test -i``).
You can also see a test report by opening ``./build/reports/tests/test/index.html``.

## Checkpoint 1
For the first checkpoint the following is required:

1. Convert the provided e-BNF grammar into JavaCC grammar format in a .jj file
2. Resolve grammar conflicts, preferably with lookaheads no greater than 2
3. Include missing information in nodes (i.e. tree annotation). E.g. include the operation type in the operation node.
4. Generate a JSON from the AST

### JavaCC to JSON
To help converting the JavaCC nodes into a JSON format, we included in this project the JmmNode interface, which can be seen in ``src-lib/pt/up/fe/comp/jmm/ast/JmmNode.java``. The idea is for you to use this interface along with the Node class that is automatically generated by JavaCC (which can be seen in ``generated``). Then, one can easily convert the JmmNode into a JSON string by invoking the method JmmNode.toJson().

Please check the JavaCC tutorial to see an example of how the interface can be implemented.

### Reports
We also included in this project the class ``src-lib/pt/up/fe/comp/jmm/report/Report.java``. This class is used to generate important reports, including error and warning messages, but also can be used to include debugging and logging information. E.g. When you want to generate an error, create a new Report with the ``Error`` type and provide the stage in which the error occurred.


### Parser Interface

We have included the interface ``src-lib/pt/up/fe/comp/jmm/parser/JmmParser.java``, which you should implement in a class that has a constructor with no parameters (please check ``src/pt/up/fe/comp/CalculatorParser.java`` for an example). This class will be used to test your parser. The interface has a single method, ``parse``, which receives a String with the code to parse, and returns a JmmParserResult instance. This instance contains the root node of your AST, as well as a List of Report instances that you collected during parsing.

To configure the name of the class that implements the JmmParser interface, use the file ``config.properties``.

### Compilation Stages 

The project is divided in four compilation stages, that you will be developing during the semester. The stages are Parser, Analysis, Optimization and Backend, and for each of these stages there is a corresponding Java interface that you will have to implement (e.g. for the Parser stage, you have to implement the interface JmmParser).


### config.properties

The testing framework, which uses the class TestUtils located in ``src-lib/pt/up/fe/comp``, has methods to test each of the four compilation stages (e.g., ``TestUtils.parse()`` for testing the Parser stage). 

In order for the test class to find your implementations for the stages, it uses the file ``config.properties`` that is in root of your repository. It has four fields, one for each stage (i.e. ``ParserClass``, ``AnalysisClass``, ``OptimizationClass``, ``BackendClass``), and initially it only has one value, ``pt.up.fe.comp.parse.Parser``, associated with the first stage.

During the development of your compiler you will update this file in order to setup the classes that implement each of the compilation stages.
