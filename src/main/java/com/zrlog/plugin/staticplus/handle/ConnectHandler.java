package com.zrlog.plugin.staticplus.handle;

import com.zrlog.plugin.IOSession;
import com.zrlog.plugin.api.IConnectHandler;
import com.zrlog.plugin.data.codec.MsgPacket;
import com.zrlog.plugin.staticplus.task.SyncStaticResourceRunnable;

public class ConnectHandler implements IConnectHandler {

    private IOSession ioSession;

    public void handler(IOSession ioSession, MsgPacket msgPacket) {
        this.ioSession = ioSession;
    }

    public SyncStaticResourceRunnable getSyncStaticResourceRunnable() {
        if (ioSession == null) {
            return null;
        }
        return new SyncStaticResourceRunnable(ioSession);
    }
}
