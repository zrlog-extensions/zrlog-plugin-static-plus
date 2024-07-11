package com.zrlog.plugin.staticplus;

import com.zrlog.plugin.IOSession;
import com.zrlog.plugin.client.ClientActionHandler;
import com.zrlog.plugin.data.codec.MsgPacket;
import com.zrlog.plugin.staticplus.handle.ConnectHandler;
import com.zrlog.plugin.staticplus.task.SyncStaticResourceRunnable;

import java.util.Objects;

public class StaticPlusClientActionHandler extends ClientActionHandler {

    private final ConnectHandler connectHandler;

    public StaticPlusClientActionHandler(ConnectHandler connectHandler) {
        this.connectHandler = connectHandler;
    }

    @Override
    public void refreshCache(IOSession session, MsgPacket msgPacket) {
        SyncStaticResourceRunnable runnable = connectHandler.getSyncStaticResourceRunnable();
        if (Objects.isNull(runnable)) {
            return;
        }
        runnable.run();
    }
}
