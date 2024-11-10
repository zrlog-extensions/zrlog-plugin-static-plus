package com.zrlog.plugin.staticplus.utils;

import com.zrlog.plugin.common.FileUtils;
import com.zrlog.plugin.common.LoggerUtil;
import com.zrlog.plugin.common.SecurityUtils;
import com.zrlog.plugin.common.model.BlogRunTime;
import com.zrlog.plugin.common.model.TemplatePath;
import com.zrlog.plugin.common.vo.UploadFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SyncFileInfoCacheUtils {

    private static final Logger LOGGER = LoggerUtil.getLogger(SyncFileInfoCacheUtils.class);


    public static List<UploadFile> cacheFiles(BlogRunTime blogRunTime, Map<String, String> responseMap, Map<String, String> fileInfoCacheMap) {
        if (!"on".equals(responseMap.get("syncHtml"))) {
            return new ArrayList<>();
        }
        String cacheFolder = new File(blogRunTime.getPath()).getParent() + "/cache/zh_CN";
        return getUploadFiles(cacheFolder, cacheFolder, fileInfoCacheMap);
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

    private static void fillToUploadFiles(List<File> files, String startPath, List<UploadFile> uploadFiles, Map<String, String> fileInfoCacheMap) {
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
                fillToUploadFiles(Arrays.asList(fs), startPath, uploadFiles, fileInfoCacheMap);
            }
        }
    }

    private static List<UploadFile> getUploadFiles(String cacheFolder, String startPath, Map<String, String> fileInfoCacheMap) {
        File cacheFile = new File(cacheFolder);
        List<UploadFile> uploadFiles = new ArrayList<>();
        if (cacheFile.exists()) {
            File[] fs = cacheFile.listFiles();
            fillToUploadFiles(Arrays.asList(Objects.requireNonNull(fs)), startPath, uploadFiles, fileInfoCacheMap);
        }
        return uploadFiles;
    }

    public static List<UploadFile> templateUploadFiles(BlogRunTime blogRunTime, Map<String, String> responseMap, TemplatePath templatePath, Map<String, String> fileInfoCacheMap) {
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
            fillToUploadFiles(fileList, blogRunTime.getPath(), uploadFiles, fileInfoCacheMap);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "", e);
        }
        return uploadFiles;
    }

}
