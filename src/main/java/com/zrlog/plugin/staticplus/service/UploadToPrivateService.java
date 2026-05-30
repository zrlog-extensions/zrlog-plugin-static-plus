package com.zrlog.plugin.staticplus.service;

import com.zrlog.plugin.api.Capability;
import com.zrlog.plugin.api.Service;

@Service("uploadToPrivateService")
@Capability(
        key = "staticPlus.uploadPrivate",
        type = "service",
        label = "上传到私有静态资源仓库",
        description = "上传备份文件等私有资源到配置的静态资源仓库。",
        exposure = {"internal"},
        timeoutSeconds = 120
)
public class UploadToPrivateService extends UploadService {
}
