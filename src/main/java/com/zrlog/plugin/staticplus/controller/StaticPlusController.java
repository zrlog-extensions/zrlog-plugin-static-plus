package com.zrlog.plugin.staticplus.controller;

import com.google.gson.Gson;
import com.zrlog.plugin.IOSession;
import com.zrlog.plugin.common.IdUtil;
import com.zrlog.plugin.common.LoggerUtil;
import com.zrlog.plugin.data.codec.ContentType;
import com.zrlog.plugin.data.codec.HttpRequestInfo;
import com.zrlog.plugin.data.codec.MsgPacket;
import com.zrlog.plugin.data.codec.MsgPacketStatus;
import com.zrlog.plugin.type.ActionType;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class StaticPlusController {


    private final IOSession session;
    private final MsgPacket requestPacket;
    private final HttpRequestInfo requestInfo;

    public StaticPlusController(IOSession session, MsgPacket requestPacket, HttpRequestInfo requestInfo) {
        this.session = session;
        this.requestPacket = requestPacket;
        this.requestInfo = requestInfo;
    }

    public void githubEvent() {
        //LoggerUtil.getLogger(StaticPlusController.class).info(requestInfo.simpleParam().toString());
        //handle action
        Map<String, Object> map = new HashMap<>();
        map.put("success", true);
        session.sendMsg(new MsgPacket(map, ContentType.JSON, MsgPacketStatus.RESPONSE_SUCCESS, requestPacket.getMsgId(), requestPacket.getMethodStr()));
    }

    public void update() {
        session.sendMsg(new MsgPacket(requestInfo.simpleParam(), ContentType.JSON, MsgPacketStatus.SEND_REQUEST, IdUtil.getInt(), ActionType.SET_WEBSITE.name()), msgPacket -> {
            Map<String, Object> map = new HashMap<>();
            map.put("success", true);
            session.sendMsg(new MsgPacket(map, ContentType.JSON, MsgPacketStatus.RESPONSE_SUCCESS, requestPacket.getMsgId(), requestPacket.getMethodStr()));
            //更新缓存，可选
            session.sendJsonMsg(new HashMap<>(), ActionType.REFRESH_CACHE.name(), IdUtil.getInt(), MsgPacketStatus.SEND_REQUEST);
        });
    }

    public void index() {
        Map<String, Object> keyMap = new HashMap<>();
        keyMap.put("key", "syncTemplate,syncHtml,syncAttached,syncRemoteType,git");
        session.sendJsonMsg(keyMap, ActionType.GET_WEBSITE.name(), IdUtil.getInt(), MsgPacketStatus.SEND_REQUEST, msgPacket -> {
            Map map = new Gson().fromJson(msgPacket.getDataStr(), Map.class);
            Map<String, Object> data = new HashMap<>();
            data.put("theme", Objects.equals(requestInfo.getHeader().get("Dark-Mode"), "true") ? "dark" : "light");
            if (Objects.isNull(map.get("syncRemoteType"))) {
                map.put("syncRemoteType", "git");
            }
            data.put("data", new Gson().toJson(map));
            session.responseHtml("/templates/index.html", data, requestPacket.getMethodStr(), requestPacket.getMsgId());
        });
    }
}