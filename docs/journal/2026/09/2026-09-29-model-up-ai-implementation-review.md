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
