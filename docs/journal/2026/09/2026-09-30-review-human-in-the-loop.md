# CBD Review: KPIと人間の直感を使うHuman-in-the-Loopレビュー

日付: 2026-09-30
状態: 設計方針

## 決定

textus-cbd-supportのReview機能は、AIやルールエンジンがコンポーネントの良否を全面的に判定する機能ではなく、**人間が適切な設計判断を行うためのHuman-in-the-Loop支援機能**として位置付ける。

基本原則は「機械が測れるものは機械に測らせ、人間には判断に集中してもらう」である。

## Component KPI

Component単位で、Executable Specification/Test、Capability/Use Caseとのtraceability、Model Diff、Dependency、External Library、DSL外操作、External I/O/Effect、Low-level mechanism、State/Persistence、Failure/Consequence、利用可能ならruntime evidenceなどを集約する。

KPIは品質そのものの点数ではなく、レビューのためのindicatorとする。特に絶対値だけでなくcommit間・CAR version間のdeltaを重視する。

異質な指標を一つのquality scoreへ潰すことは避ける。人間が「なぜこの数値になったのか」「どこが変わったのか」を追えることを優先する。

## 人間の直感

Model-up、Aggregate/View/Workflow、Capability、Use Case Slice、Dependency、External Surfaceなどを可視化し、人間が構造上の違和感を発見できるreview surfaceを作る。

「テストは通るが何か変だ」「要求に対して構造が大きすぎる」「責務の分け方が不自然」といった感覚を、排除すべき曖昧さではなく重要なreview sensorとして扱う。

Reviewの基本ループは以下とする。

```text
Candidate / Change
 -> Evidence / KPI / Diff
 -> Review View
 -> Human suspicion
 -> focused AI investigation
 -> Human judgment
 -> Admission / Revision / Withdrawal
```

AIは違和感を持った箇所の由来説明、影響分析、代替案・簡素化案の調査を担当する。AI自身を最終的な設計判定器にはしない。

## 既存CAR Reviewとの関係

既存のcanonical Review Report、Evidence admission、deterministic gateは維持する。機械的に決定可能な条件は自動gateで扱ってよい。

ただしgate結果はReview全体ではなく、人間の判断に提供される重要なevidenceの一つとする。Review productの上位目的を「自動判定」ではなく「人間が効率よく、根拠を持って判断できる状態を作ること」に置く。

## Candidate-Admissionとの接続

CBD SupportがEvidence/KPI/Review Contextを準備し、人間または明示的なAdmission PolicyがCandidateをAdmissionする。

これにより、deterministic validation、AI assistance、人間の直感を、それぞれ異なるauthorityを持つものとして共存させる。

今回の方針は、2026-09-29のModel-up差分レビュー構想を一般化したものでもある。Model-up ReviewはHuman-in-the-Loop Reviewの主要なreview surfaceの一つとして位置付ける。


## 追跡性を使った抽象モデルレビュー

Human Reviewの通常経路はProgramではなくModelとする。Use Case / Use Case Sliceなどの要求モデルから、Application Capability、Component Capability、Component / Service / Operation、Aggregate / View、Workflow / State Machine、Executable Specification、Program / runtime evidenceまでのtraceabilityをCBD Support上で辿れるようにする。

人間は個々のモデル要素だけでなく、抽象モデル間の関係をレビューする。要求に対して実現構造が過大、責務分割が不自然、特定のAggregateへ無関係なUse Caseが集中、Capabilityから実現への対応が薄い、Executable Specificationは通るが重要な要求との対応が見えない、といった違和感をreview sensorとして利用する。KPI、delta、runtime evidenceも同じモデル空間に重ねる。

Programレベルの通常の正しさは、型とコンパイル、pure functional programming、ADT、Algebra / DSL、Free Monad / Interpreter境界、Executable SpecificationなどのProgramming Harnessへ委ねる。人間がProgramを直接確認するのは、Model-upされたレビュー面で違和感が見つかった後のdrill-down経路とする。

原則:

```text
機械: 低い抽象度を網羅的に検査
人間: Model-upされた高い抽象度とtraceabilityをレビュー
異常: 該当箇所だけ詳細モデル -> Executable Specification -> Programへdrill down
```

これを **Review Up, Drill Down on Anomaly** と呼ぶ。

人間による行単位のProgram Reviewを通常工程へ戻しすぎると、AIがProgramming Harnessの制約内で実装するという構造とは別の実装判断経路が増え、Harnessの拘束性と再現性を弱める。そのためProgram Reviewは診断上必要な場合に限定する。
