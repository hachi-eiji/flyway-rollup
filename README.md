# flyway-rollup

implementation rollup feature at Flyway

## How to use

1. checkout this repository
2. ```mvn exec:java``` or  ``` exec:java -Dmain.class=com.hachiyae.flyway.FlywayMain -Dexec.args=-h ```

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

## What's flyway-rollup
Flywayは使いやすいマイグレーションツールだが、

- 開発が進むとファイルが増える。
- flywayのlocationsを安定版(本番で稼働中)と開発用(自分が開発中)でディレクトリを分けた場合に、  
他の人が安定版ディレクトリにpushした後に,flyway:migrateを叩いてもversion番号が進んでいるため安定版の内容が反映されない

という、問題が起きる

```directory-structure
-- db --- migrate -+- stable       -+- V1.0.1__foo.sql
                   |                +- V1.0.2__bar.sql
                   |
                   +- development  -+- V2.0.0__foo.sql
                                    +- V2.0.1__bar.sql
```

そこで,以下の機能を提供する

- 最新の安定バージョンの番号のファイルにこれまで流したスクリプトファイルをマージ
- schema_versionテーブルの中からdevelopment ディレクトリの中にあるものだけ削除する.
