# ハイエロファントの結界

## 機能

- ファイルシステムに触れた瞬間に察知してコマンドを発射

## 開発

```
$ clj -M:dev
$ vim src/hierophant/core.clj
  :Connect 5876 src

$ clj -M -m hierophant.core
```

## リリース

```
$ clj -T:build clean
$ clj -T:build uber
```

## 使い方

```
$ java -jar hierophant.jar --help
$ javaw -jar hierophant.jar dir1 dir2/* dir3
```
