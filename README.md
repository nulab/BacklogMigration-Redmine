# Backlog Migration for Redmine

Migrate your projects from Redmine to [Backlog].
(英語の下に日本文が記載されています)

**Backlog Migration for Redmine is in beta.  To avoid problems, create a new project and import data before importing data to the existing project in Backlog.**

![Backlog icon](https://raw.githubusercontent.com/nulab/BacklogMigration-Redmine/master/icon.png)

* Backlog 
    * [http://backlog.jp](http://backlog.jp)
    * [http://backlogtool.com](http:///backlogtool.com)
    
## Requirements
* Java 8

## Update
* 2015/11/25 0.9.0b15 released
* 2015/10/28 0.9.0b14 released
* 2015/10/27 0.9.0b13 released
* 2015/10/10 0.9.0b12 released
https://github.com/nulab/BacklogMigration-Redmine/releases

Download
------------

Download the [jar], and run from the command line as follows.

    java -jar backlog-migration-redmine-<latest version>.jar

To use via proxy server, run from the command line as follows.

    java -Dhttp.proxyHost=<proxy host name> -Dhttp.proxyPort=<proxy port> -jar backlog-migration-redmine-<latest version>.jar

## How to use
### Preparation

Create a directory.

    $ mkdir work
    $ cd work

### Initialization command

Run the "init" command to prepare the mapping file.
(The mapping file is used to link data between Backlog and Redmine.)

    java -jar backlog-migration-redmine-<latest version>.jar init
    --backlog.key <Backlog of API key> \
    --backlog.url <URL of Backlog> \
    --redmine.key <Redmine API key> \
    --redmine.url <Redmine of URL>
    --projects <project identifier 1> <project identifier 2> <Redmine project identifier>: <Backlog project key>
    
Sample commands:

    java -jar backlog-migration-redmine-<latest version>.jar init \
    --backlog.key XXXXXXXXXXXXX \
    --backlog.url https://nulab.backlog.jp \
    --redmine.key XXXXXXXXXXXXX \
    --redmine.url https: //my.redmine \
    --projects estimation_system order_system: ORDER_SYSTEM
        
The mapping file is created as follows.

    .
    ├── mapping
    │ ├── priorities.json
    │ ├── statuses.json
    │ └── users.json
    └── backlog-migration-redmine-debug.log
1.mapping / users.json (users)

2.mapping / priorities.json (priority)

3.mapping / statuses.json (state)

#### About mapping projects

Specify the destination project for --projects option by colon (:).  i.e. "--projects atest:BTEST" migrates atest Redmine project in BTEST Backlog project.

    --projects <Redmine project identifier>: <Backlog project key>

### Fix the mapping file

A file in json format will be automatically created.
The items that could not be automatically associated with Backlog will be blank.
The blanks need to be filled using the items in the description.

     {
      "Description": "The values accepted for User in Backlog are "admin,tanaka". "
      "Mappings": [{
        "Backlog": "admin",
        "Redmine": "admin"
      }, {
        "Backlog": "",
        "Redmine": "satou"
      }]
    }

### Run command

Run the "execute" command to import data.

    java -jar backlog-migration-redmine-<latest version>.jar execute \
    --backlog.key <Backlog of API key> \
    --backlog.url <URL of Backlog> \
    --redmine.key <Redmine API key> \
    --redmine.url <Redmine of URL>
    --projects <project identifier 1> <project identifier 2> <Redmine project identifier>: <Backlog project key>
    
Sample commands:

    java -jar backlog-migration-redmine-<latest version>.jar execute \
    --backlog.key XXXXXXXXXXXXX \
    --backlog.url https://nulab.backlog.jp \
    --redmine.key XXXXXXXXXXXXX \
    --redmine.url https: //my.redmine \
    --projects estimation_system order_system: ORDER_SYSTEM
    
When import has been completed, the R2b directory will be created and data file that has been used for importing will be produced.

    .
    ├── mapping
    │ ├── priorities.json
    │ ├── statuses.json
    │ └── users.json
    ├── backlog-migration-redmine
    └── backlog-migration-redmine-debug.log
        
## Limitation

### Supported Redmine versions
Redmine ** 1.1.1 or later ** are supported.

### Resources that cannot be migrated
* Shared files
* Sentences
* Forum
* Roles and Privileges

### About issues
* The link to the Redmine's issue will be added in the issue's description.  i.e. Ref: From Redmine # 1
* All custom attributes in Redmine will be added to the migrating project in Backlog.
* Private notes will be also migrated. A message to tell it's a private not will be added in the description.
* The grandchild issue's parent issue will be shown as "Parent issue: <Issue key>" in the description.
* Textile formatting rule is not supported.
* In case the user who registered the issue or the assignee of the issue has been deleted from the project, the person who is migrating data will be considered as the user who has registered and updated the issue in Backlog.  The assignee will be unset.

### About Wiki
* In case the user who registered wiki has been removed from the project, the person who is migrating data will be considered as the user who has registered the wiki in Backlog.
* Wiki history will not be migrated.

### About News
* News will be registered as a Wiki.
* Attachment files will not be migrated.

### About Project
* Text formatting rules: backlog
* Some changes will be applied to the Redmine's project identifier to meet the project key format in Backlog.

Hyphen → underscore

Single-byte lowercase character → Single-byte uppercase character

### About custom fields
* Versions and users will be registered as lists and will be the fixed values.
* Boolean values will be registered in radio button format of "Yes" or "No".

Following items cannot be migrated

* Description
* Minimum - maximum length
* Regular expression
* Text formatting
* Default value
* Link URL to set to value
* For all projects
* Use as a filter
* Searched
* Display

### About limitations in Backlog
* Importing users will be terminated if the number of users will exceed the limit in Backlog.

### Resources that can be migrated by Redmine version

| Resource | Availability |
|:-----------|------------|
| Issue | 1.1 |
| Project | 1.1 |
| Project member | 1.4 |
| News | 1.1 |
| Version | 1.3 |
| Wiki | 2.2 |
| Attachment | 1.3 |
| Issue status | 1.3 |
| Tracker | 1.3 |
| Category | 1.3 |
| Group | 2.1 |
| Custom field | 2.4 |

## Re-importing

When the project key in Backlog and Redmine matches, they will be considered as the same project and data will be imported as follows.

※ If the person migrating data is not in the project

"The project will not be imported and the following message will be shown.  Join the project to migrate data.
""Importing to this project failed.  You are not a member of this project. Join the project to add issues."""


| Item | Specifications |
|:-----------|------------|
| User | The account will not be added when there is an account with same ID. |
| Group | The group will not be added when there is a group with same name. |
| Project | The project will not be added when there is a project with same project key.  The issues and wikis will be added to the existing project. |
| Issues | The issue will not be added when there is the link to the Redmine's issue in the description.  i.e. ""Ref: From Redmine #1"" is included in the  description. |
| Wiki | The wiki will be not added when there is a wiki with same name. |
| Custom fields | The custom field will not be added when there is a custom field with same name. |

## License

MIT License

* http://www.opensource.org/licenses/mit-license.php

# Backlog Migration for Redmine
Redmineのプロジェクトを[Backlog]に移行するためのツールです。

**Backlog Migration for Redmineはベータバージョンです。Backlog上の既存プロジェクトにインポートする場合は、先に新しく別プロジェクトを作成し、こちらにインポートし内容を確認後。正式なプロジェクトにインポートしてください**

* Backlog 
    * [http://backlog.jp](http://backlog.jp)
    * [http://backlogtool.com](http:///backlogtool.com)
    
## 必須要件
* Java 8

## 更新履歴
* 2015/11/25 0.9.0b15 リリース
* 2015/10/28 0.9.0b14 リリース
* 2015/10/27 0.9.0b13 リリース
* 2015/10/10 0.9.0b12 リリース
https://github.com/nulab/BacklogMigration-Redmine/releases

ダウンロード
------------

[jar]をダウンロードし、以下のようにコマンドラインから実行します。

    java -jar backlog-migration-redmine-<latest version>.jar
    
プロキシ経由で使用する場合は、以下のように実行します。

    java -Dhttp.proxyHost=<プロキシサーバのホスト名> -Dhttp.proxyPort=<プロキシサーバのポート番号> -jar backlog-migration-redmine-<latest version>.jar

## 使い方
### 前準備

作業用のディレクトリを作成します。

    $ mkdir work
    $ cd work

### 初期化コマンド

「init」コマンドを実行し、マッピングファイルを準備する必要があります。
(マッピングファイルはBacklogとRedmineのデータを対応付けるために使用します。)
    
    java -jar backlog-migration-redmine-<latest version>.jar init
    --backlog.key <BacklogのAPIキー> \
    --backlog.url <BacklogのURL> \
    --redmine.key <RedmineのAPIキー> \
    --redmine.url <RedmineのURL>
    --projects <プロジェクト識別子1> <プロジェクト識別子2> <Redmineプロジェクト識別子>:<Backlogプロジェクトキー>
    
サンプルコマンド：

    java -jar backlog-migration-redmine-<latest version>.jar init \
    --backlog.key XXXXXXXXXXXXX \
    --backlog.url https://nulab.backlog.jp \
    --redmine.key XXXXXXXXXXXXX \
    --redmine.url https://my.redmine \
    --projects estimation_system order_system:ORDER_SYSTEM
        
以下のようにマッピングファイルが作成されます。

    .
    ├── mapping
    │   ├── priorities.json
    │   ├── statuses.json
    │   └── users.json
    └── backlog-migration-redmine-debug.log
    
1.mapping/users.json(ユーザー)

2.mapping/priorities.json(優先度)

3.mapping/statuses.json(状態)

#### プロジェクトのマッピングについて

--projectsオプションに以下のようにコロン「:」区切りでプロジェクトを指定することで、Backlog側の移行先のプロジェクトを指定することができます。

    --projects <Redmineのプロジェクト識別子>:<Backlogのプロジェクトキー>

### マッピングファイルを修正

自動作成されるファイルは以下のようにjson形式で出力されます。
Backlog側の空白の項目は自動設定できなかった項目になります。
descriptionにある項目を使い、空白を埋める必要が有ります。

    {
      "description": "Backlogに設定可能なユーザーは「admin,tanaka」です。",
      "mappings": [{
        "backlog": "admin",
        "redmine": "admin"
      }, {
        "backlog": "",
        "redmine": "satou"
      }]
    }

### 実行コマンド

「execute」コマンドを実行することでインポートを実行します。

    java -jar backlog-migration-redmine-<latest version>.jar execute \
    --backlog.key <BacklogのAPIキー> \
    --backlog.url <BacklogのURL> \
    --redmine.key <RedmineのAPIキー> \
    --redmine.url <RedmineのURL>
    --projects <プロジェクト識別子1> <プロジェクト識別子2> <Redmineプロジェクト識別子>:<Backlogプロジェクトキー>
    
サンプルコマンド：

    java -jar backlog-migration-redmine-<latest version>.jar execute \
    --backlog.key XXXXXXXXXXXXX \
    --backlog.url https://nulab.backlog.jp \
    --redmine.key XXXXXXXXXXXXX \
    --redmine.url https://my.redmine \
    --projects estimation_system order_system:ORDER_SYSTEM
    
インポートが完了すると以下のようにr2bディレクトリが作成され
インポートに使用されたデータファイルが出力されます。

    .
    ├── mapping
    │   ├── priorities.json
    │   ├── statuses.json
    │   └── users.json
    ├── backlog-migration-redmine
    └── backlog-migration-redmine-debug.log
        
## 制限事項

### Redmineの対応バージョン
Redmineの対応バージョンは**1.1.1以降**になります。

### 移行できないリソース
* 共有ファイル
* 文章
* フォーラム
* ロールと権限

### 課題について
* 課題の詳細にはRedmineの課題のリンクが追加されます。例：Ref: From Redmine #1
* Redmine側に登録してあるカスタム属性の全てが、移行するプロジェクトに登録されます。
* プライベート注記も移行されます。詳細にプライベート注記である旨の追記がされます。
* 孫課題の親課題は、「親課題:課題キー」という形で詳細に記述されます。
* textile記法を変換しません。
* 課題登録者または、課題担当者であるユーザーがプロジェクトから削除されている場合、Backlog側では課題担当者は未設定、課題登録者・更新者はインポート実行者となります。

### Wikiについて
* Wiki登録者であるユーザーがプロジェクトから削除されている場合、Backlog側ではWiki登録者はインポート実行者となります。
* Wikiの履歴は登録されません。

### ニュースについて
* Wikiとして登録されます。
* 添付ファイルは移行できません。

### プロジェクトについて
* テキスト整形のルール：backlog
* Redmineのプロジェクト識別子は以下のように変換されBacklogのプロジェクトキーとして登録されます。

ハイフン→アンダースコア

半角英子文字→半角英大文字

### カスタムフィールドについて
* バージョンとユーザーはリストとして登録され固定値になります。
* 真偽値は「はい」、「いいえ」のラジオボタン形式で登録されます。

以下の項目は移行できません

* 説明
* 最短 - 最大長
* 正規表現
* テキストの書式
* デフォルト値
* 値に設定するリンクURL
* 全プロジェクト向け
* フィルタとして使用
* 検索対象
* 表示

### Backlog側の制限について
* Backlogで登録可能なユーザー数を超えた場合、インポートは中断されます。

### Redmineバージョンによる移行可能なリソース

|Resource|Availability|
|:-----------|:------------:|
|課題| 1.1 |
|プロジェクト|1.1|
|プロジェクトメンバー|1.4|
|ニュース|1.1|
|バージョン|1.3|
|Wiki|2.2|
|添付ファイル|1.3|
|課題ステータス|1.3|
|トラッカー|1.3|
|カテゴリー|1.3|
|グループ|2.1|
|カスタムフィールド|2.4|

## 再インポートの仕様

Backlog側にRedmineに対応するプロジェクトキーがある場合同一プロジェクトとみなし、以下の仕様でインポートされます。

※ 対象のプロジェクトに 参加していない場合

対象プロジェクトはインポートされず以下のメッセージが表示されます。対象プロジェクトをインポートする場合は、対象プロジェクトに参加してください。「⭕️⭕️を移行しようとしましたが⭕️⭕️に参加していません。移行したい場合は⭕️⭕️に参加してください。」

|項目|仕様|
|:-----------|------------|
|ユーザー|同じログインIDのユーザーがいる場合、同一とみなし登録しません。|
|グループ|同じグループ名のグループがある場合、同一とみなし登録しません。|
|プロジェクト|同じプロジェクトキーのプロジェクトがある場合、プロジェクトを作成せず対象のプロジェクトに課題やWikiを登録します。|
|課題|説明にRedmineの課題リンクがある場合登録しません。例：説明に次の文字列がある場合「Ref: From Redmine #1」課題番号1の課題は登録されません。|
|Wiki|同じページ名のWikiがある場合登録しません。|
|カスタム属性|同じ名前のカスタム属性がある場合登録しません。|  

## License

MIT License

* http://www.opensource.org/licenses/mit-license.php

[Backlog]: http://www.backlog.jp/

[Jar]: https://github.com/nulab/BacklogMigration-Redmine/releases/download/v0.9.0b15/backlog-migration-redmine-0.9.0b15.jar