# zrlog-plugin-static-plus

ZrLog 静态资源同步插件。将主题静态资源、静态缓存 HTML 文件和附件同步到配置的 Git 远端仓库。

## 功能

- 配置 Git 远端仓库、分支和访问凭据
- 同步主题静态资源
- 同步静态缓存 HTML 文件
- 同步上传到附件库中的媒体资源
- 记录每次同步的文件数量和执行结果

## 构建

```shell
export JAVA_HOME=${HOME}/dev/graalvm-jdk-latest
export PATH=${JAVA_HOME}/bin:$PATH
```

## 支持列表

- [x] github + cloudflare
- [ ] git pages（仓库）
- [ ] Nginx + SFTP
- [ ] Nginx + FTP
- [ ] 其他文件传输协议（webdav, nas, smb）
