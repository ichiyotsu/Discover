# Release 规范

## 版本与 Tag

- 正式版：`x.y.z`
- 测试版：`x.y.z-beta.N`
- 正式版与测试版轮流推进，已发布版本不删除
- 历史 tag `0.1.0-beta1` 保留；后续使用 `beta.N`
- Release 标题只写版本号

## 安装包

- Android：`Luma-<版本号>-android.apk`
- 桌面端保留平台后缀

## Release Notes

只用两段：

```markdown
# 新增功能

## I. 功能名称

一句话说明。

# 设计特性

## I. 设计特性名称

一句话说明。
```

不写内部实现、提交记录、构建过程；没有对应内容时写「无」。
