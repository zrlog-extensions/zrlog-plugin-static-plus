package com.zrlog.plugin.staticplus.service;

import com.zrlog.plugin.api.Capability;
import com.zrlog.plugin.api.Service;

@Service("uploadToPrivateService")
@Capability(
        key = "staticPlus.uploadPrivate",
        type = "service",
        label = "上传私有静态资源",
        description = "上传私有附件或生成资源到静态化远端存储",
        exposure = {"internal"},
        timeoutSeconds = 120
)
public class UploadToPrivateService extends UploadService {
}
