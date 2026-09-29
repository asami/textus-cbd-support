# Model-up差分によるAI実装レビュー支援

日付: 2026-09-29
状態: 構想

## 背景

AI駆動開発では、AIが誤ったコードを生成することだけでなく、正しく動作しテストにも通る一方で、要求に比べて過剰な構造・検証・抽象化・状態管理などを持ち込むことが問題になり得る。

コード上では変更が多数のファイルやクラスへ分散するため、この種のaccidental complexityは発見しにくい。textus-cbd-supportでは、実装をModel-upして得られるモデルの「形」と変更前後の差分を、人間によるレビューの手掛かりとして利用する。

## 基本方針

初期段階から「AIの変な実装」を自動判定することを目標にしない。

中心機能は次のループを支援することである。

Implementation change
→ Model-up
→ Model Diff / Visualization
→ Human suspicion
→ AI investigation
→ Design decision

Model-upしたモデルを設計者が見て違和感を持った箇所を深掘りする。人間の違和感そのものを重要なレビュー・センサーとして扱う。

## 可視化する差分

変更前後について、少なくとも次の観点で構造差分を確認できるようにする。

- Capability
- Component
- Service
- Operation
- Dependency
- State / persistence
- Failure / Consequence
- Verificationなどの補助機構

単なるadded / removedだけでなく、上位要素との対応関係を表示する。

例として、一つのOperation追加という要求に対し、Model-up結果で複数のService、永続状態、依存、検証機構が追加されていれば、その構造変化を明瞭に見せる。

## Suspicious changeの扱い

次のような変化はレビュー対象として目立たせる候補になる。

- 上位CapabilityやUse Caseとの対応が見つからない新規要素
- 要求された変更範囲に比べて大きな構造変化
- 新しい中間層や依存経路
- 同じ責務に対する複数経路
- Failure / Consequenceから由来を説明できない防御機構
- 新しい永続状態や同期機構

ただし、これらをviolationやerrorとは判定しない。正当な設計変更である可能性があるため、「確認すべき変化」「由来を説明すべき変化」として提示する。

## Human + AI review

CBD Supportは設計判断をAIへ委譲するのではなく、人間が判断できるreview surfaceを提供する。

ツール側:
- Model-upする。
- Before / Afterを比較する。
- 構造変化と上位モデルとの対応を可視化する。
- 由来不明・変化量の大きい箇所をレビュー候補として示す。

人間:
- モデルの形から違和感を捉える。
- 深掘りすべき箇所を選ぶ。
- 採用・修正・撤回を判断する。

AI:
- 選択された箇所について、なぜ必要になったかを説明する。
- 影響範囲や代替案を調査する。
- 必要なら簡素化案を提示する。

この分業によって、AIを異常判定器として過度に賢くするより、人間の設計能力を増幅する方向を優先する。

## 将来の拡張

履歴が蓄積すれば、Use Case SliceやCapabilityの変更規模とModel Diffの通常範囲を比較し、異常に大きなmodel deltaを候補として提示することも考えられる。ただし初期実装では統計的・AI的な自動異常判定を必須にせず、Model-up差分の可視化と由来の確認を優先する。

## SimpleModelingとの関係

simplemodelingorgで整理しているModel-up Reviewを具体的に支援する機能と位置付ける。

Model-downは「必要なものが実装されているか」を確認し、Model-up Reviewは「実装されたものは本当に必要だったか」を確認する。textus-cbd-supportは後者について、実装を上位モデルへ投影し、人間が構造的な違和感を発見できるビューを提供する。


## 追記: 実装メカニズムの観測と差分

Model-upした構造の違和感に加え、AIが実装時に利用した低レベル機構・外部接触面を定量化すると、過剰実装を早期に発見する手掛かりになる。

背景となった例では、単純なファイル生成処理に対して、AIがファイル排他制御のためにJNAを導入しようとした。JNAやFileLockそのものを禁止するのではなく、「この変更で、なぜそこまで低レベルな機構が増えたのか」を人間が発見できる情報を提供する。

### UnitOfWork / DSL境界

UnitOfWork DSLを通常の実装境界とみなし、DSL経由の操作とDSL外の直接操作を区別して集計する。

- UnitOfWork DSL operation usage count
- non-DSL operation usage count
- non-DSLで利用しているAPI / mechanismの一覧
- DSL / non-DSL比率

non-DSL利用を違反とはしない。必要な実装も存在するため、レビュー対象として可視化する。

### External Surface

Java実装が外界へ接触する機構を、少なくともExternal Library、JDK External I/O / Effect、Low-level / Control Mechanismに分けて分類・集計する。

External Libraryでは、外部ライブラリ数、利用箇所数、新規追加・削除、JNAのようなnative bindingを観測する。

JDK External I/O / EffectではJava標準ライブラリも一括して安全側へ分類せず、File / filesystem、Network / HTTP / socket、Process execution、Environment / system property、Clock / time、Random、Native accessなど外部effectを持つAPIを区別する。String、collection、Math等のlocal/pure寄りの利用とは分離する。

Low-level / Control Mechanismでは、explicit lock / FileLock、Thread / Executor等のconcurrency、synchronization、reflection、dynamic class loading、native access、direct filesystem operation、raw network operation、framework / DSLを迂回する直接操作などを観測候補とする。

### Usage CountとMechanism Count

単純な呼び出し回数だけでは複雑性を捉えられない。同じFiles APIを多数回利用する場合と、Files / FileChannel / FileLock / JNAという複数種類のmechanismを少数回ずつ利用する場合では、後者の方が設計上の意味が大きいことがある。

少なくとも次を分けて観測する。

- External Effect Usage Count
- External Mechanism Count / diversity
- External Dependency Count
- non-DSL Usage Count

AIによる過剰実装では、コード量より先にmechanism diversityやmechanism depthが増える可能性がある。

### Commit Diff / CAR Version Diff

絶対値だけでなく差分を主要なレビュー情報とする。

Commit間では開発中の早期レビューとして、このcommitで増えたDSL外操作、新しい外部ライブラリ、新しいexternal effect、新しいlow-level mechanism、dependency / state / model structureの変化を見る。

CAR Version間ではRelease / Admission時のレビューとして利用する。CARはSubsystem / Subcomponentの意味を持つ配布境界なので、単なるソース差分ではなく「このCARが提供するCapabilityに対して、バージョン間で実装メカニズムがどう変化したか」を確認できる。

例:

| Metric | Before | After | Delta |
| --- | ---: | ---: | ---: |
| External libraries | 3 | 4 | +1 |
| File I/O usages | 8 | 14 | +6 |
| Explicit locks | 0 | 2 | +2 |
| Native access mechanisms | 0 | 1 | +1 |
| UnitOfWork DSL usages | 94 | 97 | +3 |
| Direct external usages | 9 | 18 | +9 |

上位のCapability / Use Caseがほとんど変化していないのに、External Surfaceやlow-level mechanismだけが大きく増えた場合、人間が深掘りすべき強いシグナルになる。

### Model Diffとの統合

最終的にはModel Diff、Capability / Operation Diff、API / DSL Diff、External Surface Diff、Dependency Diff、State / Persistence Diff、Failure / Consequence Diffを組み合わせる。

特に「Model上の変化は小さいがImplementation Mechanismの変化が大きい」という組み合わせを、人間が容易に発見できることを重視する。

これは自動的な異常判定ではない。CBD Supportは差分と構造をreview surfaceとして提示し、人間が「なぜこの変更でJNA、lock、native accessが必要なのか」と問いを立て、その箇所だけAIに説明・影響分析・簡素化案を求められるようにする。

将来的にはCapabilityの変更規模に対するmechanism complexity / external surfaceの増加量を指標化することも検討できるが、初期段階では閾値による自動拒否より、一覧・集計・差分の可視化を優先する。
