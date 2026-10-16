# CBD Support: Component Usage CapabilityとComponentメタデータのhandoff

Date: 2026-10-15
Status: 方針訂正を仕様・計画へ反映済み。新契約の実装・実行検証は未着手。
対象: CBD Phase 11 / 11.1、Cozy Phase 64、CNCF Phase 78

## 開発チャットが採用する現行方針

`ApplicationCapability`を`ComponentUsageCapability`として整理する。
ComponentUsageCapabilityはUse Case・利用事例・根拠から帰納的に整理する
「このComponentを何に使えるか」のモデルである。ComponentCapabilityは
提供・要求・実現先の契約を演繹的に扱う別モデルとして保持する。
両者を統合せず、明示的で出所を保持した多対多の対応関係で結ぶ。

ApplicationはDomainと対になるApplication Model／Tierの意味に限定する。
今回はComponentレビュー用のモデルメタデータを扱う。所属先と配布単位は
レビュー対象ComponentのCARまたは生成Directoryであり、独立した
「Application所有パッケージ」は導入しない。

ユーザーから追加された実施条件:

- リリース前なので互換性は不要。旧型・旧名・旧形式を残すことによる複雑化を避ける。
- 旧型／旧メソッドの利用箇所はコンパイルエラーで検出できる方を優先する。
- type alias、deprecated forwarding、旧名のconstructor／extractor、dual reader、
  新旧切替、旧JSONの自動変換は実装しない。
- CML／JSONはコンパイル対象外なので、生成元・fixture・生成物を同時更新し、
  旧形式は通常の構文／schema検証で拒否する。専用の互換・移行サブシステムは作らない。

## この訂正で撤回する前提

同日の先行handoffと仕様では、既存ApplicationCapabilityの意味を確認しないまま
Application全体の所有パッケージを導入した。その前提を撤回する。
`applicationPackage`、`meta/application-capability/`、Application-only package、
旧`cozy.application-capability.v1`を継続する案は現行計画ではない。
CNCFの旧`application-capability-package.md`は撤回を示す文書リンクだけにした。
これは実行時互換経路ではない。

維持するのは、正規の名前・配置、Directory/CARで同じ相対配置、Cozyの正規生成、
CNCF経由の取得、CBDでの重複検証を避ける方針である。
通常生成物9ファイルを衝突禁止リストへ追加する案は引き続き採用しない。
CAR組み立て時の実際の重複エントリ検出は既存の責任として残す。

## モデルとパッケージの現行契約

| 項目 | 現行方針 |
| --- | --- |
| 帰納モデル | ComponentUsageCapability。明示したComponent owner、利用根拠・source・origin・参照・対応付けを保持 |
| 演繹モデル | ComponentCapability。既存PROVIDED/REQUIREDと実現先の契約を保持 |
| 対応 | Usage/Use CaseとUsage/ComponentCapabilityの多対多。名前や配列位置から推論しない |
| 型・CML | ComponentUsageCapability、COMPONENT-USAGE-CAPABILITY |
| 新wire | cozy.component-usage-capability.v1、kind=COMPONENT_USAGE |
| owner | レビュー対象Component。モデルのqualified IDとComponent identityは別domain |
| request | revision 2。既存packagesにusageCapabilities参照を追加。applicationOutputとapplicationPackageは不要 |
| query | traceComponentUsageCapability。関連input/result/gap/labelも一括更新 |
| 旧形式 | 互換対応なし。旧typed APIを削除し、残る呼出しをコンパイルで検出 |

```text
<component-package-root>/              # CAR内部でも同じ配置
  component-descriptor.json
  config/default.conf                  # configがある場合
  meta/
    component-capability/
      index.json
      <indexed-resource>.json
    component-usage-capability/
      index.json
      usage.json
```

Usage indexは`{"schemaVersion":1,"projections":["usage.json"]}`。
同じ入力CMLに複数ComponentのUsage定義があれば、明示ownerで分割して生成する。
全定義を各CARに丸ごと複製しない。他Componentとの関係は外部参照で保持する。
ComponentUsageCapabilityはComponentメタデータ全体の別名ではなく、その一family。
今回の変更で既存の他の構造／意味／APIメタデータまで改名・再設計しない。

帰納の根拠だけでPROVIDED/REQUIRED、実行成功、権限、現在の利用可否を作らない。
未提供のforward UseCase projectionは引き続きunavailableとし、逆向きの発見根拠を
正方向の要求契約に読み替えない。既存のComponent契約のversion compatibilityは
別の意味なので保持する。「互換性不要」は旧Application形式を残さないという条件。

## 正本と責任分担

| Owner | 正本（各repositoryからの相対パス） |
| --- | --- |
| Cozy | `docs/spec/component-usage-capability.md`：意味、型、CML、wire、所有、互換不要の変更契約 |
| Cozy | `docs/spec/cml-capability-model.md`：R1–R11とpublication訂正、旧実装記録との境界 |
| CNCF | `docs/spec/component-usage-capability-package.md`：Component所有Directory/CARと取得 |
| CNCF | `docs/spec/component-capability.md`：Component ABIとの分離 |
| CBD | [Component Usage adoption](../../../spec/component-usage-capability-adoption.md)：consumer型・query・gapの変更表 |
| CBD | [ingestion](../../../spec/capability-ingestion-contract.md)、[catalog](../../../spec/capability-catalog-contract.md)、[query](../../../spec/capability-query-contract.md)：各境界の現行訂正 |

生成・source/wireの意味はCozy、package acquisitionはCNCF、取り込み・review/queryはCBD。
CNCFにCozy/CBD実装への依存やUsage意味解析を追加しない。
CBDに独自ZIP展開・CML再解析・processによるfile read・CNCF保証の再検証を追加しない。
管理hash、安全台帳、replay、広い異常系の検証行列も追加しない。

## 実装順序

1. **型と所有関係を確定:** 本仕様に従い旧Application型・method・fieldを除去する。
   owner、usage evidence、mappingの明示入力を定める。古い全envelopeをrenameして
   複製する処理にしない。コンパイルエラーで全利用箇所を洗い出す。
2. **Cozy CAP-64-PKG01 / CNCF CAP-78-PKG01:** 新CML/IR/request/JSONから
   Component所有Directoryを正規生成し、CNCFの公開typed取得APIに接続する。
   既存のresource取得を再利用し、Application bridgeの別名を残さない。
3. **CBD CAP-11-PKG01:** ingestionだけでなくcatalog/queryの型・selector・result・gap、
   portable schema、fixtureも一緒に更新する。まず実CMLから生成したDirectoryを
   読んで帰納モデルと演繹モデルを区別した問い合わせまで通す。
4. **同じComponent imageをCARへ:** normal package-carでUsageメタデータも同梱し、
   DirectoryとCARの意味・query結果が同じことを確認する。取得locationの違いは保持。
5. **CBD Phase 11.1:** 修正済みqueryをWeb/MCPへ接続する。旧operationの互換口は
   残さずgenerated callerも更新する。帰納の根拠と契約上の保証を区別して表示する。

この一連の変更はproducer/consumerを協調して行う。旧型を残して各段階を通す
互換フェーズは設けない。Directoryを読む日常作業にCAR作成・SBT・Ivy公開は不要。
新しいCMLのコンパイル／生成を実施する工程と、生成済みmetadataを読む工程は別である。

## 計画と受入

[Phase 11](../../../phase/phase-11.md)／[checklist](../../../phase/phase-11-checklist.md)、
Cozy Phase 64、CNCF Phase 78の既存PKG01をこの訂正後の範囲に更新した。
並行する別のApplication package項目は作らない。
[Phase 11.1](../../../phase/phase-11.1.md)／[checklist](../../../phase/phase-11.1-checklist.md)
は新しい型・query・Directory/CARの引継ぎを受ける。全体テストの最終担当は変更しない。

focused acceptanceは、影響するproducer/consumer/generated codeのコンパイル、
複数Componentのowner分割、帰納根拠と演繹契約の分離、多対多の出所保持、
実Directory取り込みと同じCARの結果一致を中心とする。既存の関連テストを更新し、
新旧二重のcompatibility suiteや下位保証の再検証を作らない。

過去のCAP-11-01〜03、CAP-64-01〜05、CAP-78-01〜05の受入記録は保存する。
ただし旧型／旧wireで通った結果は新契約の受入ではない。追加PKG01は全て未完了。
今回の範囲はtransport追加だけではなく、source/IR/ABI/consumer API変更を含むため、
開発チャットは差分を再見積りしてlive planへ採用する。このチャットはlive plan JSON、
Goal状態、承認・実行履歴を変更していない。

## 作業場所と今回の作業範囲

CBD root: `/Users/asami/src/dev2026/textus-cbd-support`。
Cozy/CNCFはCBD開発が既に利用する以下の専用checkoutへ文書を反映した。

- `.codex-workflow/checkouts/cozy-phase64`
- `.codex-workflow/checkouts/goldenport-cncf-phase78`

環境ごとの`dev????`差はrepository identityの違いではない。
新規worktree、別main checkoutへのmerge、commit、push、共有Ivy公開は行っていない。
旧実装の型名・ファイル名を含む過去の受入説明は履歴として残し、現行契約の訂正を
明示している。製品コードの旧名削除は上記の開発作業で実施する。
今回は仕様・design・phase/checklist・handoffの更新のみで、SBT／runtime検証は未実施。
開発チャットへのメッセージ送信は行っていない。このjournalを採用開始点にする。

文書確認: 更新23文書の追加・変更ローカルリンク38件（anchorを含む）と、
3repositoryの`git diff --check`が成功。新モデルのコンパイル・実行成功を示すものではない。
