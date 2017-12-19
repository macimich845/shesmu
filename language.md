# Shesmu Decision-Action Language

Shesmu takes a stream of information from Provenance and performs certain
tasks, called _actions_. The language defines what actions are to be run, and
the server takes care of scheduling and running tasks.

This document describes the language where the decisions and actions are
specified. The list of possible actions are provided externally.

## Olives and Clauses
Each decision-action stanza is called an _olive_. There are two: _define_ and
_run_. Each is processing a _stream_ of input from provenance. Information from
provenance is available as variables.

A _run_ olive specifies an action to run if the conditions are met. For example:

    Run fastqc
      Where workflow == "BamQC 2.7+"
      With {
        memory = 4Gi,
        input = fileswa
      }

This will take all the input provenance and filter out any run by the workflow
`BamQC 2.7+` and then launch `fastqc`. The `With` portion sets all the
parameters. These are specific to the action.

The `Where` line is an _olive clause_. There are three clauses: where, group, and matches.

A `Group` clause groups items in the stream to be de-duplicated based on
_discriminators_ and other variables are grouped into _collectors_.

    Run fingerprint
      Where workflow == "BamQC 2.7+"
      Group files = file_swa By project
      With {
        memory = 4Gi,
        input = files
      }

The grouping changes the stream. After the grouping, `files` will be a list of
all the `file_swa` values for each `project`. Any other variables, (_e.g._,
`workflow`) won't be accessible since they weren't included in the grouping
operation.

To make reusable logic, the _Define_ olive can be used:

    Define standard_fastq():
      Where file_meta_type == "x-chemical/fastq-gzip"
      Where workflow == "CASAVA 1.8"

    Run fastqc
      Matches standard_fastq()
      With {
        memory = 4Gi,
        input = file_swa
      }

The `Define` olive creates a reusable set of clauses and `Matches` includes it in another olive. Parameters can also be specified:

    Define standard_fastq(date limit):
      Where after_date > limit
      Where file_meta_type == "x-chemical/fastq-gzip"
      Where workflow == "CASAVA 1.8"

    Run fastqc
      Matches standard_fastq(Date 2017-01-01)
      With {
        memory = 4Gi,
        input = file_swa
      }

Because grouping changes variables, `Matches` must appear before `Group` clauses.

## Types
There are a small number of types in the language, listed below. Each has
syntax as it appears in the language and a signature that is used for
machine-to-machine communication.

| Name      | Underlying Type | Syntax                   | Signature  |
|---        |---              |---                       |---         |
| Integer   | long            | `integer`                | `i`	      |
| String    | String          | `string`                 | `s`	      |
| Boolean   | boolean         | `boolean`                | `b`	      |
| Date      | Instant         | `date`                   | `d`        |
| List      | Set             | `[`_inner_`]`            | `a`_inner_ |
| Tuple     | Tuple (custom)  | `{`_t1_`,`_t2_`,` ...`}` | `t` _n_` _t1_ _t2_ Where _n_ is the number of elements in the tuple. |


## Expressions
Shesmu has the following expressions, for lowest precedence to highest precendence.

### Switch Selections
- `Switch` _refexpr_ (`When` _testexpr_ `Then` _valueexpr_)* `Else` _altexpr_

Compares _refexpr_ to every _testexpr_ for equality and returns the matching
_valueexpr_. If none match, returns _altexpr_. The _altexpr_ and every
_valueexpr_ must have the same type. The _refexpr_ and every _testexpr_ must
have the same type, but not necessarily the same type as the _altexpr_ and
_valueexpr_.

### Logical Disjunction
- _expr_ `||` _expr_

Logical short-circuring `or`. Both operands must be boolean and the result is boolean.

### Logical Conjunction
- _expr_ `&&` _expr_

Logical short-circuring `and`. Both operands must be boolean and the result is boolean.

### Comparison
#### Equality
- _expr_ == _expr_

Compare two types for equality. This is supported for all types. For tuples,
the values in the tuples must be the same. For lists, the items must be the
same, but the order is not considered.

#### Inequality
- _expr_ `!=` _expr_

Compare two values for inequality. This is the logical complement to `==`.

#### Ordering
- _expr_ `<` _expr_
- _expr_ `<=` _expr_
- _expr_ `>=` _expr_
- _expr_ `>` _expr_

Compare two values for order. This is only defined for integers and dates. For
dates, the lesser value occurs temporally earlier.

### Arithmetic Disjunction
#### Addition
- _expr_ `+` _expr_

Adds two values. If both are integers, then they are summed. If the left is a
date and the right is an integer, a new date is produced that is later by the
number of seconds in the integer value.

#### Subtraction
- _expr_ `-` _expr_

Subtracts two values. If both are integer, then the right is subtracted from
the left. If both are dates, the difference in seconds from the first left date
to the right date is computed. If the left is a date and the right is an
integer, a new date is produced that is earlier by the number of seconds on the
right.

### Arithmetic Conjunction
#### Multiplication
- _expr_ `*` _expr_

Multiplies two values which must be integers.

#### Division
- _expr_ `/` _expr_

Divides two values which must be integers.

#### Modulus
- _expr_ `%` _expr_

Computes the remainder of diving the first value by the second, both of which
must be integers.

### Unary Operators
#### Boolean Not
- `!` _expr_

Compute the logical complement of the expression, which must be a boolean.

#### Integer Negation
- `-` _expr_

Computes the arithmetic additive inverse of the expression, which must be an integer.

#### List Cardinality
- `Count` _expr_

Computes the number of unique items in the expression, which must be a list.


### Suffx Operators
#### List Membership
- _needle_ `In` _haystack_

Determines if the expression _needle_ is present in the list _haystack_ and
returns the result as a boolean. _needle_ may be any type, but _haystack_ must
be a list of the same type.

#### Tuple Access
- _expr_ `@` _n_

Extracts an element from a tuple. _n_ is an integer that specifies the
zero-based index of the item in the tuple. The result type will be based on the
type of that position in the tuple. If _n_ is beyond the number of items in the
tuple, an error occurs.


### Terminals
#### Date Literal
- `Date` _YYYY_`-`_mm_`-`_dd_
- `Date` _YYYY_`-`_mm_`-`_dd_`T`_HH_`:`_MM_`:`_SS_`Z`
- `Date` _YYYY_`-`_mm_`-`_dd_`T`_HH_`:`_MM_`:`_SS_`+`_zz_
- `Date` _YYYY_`-`_mm_`-`_dd_`T`_HH_`:`_MM_`:`_SS_`-`_zz_

Specifies a date and time. If the time is not specified, it is assumed to be midnight UTC.

#### Tuple Literal
- `{`_expr_`, `_expr_`, `...`}`

Creates a new tuple with the elements as specified. The type of the tuple is
determined based on the elements.

#### List Literal
- `[`_expr_`,` _expr_`,` ...`]`

Creates a new list from the specified elements. All the expressions must be of the same type.

#### String Literla
- `"`parts`"`

Specified a new string literal. A string may contain the following special items in addition to text:

- `\\t` for a tab character
- `\\n` for a new line character
- `\\{` for a open brace character
- `{`_expr_`}` for a string interpolation; the expression must be a string, integer, or date
- `{`_expr_`:`_n_`}` for a zero-padded integer string interpolation; the expression must be an integer and _n_ is the number of digits to pad to
- `{`_expr_`:`_f_`}` for a formatted date string interpolation; the expression must be a date and _f_ is the [format code](https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html)

#### Subexpression
- `(`_expr_`)`

#### Integer Literal
A subexpression.

- _n_

An integer literal. Integer may be suffixed by one of the following multipliers:

| G  | 1000^3 |
| Gi | 1024^3 |
| M  | 1000^2 |
| Mi | 1024^2 |
| k  | 1000   |
| ki | 1024   |

#### Boolean Literals
- `True`
- `False`

The boolean true and false values, respectively.

#### Lookup Access
- _lookup_`[`_expr_`,` _expr_`,` ...`]`

Performs a lookup in a table. Lookup tables are provided by external services to Shesmu.

#### Variables
- _var_

The value of a variable. There are three kinds of variables in Shesmu:

- stream variables, attached to the data being processed (or the grouped versions of it)
- parameters, as specified in `Define` olives
- lambda variables, XXXTODOXXX

Only stream variables may be used as discriminators in `Group` clauses.

## Identifiers
All identifier is Shesmu, including olive definitions, lookup names, action
names, and variables must begin with a lowercase letter a-z, followed by an
number of underscores, lowercase letters a-z, and decimal digits.

Olive definitions, lookup names, action names, and variables exist in different
name spaces. It is possible to create a parameter with the same name as an
action, though this is not recommended.

## Variables
The default variables available in the Shesmu langauge can be seen on the status page, or:

    curl http://localhost:8081/variables | js -S .

or:

    java ca.on.oicr.gsi.shesmu.compiler.Build -v