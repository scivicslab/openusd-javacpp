# openusd-javacpp — 計画

OpenUSD core（Sdf / Usd / UsdGeom）を JavaCPP で Java から呼べるようにし、Maven で配る。
描画（Hydra）と Python は含めない。

## 段階

- [x] OpenUSD v26.08 を monolithic でビルド（`~/.local/openusd-26.08`、`libusd_ms.so`）
- [x] Maven プロジェクトの骨組み（pom、preset クラス、`linux-x86_64` 分類子 jar）
- [x] 第1版の束縛（JUnit 2 件が通過: CreateNew → DefinePrim → CreateAttribute → Set → Save → Open → GetPrimAtPath）: `TfToken`, `SdfPath`, `UsdStage`（CreateNew / Open / DefinePrim / OverridePrim / GetPrimAtPath / Save / GetRootLayer）, `UsdPrim`（CreateAttribute / CreateRelationship）, `UsdAttribute.Set`（string / double / int / bool / GfVec3d）
- [x] 第2版の一部: `UsdGeomXform.Define`, `UsdGeomXformable(prim).AddTranslateOp().Set(GfVec3d)`（`xformOpOrder` も書かれる）
- [ ] `VtArray<…>`: `vt/array.h` は JavaCPP のパーサが通らない（`template<typename ELEM> class VtArray : public Vt_ArrayBase` で停止）。配列値の属性（`points`、`token[]`）はまだ書けない。アダプタを書くか、パーサに通る形を探す
- [ ] `UsdGeomMesh` / `UsdGeomBasisCurves`（`VtArray` が要る）, `SdfLayer.SetSubLayerPaths`
- [x] 図の描画: 文書と同じディレクトリの `actor-tree.jsh`（jshell）が図の構造を `.usda` に書き、`make-diagram.sh` が jshell と Blender を順に呼ぶ。`examples/render_diagram.py` が Blender 4.5.9（`~/.local/blender`）で PNG にする。最初の適用先は `OneConversationAndItsRecord_260913_oo01` のアクターツリー
- [x] テスト（書き出しと再読込）
- [ ] テスト: `over` 層を重ねて `Stage.Open` で合成値を読む（`VtValue` から値を取り出す束縛が要る）
- [ ] plugInfo.json の同梱: いまは surefire の `PXR_PLUGINPATH_NAME` で `~/.local/openusd-26.08/lib/usd` を指している。jar だけで動くように `lib/usd` の必要分を native jar に入れ、`Loader` で展開した場所を plugin registry に教える
- [ ] `mvn install` でローカル配布。公開は `ReleaseProcedure_260830_oo01` に従う

## 決めたこと

- ディレクトリ `~/works/openusd-javacpp`、`groupId=com.scivicslab`、`artifactId=openusd-javacpp`
- 版は `0.1.0-SNAPSHOT` を仮置き（版番号は人が決める）
- Java は `--release 21`（JDK は 25）
- 対象プラットフォームは `linux-x86_64` のみ

## 既知の難所

- `TfRefPtr<UsdStage>` → `TfWeakPtr<UsdStage>` の暗黙変換（`UsdGeomXform::Define` が `UsdStagePtr` を取る）
- `UsdAttribute::Set<T>` はテンプレート。使う型ごとに実体化を列挙する
- `VtValue` は型消去の箱。Java 側には型ごとの `Set` を出す
