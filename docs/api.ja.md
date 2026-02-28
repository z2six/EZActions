# API

このページは EZ Actions 2.0.0.0 の公開 API `org.z2six.ezactions.api` を説明します。

## API でできること

GUI でできる操作のほぼ全て + runtime 制御:

- メニューツリー読み取り
- Action / Bundle の追加・更新・削除・並び替え
- `locked` 設定
- JSON import/export/validate
- editor/config/radial の起動
- 一時 runtime radial（非永続）の起動

## Access

```java
import org.z2six.ezactions.api.EzActions;
import org.z2six.ezactions.api.EzActionsApi;

EzActionsApi api = EzActions.get();
```

## Action type

- `KEY`
- `COMMAND`
- `ITEM_EQUIP`

## modpack 向け注意

API で Bundle keybind を有効化した場合、ユーザー再起動が必要です。

## 完全な参照

完全シグネチャと高度な例は、最も更新が早い英語 API ページを参照してください。
