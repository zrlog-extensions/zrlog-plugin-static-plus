package com.zrlog.plugin.staticplus.task;

import com.google.gson.Gson;
import com.zrlog.plugin.IOSession;
import com.zrlog.plugin.common.IdUtil;
import com.zrlog.plugin.common.LoggerUtil;
import com.zrlog.plugin.common.model.BlogRunTime;
import com.zrlog.plugin.common.model.TemplatePath;
import com.zrlog.plugin.common.vo.UploadFile;
import com.zrlog.plugin.data.codec.ContentType;
import com.zrlog.plugin.data.codec.MsgPacketStatus;
import com.zrlog.plugin.staticplus.sync.FileManage;
import com.zrlog.plugin.staticplus.sync.GitFileManageImpl;
import com.zrlog.plugin.staticplus.utils.SyncFileInfoCacheUtils;
import com.zrlog.plugin.type.ActionType;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

public class SyncStaticResourceRunnable implements Runnable {

    private static final Logger LOGGER = LoggerUtil.getLogger(SyncStaticResourceRunnable.class);

    private final IOSession session;

    private static final ReentrantLock REENTRANT_LOCK = new ReentrantLock();
    private static final AtomicLong VERSION = new AtomicLong();
    private boolean success = true;
    private int filesCount;
    private String message = "";

    public SyncStaticResourceRunnable(IOSession session) {
        this.session = session;
    }

    @Override
    public void run() {
        long expectVersion = VERSION.incrementAndGet();
        REENTRANT_LOCK.lock();
        try {
            if (!Objects.equals(VERSION.get(), expectVersion)) {
                return;
            }
            Map<String, Object> map = new HashMap<>();
            map.put("key", "syncTemplate,syncHtml,syncRemoteType");
            Map<String, String> responseMap = (Map<String, String>) session.getResponseSync(ContentType.JSON, map, ActionType.GET_WEBSITE, Map.class);
            if (responseMap == null) {
                markResult(true, 0, "未读取到静态同步配置。");
                return;
            }
            TemplatePath templatePath = session.getResponseSync(ContentType.JSON, new HashMap<>(), ActionType.CURRENT_TEMPLATE, TemplatePath.class);
            BlogRunTime blogRunTime = session.getResponseSync(ContentType.JSON, new HashMap<>(), ActionType.BLOG_RUN_TIME, BlogRunTime.class);
            List<UploadFile> uploadFiles = new ArrayList<>();
            Map<String, String> copyFileInfoMap = new HashMap<>();
            uploadFiles.addAll(SyncFileInfoCacheUtils.templateUploadFiles(blogRunTime, responseMap, templatePath, copyFileInfoMap));
            uploadFiles.addAll(SyncFileInfoCacheUtils.cacheFiles(blogRunTime, responseMap, copyFileInfoMap));
            if (uploadFiles.isEmpty()) {
                markResult(true, 0, "同步已完成，无新变动资源需要推送。");
                return;
            }
            String syncRemoteType = responseMap.get("syncRemoteType");
            if (Objects.isNull(syncRemoteType)) {
                markResult(true, 0, "未配置静态资源远程同步类型。");
                return;
            }
            Map<String, Object> configMapRequest = new HashMap<>();
            configMapRequest.put("key", syncRemoteType);
            Map<String, String> configResponse = (Map<String, String>) session.getResponseSync(ContentType.JSON, configMapRequest, ActionType.GET_WEBSITE, Map.class);
            if (configResponse == null) {
                markResult(true, 0, "未读取到 " + syncRemoteType + " 同步配置。");
                return;
            }
            if (Objects.equals(syncRemoteType, "git")) {
                String gitConfig = configResponse.get(syncRemoteType);
                if (Objects.isNull(gitConfig) || gitConfig.trim().isEmpty()) {
                    markResult(true, 0, "未配置 git 同步信息。");
                    return;
                }
                try (FileManage fileManage = new GitFileManageImpl(gitConfig, uploadFiles, session)) {
                    List<UploadFile> uploadedFiles = fileManage.doSync();
                    if (uploadedFiles.isEmpty()) {
                        markResult(true, 0, "同步已完成，无新变动资源需要推送。");
                        recordSyncHistory(success, filesCount, message);
                        return;
                    }
                    markResult(true, uploadedFiles.size(), "成功推送了 " + uploadedFiles.size() + " 个新增/变更资源。");
                    recordSyncHistory(success, filesCount, message);
                }
            } else {
                markResult(true, 0, "暂不支持 " + syncRemoteType + " 同步类型。");
            }
        } catch (Exception e) {
            LOGGER.warning("Sync error " + e.getMessage());
            markResult(false, 0, "同步失败: " + e.getMessage());
            recordSyncHistory(success, filesCount, message);
        } finally {
            REENTRANT_LOCK.unlock();
        }
    }

    private void markResult(boolean success, int filesCount, String message) {
        this.success = success;
        this.filesCount = filesCount;
        this.message = message;
    }

    private void recordSyncHistory(boolean success, int filesCount, String message) {
        try {
            Map<String, Object> historyRequest = new HashMap<>();
            historyRequest.put("key", "syncHistory");
            Map<String, String> historyResponse = (Map<String, String>) session.getResponseSync(ContentType.JSON, historyRequest, ActionType.GET_WEBSITE, Map.class);
            String syncHistoryJson = historyResponse != null ? historyResponse.get("syncHistory") : null;

            List<Map<String, Object>> historyList;
            if (syncHistoryJson != null && !syncHistoryJson.trim().isEmpty()) {
                historyList = new Gson().fromJson(syncHistoryJson, List.class);
            } else {
                historyList = new ArrayList<>();
            }

            Map<String, Object> record = new HashMap<>();
            record.put("id", UUID.randomUUID().toString());
            record.put("time", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            record.put("success", success);
            record.put("filesCount", filesCount);
            record.put("message", message);

            historyList.add(0, record);
            if (historyList.size() > 15) {
                historyList = historyList.subList(0, 15);
            }

            Map<String, String> saveMap = new HashMap<>();
            saveMap.put("syncHistory", new Gson().toJson(historyList));
            session.sendJsonMsg(saveMap, ActionType.SET_WEBSITE.name(), IdUtil.getInt(), MsgPacketStatus.SEND_REQUEST);
        } catch (Exception e) {
            LOGGER.warning("Failed to record sync history: " + e.getMessage());
        }
    }

    public boolean isSuccess() {
        return success;
    }

    public int getFilesCount() {
        return filesCount;
    }

    public String getMessage() {
        return message;
    }

}
