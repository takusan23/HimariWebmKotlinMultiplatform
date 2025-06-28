# HimariWebmKotlinMultiplatform
`Kotlin Multiplatform`製の`WebM`のパーサー、ミキサーライブラリ。  
`Android`や`Web`で使えます。`iOS`は持ってないのですが多分動くんじゃないかな。

また、`JavaScript`の`MediaRecorder`が生成する`WebM`ファイルをシークできるようにする処理も提供しています。  
`JavaScript`側から使いやすくするために、（今のところ）この処理だけ`npm ライブラリ`にしてあります。

# 例
// todo ちゃんと書く
今は ParseAndMuxerText.kt 参照

# ダウンロード
## MavenCentral
まだ`MavenCentral`に公開されてませｎ

## npm ライブラリ
`npm install`出来ます。

```shell
npm install takusan23/himari-webm-kotlin-multiplatform-npm-library
```

今のところ、以下の組み合わせて利用できることを確認しました
- `React`+`Vite`
- `Next.js`の`サーバーコンポーネント`
- `Next.js`の`クライアントコンポーネント`（ひと手間必要です）

### Next.js で使う場合
サーバーコンポーネントの場合は問題なく利用できます。  
クライアントコンポーネントでこのライブラリを利用する場合、必ずクライアント側で読み込まれるように遅延ロードする必要があります。多分。

よって、` import { ... } from "himari-webm-kotlin-multiplatform" `は使えず、  
` const { ... } = await import("himari-webm-kotlin-multiplatform") `をクライアントだけで呼び出す必要があります。

`useEffect()`や`clickHandler`なんかはクライアント側なので、ここでロードすればよいはず

# 環境構築
- ソースコードをダウンロードして、`Android Studio`や`IDEA`で開く
- 待つ

## テスト実行
テストのフォルダを選んで、実行

![run test](https://oekakityou.negitoro.dev/original/12b6a051-d803-4a6b-b6d6-b1b36ffe9c9f.png)

## MavenCentral 提出
// todo

## npm ライブラリ作成
- `Gradle`のコマンドを入力する画面を開き、以下のコマンドを叩く
  - `gradle wasmJsBrowserProductionRun`
- `build/wasm/packages/himari-webm-kotlin-multiplatform`フォルダをの中身を移動
  - https://github.com/takusan23/himari-webm-kotlin-multiplatform-npm-library に移動させる
  - `package.json`のバージョンとかを直す？
- コードを修正する
  - `himari-webm-kotlin-multiplatform.uninstantiated.mjs`ファイルを開き、
  - 以下の行を
    - `const isNodeJs = (typeof process !== 'undefined') && (process.release.name === 'node');`
  - こう書き直す
    - `const isNodeJs = (typeof process !== 'undefined') && (process.release?.name === 'node');`
  - こうしないと`Next.js`のクライアントコンポーネントでエラーになってしまった
- 修正したら push

# ライセンス
`Apache License Version 2.0`

# special thanks
https://github.com/Kotlin/multiplatform-library-template