package com.zrlog.plugin.staticplus.controller;

import com.google.gson.Gson;
import com.zrlog.plugin.IOSession;
import com.zrlog.plugin.common.IdUtil;
import com.zrlog.plugin.data.codec.ContentType;
import com.zrlog.plugin.data.codec.HttpRequestInfo;
import com.zrlog.plugin.data.codec.MsgPacket;
import com.zrlog.plugin.data.codec.MsgPacketStatus;
import com.zrlog.plugin.staticplus.config.StaticPlusHistoryConfig;
import com.zrlog.plugin.staticplus.config.StaticPlusUpdateRequest;
import com.zrlog.plugin.staticplus.config.WebsiteKeyRequest;
import com.zrlog.plugin.type.ActionType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class StaticPlusController {


    private final IOSession session;
    private final MsgPacket requestPacket;
    private final HttpRequestInfo requestInfo;
    private final Gson gson = new Gson();

    public StaticPlusController(IOSession session, MsgPacket requestPacket, HttpRequestInfo requestInfo) {
        this.session = session;
        this.requestPacket = requestPacket;
        this.requestInfo = requestInfo;
    }

    public void githubEvent() {
        //handle action
        response(StaticPlusApiResponse.success());
    }

    public void update() {
        session.sendMsg(new MsgPacket(updateRequest(), ContentType.JSON, MsgPacketStatus.SEND_REQUEST, IdUtil.getInt(), ActionType.SET_WEBSITE.name()), msgPacket -> {
            response(StaticPlusApiResponse.success());
            //更新缓存，可选
            session.sendJsonMsg(Collections.emptyMap(), ActionType.REFRESH_CACHE.name(), IdUtil.getInt(), MsgPacketStatus.SEND_REQUEST);
        });
    }

    public void history() {
        session.sendJsonMsg(WebsiteKeyRequest.of("syncHistory"), ActionType.GET_WEBSITE.name(), IdUtil.getInt(), MsgPacketStatus.SEND_REQUEST, msgPacket -> {
            StaticPlusHistoryConfig historyConfig = historyConfig(msgPacket);
            response(StaticPlusApiResponse.success(historyConfig.getSyncHistory()));
        });
    }

    public void index() {
        session.sendJsonMsg(WebsiteKeyRequest.of("syncTemplate,syncHtml,syncAttached,syncRemoteType,git,s3,syncHistory"),
                ActionType.GET_WEBSITE.name(), IdUtil.getInt(), MsgPacketStatus.SEND_REQUEST, msgPacket -> {
            StaticPlusPageData pageData = pageData(msgPacket);
            pageData.normalize();
            pageData.setAdminColorPrimary(requestInfo.getAdminColorPrimary());
            Map<String, Object> data = new HashMap<>();
            data.put("theme", requestInfo.isDarkMode() ? "dark" : "light");
            data.put("data", gson.toJson(pageData));
            session.responseHtml("/templates/index", data, requestPacket.getMethodStr(), requestPacket.getMsgId());
        });
    }

    private StaticPlusPageData pageData(MsgPacket msgPacket) {
        StaticPlusPageData pageData = gson.fromJson(msgPacket.getDataStr(), StaticPlusPageData.class);
        return pageData == null ? new StaticPlusPageData() : pageData;
    }

    private StaticPlusHistoryConfig historyConfig(MsgPacket msgPacket) {
        StaticPlusHistoryConfig historyConfig = gson.fromJson(msgPacket.getDataStr(), StaticPlusHistoryConfig.class);
        return historyConfig == null ? new StaticPlusHistoryConfig() : historyConfig;
    }

    private StaticPlusUpdateRequest updateRequest() {
        StaticPlusUpdateRequest request = new StaticPlusUpdateRequest();
        request.setSyncTemplate(paramValue("syncTemplate"));
        request.setSyncHtml(paramValue("syncHtml"));
        request.setSyncAttached(paramValue("syncAttached"));
        request.setSyncRemoteType(paramValue("syncRemoteType"));
        request.setGit(paramValue("git"));
        request.setS3(paramValue("s3"));
        return request;
    }

    private String paramValue(String key) {
        if (requestInfo.getParam() == null || requestInfo.getParam().get(key) == null || requestInfo.getParam().get(key).length == 0) {
            return "";
        }
        return requestInfo.getParam().get(key)[0];
    }

    private void response(Object data) {
        session.sendMsg(new MsgPacket(data, ContentType.JSON, MsgPacketStatus.RESPONSE_SUCCESS, requestPacket.getMsgId(),
                requestPacket.getMethodStr()));
    }
}
