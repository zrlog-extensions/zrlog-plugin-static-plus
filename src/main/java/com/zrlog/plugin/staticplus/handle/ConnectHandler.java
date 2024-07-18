package com.zrlog.plugin.staticplus.handle;

import com.zrlog.plugin.IOSession;
import com.zrlog.plugin.RunConstants;
import com.zrlog.plugin.api.IConnectHandler;
import com.zrlog.plugin.data.codec.MsgPacket;
import com.zrlog.plugin.staticplus.task.SyncStaticResourceRunnable;
import com.zrlog.plugin.type.RunType;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ConnectHandler implements IConnectHandler {

    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    private SyncStaticResourceRunnable syncStaticResourceRunnable;

    public void handler(IOSession ioSession, MsgPacket msgPacket) {
        this.syncStaticResourceRunnable = new SyncStaticResourceRunnable(ioSession);
        executorService.scheduleAtFixedRate(syncStaticResourceRunnable, 0, 1, RunConstants.runType == RunType.BLOG ? TimeUnit.MINUTES : TimeUnit.HOURS);
    }

    public SyncStaticResourceRunnable getSyncStaticResourceRunnable() {
        return syncStaticResourceRunnable;
    }
}