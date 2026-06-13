package com.zrlog.plugin.staticplus.service;

import com.zrlog.plugin.IOSession;
import com.zrlog.plugin.api.Capability;
import com.zrlog.plugin.api.IPluginService;
import com.zrlog.plugin.api.Service;
import com.zrlog.plugin.data.codec.MsgPacket;

@Service("staticPlus.refreshCache")
@Capability(
        key = "staticPlus.refreshCache",
        type = "event_handler",
        label = "刷新静态资源缓存",
        description = "响应系统缓存刷新事件，发布模板静态资源和静态缓存文件。",
        exposure = {"runtime_event"},
        riskLevel = "medium",
        timeoutSeconds = 300,
        channel = "system.refreshCache"
)
public class StaticPlusRefreshCacheService implements IPluginService {

    @Override
    public void handle(IOSession session, MsgPacket msgPacket) {
        new StaticPlusSyncService().handle(session, msgPacket);
    }
}
