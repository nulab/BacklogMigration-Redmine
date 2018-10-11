# Backlog Migration for Redmine

Migrate your projects from Redmine to [Backlog].
(英語の下に日本文が記載されています)

**Backlog Migration for Redmine is in beta.  To avoid problems, create a new project and import data before importing data to the existing project in Backlog.**

![Backlog icon](https://raw.githubusercontent.com/nulab/BacklogMigration-Redmine/master/icon.png)

* Backlog
    * [http://backlog.jp](http://backlog.jp)
    * [http://backlogtool.com](http://backlogtool.com)

## DEMO

![Demo](http://www.backlog.jp/backlog-migration/backlog-migration-redmine.gif)

## Requirements
* **Java 8**
* The Backlog Space's **administrator** roles.

https://github.com/nulab/BacklogMigration-Redmine/releases

Download
------------

Please download the jar file from this link, and run from the command line as follows.

https://github.com/nulab/BacklogMigration-Redmine/releases

    java -jar backlog-migration-redmine-[latest version].jar

To use via proxy server, run from the command line as follows.

    java -Djdk.http.auth.tunneling.disabledSchemes= -Dhttps.proxyHost=[proxy host name] -Dhttps.proxyPort=[proxy port] -Dhttps.proxyUser=[proxy user] -Dhttps.proxyPassword=[proxy password] -jar backlog-migration-redmine-[latest version].jar

## How to use
### Preparation

Create a directory.

    $ mkdir work
    $ cd work

### Initialization command

Run the [**init**] command to prepare the mapping file.
(The mapping file is used to link data between Backlog and Redmine.)

    java -jar backlog-migration-redmine-[latest version].jar \
      init \
      --redmine.key [Redmine API key] \
      --redmine.url [Redmine of URL] \
      --backlog.key [Backlog of API key] \
      --backlog.url [URL of Backlog] \
      --projectKey [Redmine project identifier]:[Backlog project key]
    
Sample commands:

    java -jar backlog-migration-redmine-[latest version].jar \
      init  \
      --redmine.key XXXXXXXXXXXXX \
      --redmine.url https://my.redmine \
      --backlog.key XXXXXXXXXXXXX \
      --backlog.url https://nulab.backlog.jp \
      --projectKey redmine_project:BACKLOG_PROJECT
        
The mapping file is created as follows.

    .
    ├── log
    │   ├── backlog-migration-redmine-warn.log
    │   └── backlog-migration-redmine.log
    └── mapping
        ├── priorities.json
        ├── statuses.json
        └── users.json

- 1.mapping / users.json (users)
- 2.mapping / priorities.json (priority)
- 3.mapping / statuses.json (state)

#### About mapping projects

Specify the destination project for **--projectKey** option by colon (:).  i.e. [**--projectKey redmine_project:BACKLOG_PROJECT**] migrates **redmine_project** redmine project in **BACKLOG_PROJECT** backlog project.

    --projectKey [Redmine project identifier]:[Backlog project key]

### Fix the mapping file

A file in json format will be automatically created.
The items that could not be automatically associated with Backlog will be blank.
The blanks need to be filled using the items in the description.

     {
      "Description": "The values accepted for User in Backlog are "admin,tanaka". "
      "Mappings": [{
        "redmine": "admin",
        "backlog": "admin"
      }, {
        "redmine": "satou",
        "backlog": ""
      }]
    }

### Run command

Run the [**execute**] command to import data.

    java -jar backlog-migration-redmine-[latest version].jar \
      execute \
      --redmine.key [Redmine API key] \
      --redmine.url [Redmine of URL]
      --backlog.key [Backlog of API key] \
      --backlog.url [URL of Backlog] \
      --projectKey [Redmine project identifier]:[Backlog project key]
    
Sample commands:

    java -jar backlog-migration-redmine-[latest version].jar \
      execute \
      --redmine.key XXXXXXXXXXXXX \
      --redmine.url https: //my.redmine \
      --backlog.key XXXXXXXXXXXXX \
      --backlog.url https://nulab.backlog.jp \
      --projectKey redmine_project:BACKLOG_PROJECT
    
When import has been completed, the directory will be created and data file that has been used for importing will be produced.

    .
    ├── log
    │   ├── backlog-migration-redmine-warn.log
    │   └── backlog-migration-redmine.log
    └── mapping
        ├── priorities.json
        ├── statuses.json
        └── users.json

#### Import-Only Mode
[**--importOnly**] If you set this property, only import will be run by the program.

    java -jar backlog-migration-redmine-[latest version].jar \
      execute \
      --redmine.key [Redmine API key] \
      --redmine.url [Redmine of URL] \
      --backlog.key [Backlog of API key] \
      --backlog.url [URL of Backlog] \
      --projects [Redmine project identifier]: [Backlog project key] \
      --importOnly
      
[**--exclude**] If you set this property, specified import will **not** be run by the program.

    java -jar backlog-migration-redmine-[latest version].jar \
      execute \
      --redmine.key [Redmine API key] \
      --redmine.url [Redmine of URL] \
      --backlog.key [Backlog of API key] \
      --backlog.url [URL of Backlog] \
      --projects [Redmine project identifier]: [Backlog project key] \
      --exclude [issue or wiki]
        
## Limitation

### Supported Redmine versions
Redmine **1.1.1 or later** are supported.

### Backlog's user roles
This program is for the users with the Space's **administrator** roles.

### Migration project with custom fields
Only applied to **Premium** or **Platina** plan.

### Resources that cannot be migrated
* Shared files
* Sentences
* Forum
* Roles and Privileges

### About issues
* All custom attributes in Redmine will be added to the migrating project in Backlog.
* Private notes will be also migrated. A message to tell it's a private not will be added in the description.
* The grandchild issue's parent issue will be shown as [**Parent issue: [Issue key]**] in the description.
* **Textile** formatting rule is not supported.
* In case the user who registered the issue or the assignee of the issue has been deleted from the project, the person who is migrating data will be considered as the user who has registered and updated the issue in Backlog.  The assignee will be unset.

### About Wiki
* In case the user who registered wiki has been removed from the project, the person who is migrating data will be considered as the user who has registered the wiki in Backlog.
* Wiki history will not be migrated.

### About News
* News will be registered as a Wiki.
* Attachment files will not be migrated.

### About Project
* Text formatting rules: **markdown**
* Some changes will be applied to the Redmine's project identifier to meet the project key format in Backlog.

**Hyphen** → **underscore**

Single-byte **lowercase** character → Single-byte **uppercase** character

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
| Priority | 2.2 |

## Re-importing

When the project key in Backlog and Redmine matches, they will be considered as the same project and data will be imported as follows.

**If the person migrating data is not in the project.**

The project will not be imported and the following message will be shown.  Join the project to migrate data.
Importing to this project failed.  You are not a member of this project. Join the project to add issues.


| Item | Specifications |
|:-----------|------------|
| Group | The group will not be added when there is a group with same name. |
| Project | The project will not be added when there is a project with same project key.  The issues and wikis will be added to the existing project. |
| Issues | Issues with matching subject, creator, creation date are not registered. |
| Wiki | The wiki will be not added when there is a wiki with same name. |
| Custom fields | The custom field will not be added when there is a custom field with same name. |

## License

MIT License

* http://www.opensource.org/licenses/mit-license.php

## Inquiry

Please contact us if you encounter any problems during the Redmine to Backlog migration.

https://www.backlog.jp/contact/

# Backlog Migration for Redmine
Redmineのプロジェクトを[Backlog]に移行するためのツールです。

**Backlog Migration for Redmineはベータバージョンです。Backlog上の既存プロジェクトにインポートする場合は、先に新しく別プロジェクトを作成し、こちらにインポートし内容を確認後。正式なプロジェクトにインポートしてください**

* Backlog
    * [http://backlog.jp](http://backlog.jp)
    * [http://backlogtool.com](http://backlogtool.com)

## 必須要件
* **Java 8**
* Backlogの **管理者権限**

https://github.com/nulab/BacklogMigration-Redmine/releases

ダウンロード
------------

こちらのリンクからjarファイルをダウンロードし、以下のようにコマンドラインから実行します。

https://github.com/nulab/BacklogMigration-Redmine/releases

    java -jar backlog-migration-redmine-[最新バージョン].jar

プロキシ経由で使用する場合は、以下のように実行します。

    java -Djdk.http.auth.tunneling.disabledSchemes= -Dhttps.proxyHost=[プロキシサーバのホスト名] -Dhttps.proxyPort=[プロキシサーバのポート番号] -Dhttps.proxyUser=[プロキシユーザー名] -Dhttps.proxyPassword=[プロキシパスワード] -jar backlog-migration-redmine-[最新バージョン].jar

## 使い方
### 前準備

作業用のディレクトリを作成します。

    $ mkdir work
    $ cd work

### 初期化コマンド

[**init**]コマンドを実行し、マッピングファイルを準備する必要があります。
(マッピングファイルはRedmineとBacklogのデータを対応付けるために使用します。)

    java -jar backlog-migration-redmine-[最新バージョン].jar \
      init \
      --redmine.key [RedmineのAPIキー] \
      --redmine.url [RedmineのURL] \
      --backlog.key [BacklogのAPIキー] \
      --backlog.url [BacklogのURL] \
      --projectKey [Redmineプロジェクト識別子]:[Backlogプロジェクトキー]

サンプルコマンド：

    java -jar backlog-migration-redmine-[latest version].jar \
      init \
      --redmine.key XXXXXXXXXXXXX \
      --redmine.url https://my.redmine \
      --backlog.key XXXXXXXXXXXXX \
      --backlog.url https://nulab.backlog.jp \
      --projectKey redmine_project:BACKLOG_PROJECT

以下のようにマッピングファイルが作成されます。

    .
    ├── log
    │   ├── backlog-migration-redmine-warn.log
    │   └── backlog-migration-redmine.log
    └── mapping
        ├── priorities.json
        ├── statuses.json
        └── users.json

- 1.mapping/users.json(ユーザー)
- 2.mapping/priorities.json(優先度)
- 3.mapping/statuses.json(状態)

#### プロジェクトのマッピングについて

**--projectKey** オプションに以下のようにコロン **[:]** 区切りでプロジェクトを指定することで、Backlog側の移行先のプロジェクトを指定することができます。

    --projectKey [Redmineのプロジェクト識別子]:[Backlogのプロジェクトキー]

### マッピングファイルを修正

自動作成されるファイルは以下のようにjson形式で出力されます。
Backlog側の空白の項目は自動設定できなかった項目になります。
descriptionにある項目を使い、空白を埋める必要が有ります。

    {
      "description": "Backlogに設定可能なユーザーは[admin,tanaka]です。",
      "mappings": [{
        "redmine": "admin",
        "backlog": "admin"
      }, {
        "redmine": "satou",
        "backlog": ""
      }]
    }

### 実行コマンド

[**execute**]コマンドを実行することでインポートを実行します。

    java -jar backlog-migration-redmine-[最新バージョン].jar \
      execute \
      --redmine.key [RedmineのAPIキー] \
      --redmine.url [RedmineのURL] \
      --backlog.key [BacklogのAPIキー] \
      --backlog.url [BacklogのURL] \
      --projectKey [Redmineプロジェクト識別子]:[Backlogプロジェクトキー]

サンプルコマンド：

    java -jar backlog-migration-redmine-[latest version].jar \
      execute \
      --redmine.key XXXXXXXXXXXXX \
      --redmine.url https://my.redmine \
      --backlog.key XXXXXXXXXXXXX \
      --backlog.url https://nulab.backlog.jp \
      --projectKey redmine_project:BACKLOG_PROJECT

インポートが完了すると以下のようにディレクトリが作成され
インポートに使用されたデータファイルが出力されます。

    .
    ├── log
    │   ├── backlog-migration-redmine-warn.log
    │   └── backlog-migration-redmine.log
    └── mapping
        ├── priorities.json
        ├── statuses.json
        └── users.json

#### インポートのみ実行

[**--importOnly**]オプションを利用すると前回出力したファイルからインポートのみ実行します。

    java -jar backlog-migration-redmine-[最新バージョン].jar \
      execute \
      --redmine.key [RedmineのAPIキー] \
      --redmine.url [RedmineのURL] \
      --backlog.key [BacklogのAPIキー] \
      --backlog.url [BacklogのURL] \
      --projectKey [Redmineプロジェクト識別子]:[Backlogプロジェクトキー] \
      --importOnly

#### 課題またはWikiのみ実行

[**--exclude**]オプションを利用すると指定された項目を**除外**してインポートを実行します。

    java -jar backlog-migration-redmine-[最新バージョン].jar \
      execute \
      --redmine.key [RedmineのAPIキー] \
      --redmine.url [RedmineのURL] \
      --backlog.key [BacklogのAPIキー] \
      --backlog.url [BacklogのURL] \
      --projectKey [Redmineプロジェクト識別子]:[Backlogプロジェクトキー] \
      --exclude [issue または wiki]
      
## 制限事項

### Redmineの対応バージョン
Redmineの対応バージョンは **1.1.1以降** になります。

### 実行できるユーザー
Backlogの **管理者権限** が必要になります。

### カスタムフィールドを使用しているプロジェクトの移行
Backlogで **プレミアムプラン以上** のプランを契約している必要があります。

### 移行できないリソース
* 共有ファイル
* 文書
* フォーラム
* ロールと権限

### 課題について
* Redmine側に登録してあるカスタム属性の全てが、移行するプロジェクトに登録されます。
* プライベート注記も移行されます。詳細にプライベート注記である旨の追記がされます。
* 孫課題の親課題は、 **[親課題:課題キー]** という形で詳細に記述されます。
* **textile記法** を変換しません。
* 課題登録者または、課題担当者であるユーザーがプロジェクトから削除されている場合、Backlog側では課題担当者は未設定、課題登録者・更新者はインポート実行者となります。

### Wikiについて
* Wiki登録者であるユーザーがプロジェクトから削除されている場合、Backlog側ではWiki登録者はインポート実行者となります。
* Wikiの履歴は登録されません。

### ニュースについて
* Wikiとして登録されます。
* 添付ファイルは移行できません。

### プロジェクトについて
* テキスト整形のルール： **markdown**
* Redmineのプロジェクト識別子は以下のように変換されBacklogのプロジェクトキーとして登録されます。

**ハイフン** → **アンダースコア**

**半角英子文字** → **半角英大文字**

### カスタムフィールドについて
* バージョンとユーザーはリストとして登録され固定値になります。
* 真偽値は[はい]、[いいえ]のラジオボタン形式で登録されます。

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
|優先度|2.2|

## 再インポートの仕様

Backlog側にRedmineに対応するプロジェクトキーがある場合同一プロジェクトとみなし、以下の仕様でインポートされます。

※ 対象のプロジェクトに 参加していない場合

対象プロジェクトはインポートされず以下のメッセージが表示されます。対象プロジェクトをインポートする場合は、対象プロジェクトに参加してください。[⭕️⭕️を移行しようとしましたが⭕️⭕️に参加していません。移行したい場合は⭕️⭕️に参加してください。]

|項目|仕様|
|:-----------|------------|
|グループ|同じグループ名のグループがある場合、同一とみなし登録しません。|
|プロジェクト|同じプロジェクトキーのプロジェクトがある場合、プロジェクトを作成せず対象のプロジェクトに課題やWikiを登録します。|
|課題|件名、作成者、作成日が一致する課題は登録されません。|
|Wiki|同じページ名のWikiがある場合登録しません。|
|カスタム属性|同じ名前のカスタム属性がある場合登録しません。|  

## License

MIT License

* http://www.opensource.org/licenses/mit-license.php

## お問い合わせ

お問い合わせは下記サイトからご連絡ください。

https://www.backlog.jp/contact/

[Backlog]: http://www.backlog.jp/
