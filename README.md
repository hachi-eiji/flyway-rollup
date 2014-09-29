# flyway-rollup

implementation rollup feature at Flyway

- [Japanese](README_ja.md)

## How to use

1. checkout this repository
2. ``` exec:java -Dmain.class=com.hachiyae.flyway.FlywayMain -Dexec.args=-h ```

## What's flyway-rollup

1. Rollup SQL script files in specific directory.
2. delete specific record in schme_version tables.


Our team are using [Flyway](http://flywaydb.org/).We create two directies

1. stable
 - other member(branch) created file.
2. development
 - working directory in feature branch

```
-- db --- migrate -+- stable       -+- V1.0.1__foo.sql
                   |                +- V1.0.2__bar.sql
                   |
                   +- development  -+- V2.0.0__foo.sql
                                    +- V2.0.1__bar.sql
```

We have two issues.

1. We must keep a lot of SQL files.(V1.0.1__xxx.sql 〜 V1.0.200__yyy.sql !!!)
2. I can't migrate in development directory, after I merge from other branch.  
e.g.)
 1. migrate V2.0.1__bar.sql
 2. merge from other branch, get V1.0.10__create.sql
 3. migrate, but don't run V1.0.10__create.sql in my enviroment.

This tool resolve above issues.

### options

option | description
-------|-----------
-h | database host
-P | database port
-d | database name
-u | database user
-p | database password
-s | stable script directories
-D | development script directories
-c | migrate command(flyway default command and rollup, dev_clean)
--encode | flyway encode
-X | debug
-o | rollup file description
-h | help

## License

See [License](LICENSE)

