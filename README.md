# zrlog plugin static plus 

> 增强 zrlog 静态化的能力，用于自动将 zrlog 生成静态 html 或者上传的静态资源（图片/视频/文件）同步到第三方存储（不含公有化存储方案）

## 开发环境打包

```shell
export JAVA_HOME=${HOME}/dev/graalvm-jdk-latest
export PATH=${JAVA_HOME}/bin:$PATH
```

## 支持列表

- [x] github + cloudflare
- [ ] git pages（仓库）
- [ ] Nginx + SFTP
- [ ] Nginx + FTP (优先级低，传统 FTP 在淘汰的边缘上)
- [ ] 其他文件传输协议（webdav, nas, smb）