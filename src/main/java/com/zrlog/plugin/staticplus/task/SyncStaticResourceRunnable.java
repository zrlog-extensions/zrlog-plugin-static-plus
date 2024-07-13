package com.zrlog.plugin.staticplus.task;

import com.google.gson.Gson;
import com.zrlog.plugin.IOSession;
import com.zrlog.plugin.common.FileUtils;
import com.zrlog.plugin.common.IdUtil;
import com.zrlog.plugin.common.LoggerUtil;
import com.zrlog.plugin.common.SecurityUtils;
import com.zrlog.plugin.common.model.BlogRunTime;
import com.zrlog.plugin.common.model.TemplatePath;
import com.zrlog.plugin.common.vo.UploadFile;
import com.zrlog.plugin.data.codec.ContentType;
import com.zrlog.plugin.data.codec.MsgPacketStatus;
import com.zrlog.plugin.staticplus.sync.FileManage;
import com.zrlog.plugin.staticplus.sync.GitFileManageImpl;
import com.zrlog.plugin.type.ActionType;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SyncStaticResourceRunnable implements Runnable {

    private static final Logger LOGGER = LoggerUtil.getLogger(SyncStaticResourceRunnable.class);

    private final IOSession session;

    private final Map<String, Object> fileInfoCacheMap = new TreeMap<>();
    private final String cacheKeyMapKey = "cacheMap";
    private final ReentrantLock reentrantLock = new ReentrantLock();

    public SyncStaticResourceRunnable(IOSession session) {
        this.session = session;
    }

    private List<UploadFile> cacheFiles(BlogRunTime blogRunTime, Map<String, String> responseMap) {
        if (!"on".equals(responseMap.get("syncHtml"))) {
            return new ArrayList<>();
        }
        String cacheFolder = new File(blogRunTime.getPath()).getParent() + "/cache/zh_CN";
        return getUploadFiles(cacheFolder, cacheFolder);
    }

    private List<UploadFile> getUploadFiles(String cacheFolder, String startPath) {
        File cacheFile = new File(cacheFolder);
        List<UploadFile> uploadFiles = new ArrayList<>();
        if (cacheFile.exists()) {
            File[] fs = cacheFile.listFiles();
            fillToUploadFiles(Arrays.asList(Objects.requireNonNull(fs)), startPath, uploadFiles);
        }
        return uploadFiles;
    }

    private List<UploadFile> templateUploadFiles(BlogRunTime blogRunTime, Map<String, String> responseMap, TemplatePath templatePath) {
        if (!"on".equals(responseMap.get("syncTemplate"))) {
            return new ArrayList<>();
        }
        File templateFilePath = new File(blogRunTime.getPath() + templatePath.getValue());
        if (!templateFilePath.isDirectory()) {
            if (Objects.equals(templatePath.getValue(), "/include/templates/default")) {
                return new ArrayList<>();
            }
            LOGGER.log(Level.INFO, "Template path not directory " + templateFilePath);
            return new ArrayList<>();
        }
        File propertiesFile = new File(templateFilePath + "/template.properties");
        if (!propertiesFile.exists()) {
            LOGGER.log(Level.SEVERE, "Template properties not find " + propertiesFile);
            return new ArrayList<>();
        }
        List<UploadFile> uploadFiles = new ArrayList<>();
        Properties prop = new Properties();

        try (FileInputStream fileInputStream = new FileInputStream(propertiesFile)) {
            prop.load(fileInputStream);
            String staticResource = (String) prop.get("staticResource");
            List<File> fileList = new ArrayList<>(getStaticFolderFiles(staticResource, templateFilePath, blogRunTime));
            fillToUploadFiles(fileList, blogRunTime.getPath(), uploadFiles);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "", e);
        }
        return uploadFiles;
    }

    private void preloadCache(Map<String, String> responseMap) {
        String cacheMapStr = responseMap.get(cacheKeyMapKey);
        if (Objects.nonNull(cacheMapStr) && !cacheMapStr.isEmpty()) {
            fileInfoCacheMap.putAll(new Gson().fromJson(cacheMapStr, Map.class));
        }
    }

    private void saveCacheToDb() {
        Map<String, String> newCacheMap = new TreeMap<>();
        newCacheMap.put(cacheKeyMapKey, new Gson().toJson(fileInfoCacheMap));
        session.sendJsonMsg(newCacheMap, ActionType.SET_WEBSITE.name(), IdUtil.getInt(), MsgPacketStatus.SEND_REQUEST);
    }

    @Override
    public void run() {
        reentrantLock.lock();
        try {
            Map<String, Object> map = new HashMap<>();
            map.put("key", "syncTemplate,syncHtml,syncRemoteType," + cacheKeyMapKey);
            Map<String, String> responseMap = (Map<String, String>) session.getResponseSync(ContentType.JSON, map, ActionType.GET_WEBSITE, Map.class);
            //reload cache
            preloadCache(responseMap);
            TemplatePath templatePath = session.getResponseSync(ContentType.JSON, new HashMap<>(), ActionType.CURRENT_TEMPLATE, TemplatePath.class);
            BlogRunTime blogRunTime = session.getResponseSync(ContentType.JSON, new HashMap<>(), ActionType.BLOG_RUN_TIME, BlogRunTime.class);
            List<UploadFile> uploadFiles = new ArrayList<>();
            uploadFiles.addAll(templateUploadFiles(blogRunTime, responseMap, templatePath));
            uploadFiles.addAll(cacheFiles(blogRunTime, responseMap));
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
            boolean uploaded = false;
            if (Objects.equals(syncRemoteType, "git")) {
                String gitConfig = configResponse.get(syncRemoteType);
                if (Objects.isNull(gitConfig) || gitConfig.trim().isEmpty()) {
                    return;
                }
                try (FileManage fileManage = new GitFileManageImpl(gitConfig, uploadFiles)) {
                    uploaded = fileManage.doSync();
                }
            }
            if (uploaded) {
                saveCacheToDb();
            } else {
                uploadFiles.forEach(e -> {
                    fileInfoCacheMap.remove(e.getFile().toString());
                });
            }
        } catch (Exception e) {
            LOGGER.warning("Sync error " + e.getMessage());
        } finally {
            reentrantLock.unlock();
        }
    }

    private static List<File> getStaticFolderFiles(String staticResource, File templateFilePath, BlogRunTime blogRunTime) {
        List<File> fileList = new ArrayList<>();
        if (staticResource != null && !staticResource.isEmpty()) {
            String[] staticFileArr = staticResource.split(",");
            for (String sFile : staticFileArr) {
                fileList.add(new File(templateFilePath + "/" + sFile));
            }
        }
        return fileList;
    }

    private void fillToUploadFiles(List<File> files, String startPath, List<UploadFile> uploadFiles) {
        List<File> fullFileList = new ArrayList<>();
        for (File file : files) {
            FileUtils.getAllFiles(file.toString(), fullFileList);
        }
        if (!startPath.endsWith("/")) {
            startPath = startPath + "/";
        }
        for (File file : fullFileList) {
            if (!file.exists()) {
                continue;
            }
            if (file.isFile()) {
                String md5 = SecurityUtils.md5ByFile(file);
                if (fileInfoCacheMap.get(file.toString()) == null || !Objects.equals(fileInfoCacheMap.get(file.toString()), md5)) {
                    UploadFile uploadFile = new UploadFile();
                    uploadFile.setFile(file);
                    uploadFile.setRefresh(true);
                    String key = file.toString().substring(startPath.length());
                    uploadFile.setFileKey(key);
                    uploadFiles.add(uploadFile);
                    fileInfoCacheMap.put(file.toString(), md5);
                }
            } else if (file.isDirectory()) {
                File[] fs = file.listFiles();
                if (fs.length == 0) {
                    continue;
                }
                fillToUploadFiles(Arrays.asList(fs), startPath, uploadFiles);
            }
        }
    }

}
