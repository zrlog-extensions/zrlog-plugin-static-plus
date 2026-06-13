# zrlog-plugin-static-plus

ZrLog 静态资源发布插件。将主题静态资源、静态缓存 HTML、文章附件、生成资源和私有备份资源发布到配置的远端静态资源仓库。

## 功能

- 配置 Git 仓库或标准 S3 API 对象存储作为发布目标
- 发布主题静态资源
- 发布静态缓存 HTML 文件
- 上传文章附件和生成资源到远端静态资源仓库
- 上传备份文件等私有资源到远端静态资源仓库
- 记录每次发布的目标、文件数量、耗时和完整失败信息

## 构建

```shell
export JAVA_HOME=${HOME}/dev/graalvm-jdk-latest
export PATH=${JAVA_HOME}/bin:$PATH
```

## 支持列表

- [x] Git 仓库
- [x] 标准 S3 API 对象存储（兼容 AWS S3、Cloudflare R2 等服务）
- [ ] Git Pages（仓库）
- [ ] Nginx + SFTP
- [ ] Nginx + FTP
- [ ] 其他文件传输协议（webdav, nas, smb）
