package com.zrlog.plugin.staticplus.sync;

import com.zrlog.plugin.api.BucketManageAPI;

public interface FileManage extends BucketManageAPI {

    boolean doSync();
}
