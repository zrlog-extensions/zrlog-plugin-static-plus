package com.zrlog.plugin.staticplus.sync;

import com.zrlog.plugin.api.BucketManageAPI;
import com.zrlog.plugin.common.vo.UploadFile;

import java.util.List;

public interface FileManage extends BucketManageAPI {

    /**
     * 返回同步成功后的文件列表
     *
     * @return
     */
    List<UploadFile> doSync();
}
