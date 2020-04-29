{::options parse_block_html="true" /}

# Grammar

## Structure of the directory

* `algol60.g`   -> Grammar ANTLR3

## Edition

```sh
# Using antlrworks
java -jar ${git}/jar/antlrworks-1.5.2-complete.jar
```

## Limits

### BNF specification exceptions

* ❌ Imbricated strings are not recognized

### Limits of the lexer and parser

The following verification are handled by the semantics controls

* ❗ Numbers are all understood as reals in the grammar
* ❗ No expressions are typed
* ❗ Successive expression applied to expressions
* ❗ Parameters delimiters can contain numbers
