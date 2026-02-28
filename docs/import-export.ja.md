# Import / Export

EZ Actions の Import / Export はクリップボードを使用します。

## Export

Menu Editor で `Export` をクリック。

結果:

- Root 全体を JSON 化
- JSON をクリップボードへコピー

## Import

Menu Editor で `Import` をクリック。

結果:

- クリップボード JSON を parse / validate
- 成功時に対象パスへ適用

## よくあるエラー

- Clipboard is empty
- Clipboard is not JSON
- Root JSON is not array
- Entry is not object / invalid

## おすすめ手順

1. まずバックアップを Export
2. JSON を編集
3. Import
4. 問題があればバックアップを再 Import
