package com.zrlog.plugin.staticplus.service;

import com.google.gson.Gson;
import com.zrlog.plugin.IOSession;
import com.zrlog.plugin.api.Capability;
import com.zrlog.plugin.api.IPluginService;
import com.zrlog.plugin.api.Service;
import com.zrlog.plugin.common.LoggerUtil;
import com.zrlog.plugin.common.response.UploadFileResponse;
import com.zrlog.plugin.common.response.UploadFileResponseEntry;
import com.zrlog.plugin.common.vo.UploadFile;
import com.zrlog.plugin.data.codec.ContentType;
import com.zrlog.plugin.data.codec.MsgPacket;
import com.zrlog.plugin.data.codec.MsgPacketStatus;
import com.zrlog.plugin.message.CapabilityInvokeResult;
import com.zrlog.plugin.staticplus.sync.FileManage;
import com.zrlog.plugin.staticplus.sync.GitFileManageImpl;
import com.zrlog.plugin.staticplus.sync.S3FileManageImpl;
import com.zrlog.plugin.type.ActionType;

import java.io.File;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service("uploadService")
@Capability(
        key = "staticPlus.upload",
        type = "service",
        label = "上传到静态资源仓库",
        description = "上传文章附件和生成资源到配置的静态资源仓库。",
        exposure = {"internal"},
        riskLevel = "medium",
        timeoutSeconds = 120
)
public class UploadService implements IPluginService {

    private static final Logger LOGGER = LoggerUtil.getLogger(UploadService.class);

    @Override
    public void handle(final IOSession ioSession, final MsgPacket requestPacket) {
        Map<String, Object> rawRequest = new Gson().fromJson(requestPacket.getDataStr(), Map.class);
        Map<String, Object> request = requestPayload(requestPacket, rawRequest);
        List<String> fileInfoList = fileInfoList(request.get("fileInfo"));
        List<UploadFile> uploadFileList = getUploadFiles(fileInfoList);
        UploadFileResponse uploadFileResponse = upload(ioSession, uploadFileList);
        List<Map<String, Object>> responseList = new ArrayList<>();
        for (UploadFileResponseEntry entry : uploadFileResponse) {
            Map<String, Object> map = new HashMap<>();
            map.put("url", entry.getUrl());
            responseList.add(map);
        }
        if (Objects.equals(ActionType.CAPABILITY_INVOKE.name(), requestPacket.getMethodStr())) {
            CapabilityInvokeResult result = new CapabilityInvokeResult();
            result.setSuccess(true);
            Map<String, Object> data = new HashMap<>();
            data.put("items", responseList);
            result.setData(data);
            ioSession.sendJsonMsg(result, requestPacket.getMethodStr(), requestPacket.getMsgId(), MsgPacketStatus.RESPONSE_SUCCESS);
        } else {
            ioSession.sendMsg(ContentType.JSON, responseList, requestPacket.getMethodStr(), requestPacket.getMsgId(), MsgPacketStatus.RESPONSE_SUCCESS);
        }
    }

    private Map<String, Object> requestPayload(MsgPacket requestPacket, Map<String, Object> rawRequest) {
        if (rawRequest == null) {
            return new HashMap<>();
        }
        Object payload = rawRequest.get("payload");
        if (Objects.equals(ActionType.CAPABILITY_INVOKE.name(), requestPacket.getMethodStr()) && payload instanceof Map) {
            return (Map<String, Object>) payload;
        }
        return rawRequest;
    }

    private List<String> fileInfoList(Object rawFileInfo) {
        List<String> fileInfoList = new ArrayList<>();
        if (rawFileInfo instanceof List) {
            for (Object item : (List) rawFileInfo) {
                if (item != null) {
                    fileInfoList.add(item.toString());
                }
            }
        } else if (rawFileInfo != null) {
            fileInfoList.add(rawFileInfo.toString());
        }
        return fileInfoList;
    }

    private static List<UploadFile> getUploadFiles(List<String> fileInfoList) {
        List<UploadFile> uploadFileList = new ArrayList<>();
        for (String fileInfo : fileInfoList) {
            UploadFile uploadFile = new UploadFile();
            String[] infos = fileInfo.split(",");
            uploadFile.setFile(new File(infos[0]));
            String fileKey = infos[1];
            if (fileKey.startsWith("/")) {
                uploadFile.setFileKey(fileKey.substring(1));
            } else {
                uploadFile.setFileKey(fileKey);
            }
            if (infos.length > 2) {
                uploadFile.setRefresh(Boolean.parseBoolean(infos[2]));
            }
            uploadFileList.add(uploadFile);
        }
        return uploadFileList;
    }

    private UploadFileResponse convertByUploadFileList(List<UploadFile> uploadFileList) {
        UploadFileResponse uploadFileResponse = new UploadFileResponse();
        uploadFileList.forEach(e -> {
            UploadFileResponseEntry responseEntry = new UploadFileResponseEntry();
            responseEntry.setUrl(e.getFileKey());
            uploadFileResponse.add(responseEntry);
        });
        return uploadFileResponse;
    }

    public UploadFileResponse upload(IOSession session, final List<UploadFile> uploadFileList) {
        final UploadFileResponse response = new UploadFileResponse();
        if (uploadFileList == null || uploadFileList.isEmpty()) {
            return response;
        }
        long startTime = System.currentTimeMillis();
        Map<String, String> responseMap = (Map<String, String>) session.getResponseSync(ContentType.JSON, Map.of("key", "syncRemoteType"), ActionType.GET_WEBSITE, Map.class);
        if (Objects.isNull(responseMap) || Objects.isNull(responseMap.get("syncRemoteType"))) {
            return convertByUploadFileList(uploadFileList);
        }
        String syncType = responseMap.get("syncRemoteType");
        Map<String, String> configMap = (Map<String, String>) session.getResponseSync(ContentType.JSON, Map.of("key", syncType), ActionType.GET_WEBSITE, Map.class);
        if (Objects.isNull(configMap)) {
            return convertByUploadFileList(uploadFileList);
        }
        String gitConfig = configMap.get(syncType);
        if (Objects.isNull(gitConfig) || gitConfig.trim().isEmpty()) {
            return convertByUploadFileList(uploadFileList);
        }
        FileManage bucketManageAPI = null;
        if (Objects.equals("git", responseMap.get("syncRemoteType"))) {
            bucketManageAPI = new GitFileManageImpl(configMap.get(syncType), new ArrayList<>(), session);
        } else if (Objects.equals("s3", responseMap.get("syncRemoteType"))) {
            bucketManageAPI = new S3FileManageImpl(configMap.get(syncType), new ArrayList<>(), session);
        }
        if (Objects.isNull(bucketManageAPI)) {
            return convertByUploadFileList(uploadFileList);
        }
        try {
            for (UploadFile uploadFile : uploadFileList) {
                UploadFileResponseEntry entry = new UploadFileResponseEntry();
                try {
                    boolean supportHttps = responseMap.get("supportHttps") != null && "on".equalsIgnoreCase(responseMap.get("supportHttps"));
                    entry.setUrl(bucketManageAPI.create(uploadFile.getFile(), uploadFile.getFileKey(), true, supportHttps));
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "upload error " + e.getMessage());
                    entry.setUrl(uploadFile.getFileKey());
                }
                response.add(entry);
            }
            LOGGER.info("Upload " + uploadFileList.size() + " files finish use time " + (System.currentTimeMillis() - startTime) + "ms");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "upload error " + e.getMessage());
        } finally {
            try {
                bucketManageAPI.close();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "upload error close fileManage error " + e.getMessage());
            }
        }
        return response;
    }
}
