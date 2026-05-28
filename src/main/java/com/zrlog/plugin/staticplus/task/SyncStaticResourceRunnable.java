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

    private final String cacheKeyMapKey = "cacheMap";
    private final ReentrantLock reentrantLock = new ReentrantLock();
    private final AtomicLong version = new AtomicLong();

    public SyncStaticResourceRunnable(IOSession session) {
        this.session = session;
    }

    private Map<String, String> preloadCache(Map<String, String> responseMap) {
        String cacheMapStr = responseMap.get(cacheKeyMapKey);
        if (Objects.nonNull(cacheMapStr) && !cacheMapStr.isEmpty()) {
            return new Gson().fromJson(cacheMapStr, Map.class);
        }
        return new HashMap<>();
    }

    private void saveCacheToDb(Map<String, String> fileInfoCacheMap) {
        Map<String, String> newCacheMap = new TreeMap<>();
        newCacheMap.put(cacheKeyMapKey, new Gson().toJson(fileInfoCacheMap));
        session.sendJsonMsg(newCacheMap, ActionType.SET_WEBSITE.name(), IdUtil.getInt(), MsgPacketStatus.SEND_REQUEST);
    }

    @Override
    public void run() {
        long expectVersion = version.incrementAndGet();
        reentrantLock.lock();
        try {
            if (!Objects.equals(version.get(), expectVersion)) {
                return;
            }
            Map<String, Object> map = new HashMap<>();
            map.put("key", "syncTemplate,syncHtml,syncRemoteType," + cacheKeyMapKey);
            Map<String, String> responseMap = (Map<String, String>) session.getResponseSync(ContentType.JSON, map, ActionType.GET_WEBSITE, Map.class);
            //reload cache
            Map<String, String> fileInfoCacheMap = preloadCache(responseMap);
            TemplatePath templatePath = session.getResponseSync(ContentType.JSON, new HashMap<>(), ActionType.CURRENT_TEMPLATE, TemplatePath.class);
            BlogRunTime blogRunTime = session.getResponseSync(ContentType.JSON, new HashMap<>(), ActionType.BLOG_RUN_TIME, BlogRunTime.class);
            List<UploadFile> uploadFiles = new ArrayList<>();
            Map<String, String> copyFileInfoMap = new HashMap<>(fileInfoCacheMap);
            uploadFiles.addAll(SyncFileInfoCacheUtils.templateUploadFiles(blogRunTime, responseMap, templatePath, copyFileInfoMap));
            uploadFiles.addAll(SyncFileInfoCacheUtils.cacheFiles(blogRunTime, responseMap, copyFileInfoMap));
            if (uploadFiles.isEmpty()) {
                return;
            }
            String syncRemoteType = responseMap.get("syncRemoteType");
            if (Objects.isNull(syncRemoteType)) {
                return;
            }
            Map<String, Object> configMapRequest = new HashMap<>();
            configMapRequest.put("key", syncRemoteType);
            Map<String, String> configResponse = (Map<String, String>) session.getResponseSync(ContentType.JSON, configMapRequest, ActionType.GET_WEBSITE, Map.class);
            if (Objects.equals(syncRemoteType, "git")) {
                String gitConfig = configResponse.get(syncRemoteType);
                if (Objects.isNull(gitConfig) || gitConfig.trim().isEmpty()) {
                    return;
                }
                try (FileManage fileManage = new GitFileManageImpl(gitConfig, uploadFiles, session)) {
                    List<UploadFile> uploadedFiles = fileManage.doSync();
                    if (uploadedFiles.isEmpty()) {
                        recordSyncHistory(true, 0, "同步已完成，无新变动资源需要推送。");
                        return;
                    }
                    for (UploadFile uploadFile : uploadedFiles) {
                        String key = uploadFile.getFile().toString();
                        fileInfoCacheMap.put(key, copyFileInfoMap.get(key));
                    }
                    saveCacheToDb(fileInfoCacheMap);
                    recordSyncHistory(true, uploadedFiles.size(), "成功推送了 " + uploadedFiles.size() + " 个新增/变更资源。");
                }
            }
        } catch (Exception e) {
            LOGGER.warning("Sync error " + e.getMessage());
            recordSyncHistory(false, 0, "同步失败: " + e.getMessage());
        } finally {
            reentrantLock.unlock();
        }
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


}
