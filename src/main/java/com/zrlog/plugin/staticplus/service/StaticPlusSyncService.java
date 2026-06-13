package com.zrlog.plugin.staticplus.service;

import com.zrlog.plugin.IOSession;
import com.zrlog.plugin.api.Capability;
import com.zrlog.plugin.api.IPluginService;
import com.zrlog.plugin.api.ScheduledCapability;
import com.zrlog.plugin.api.Service;
import com.zrlog.plugin.data.codec.MsgPacket;
import com.zrlog.plugin.data.codec.MsgPacketStatus;
import com.zrlog.plugin.message.CapabilityInvokeResult;
import com.zrlog.plugin.staticplus.task.SyncStaticResourceRunnable;

import java.util.HashMap;
import java.util.Map;

@Service("staticPlus.sync")
@Capability(key = "staticPlus.sync", riskLevel = "medium")
@ScheduledCapability(
        key = "staticPlus.sync",
        label = "同步静态化资源",
        description = "同步模板静态资源和静态缓存文件到配置的远端存储（支持 Git 与标准 S3 API，兼容 R2 等服务）。",
        defaultCron = "*/1 * * * *",
        timeoutSeconds = 300
)
public class StaticPlusSyncService implements IPluginService {

    @Override
    public void handle(IOSession session, MsgPacket msgPacket) {
        SyncStaticResourceRunnable runnable = new SyncStaticResourceRunnable(session);
        runnable.run();

        CapabilityInvokeResult result = new CapabilityInvokeResult();
        result.setSuccess(runnable.isSuccess());
        result.setErrorMessage(runnable.isSuccess() ? "" : runnable.getMessage());
        Map<String, Object> data = new HashMap<>();
        data.put("filesCount", runnable.getFilesCount());
        data.put("message", runnable.getMessage());
        data.put("uploadTimeMs", runnable.getUploadTimeMs());
        data.put("syncRemoteType", runnable.getSyncRemoteType());
        result.setData(data);
        session.sendJsonMsg(result, msgPacket.getMethodStr(), msgPacket.getMsgId(),
                result.isSuccess() ? MsgPacketStatus.RESPONSE_SUCCESS : MsgPacketStatus.RESPONSE_ERROR);
    }
}
