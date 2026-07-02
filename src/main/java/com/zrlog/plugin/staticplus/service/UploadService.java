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
import com.zrlog.plugin.staticplus.config.StaticPlusRemoteConfig;
import com.zrlog.plugin.staticplus.config.StaticPlusSyncConfig;
import com.zrlog.plugin.staticplus.config.WebsiteKeyRequest;
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
        UploadServiceRequest rawRequest = new Gson().fromJson(requestPacket.getDataStr(), UploadServiceRequest.class);
        UploadServiceRequest request = requestPayload(requestPacket, rawRequest);
        List<String> fileInfoList = request.fileInfoList();
        List<UploadFile> uploadFileList = getUploadFiles(fileInfoList);
        UploadFileResponse uploadFileResponse = upload(ioSession, uploadFileList);
        if (Objects.equals(ActionType.CAPABILITY_INVOKE.name(), requestPacket.getMethodStr())) {
            CapabilityInvokeResult result = new CapabilityInvokeResult();
            result.setSuccess(true);
            Map<String, Object> data = new HashMap<>();
            data.put("items", uploadFileResponse);
            result.setData(data);
            ioSession.sendJsonMsg(result, requestPacket.getMethodStr(), requestPacket.getMsgId(), MsgPacketStatus.RESPONSE_SUCCESS);
        } else {
            ioSession.sendMsg(ContentType.JSON, uploadFileResponse, requestPacket.getMethodStr(), requestPacket.getMsgId(), MsgPacketStatus.RESPONSE_SUCCESS);
        }
    }

    private UploadServiceRequest requestPayload(MsgPacket requestPacket, UploadServiceRequest rawRequest) {
        if (rawRequest == null) {
            return new UploadServiceRequest();
        }
        if (Objects.equals(ActionType.CAPABILITY_INVOKE.name(), requestPacket.getMethodStr())
                && rawRequest.getPayload() != null) {
            return rawRequest.getPayload();
        }
        return rawRequest;
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
        StaticPlusSyncConfig syncConfig = session.getResponseSync(ContentType.JSON, WebsiteKeyRequest.of("syncRemoteType,supportHttps"),
                ActionType.GET_WEBSITE, StaticPlusSyncConfig.class);
        if (Objects.isNull(syncConfig) || Objects.isNull(syncConfig.getSyncRemoteType())) {
            return convertByUploadFileList(uploadFileList);
        }
        String syncType = syncConfig.getSyncRemoteType();
        StaticPlusRemoteConfig remoteConfig = session.getResponseSync(ContentType.JSON, WebsiteKeyRequest.of(syncType),
                ActionType.GET_WEBSITE, StaticPlusRemoteConfig.class);
        if (Objects.isNull(remoteConfig)) {
            return convertByUploadFileList(uploadFileList);
        }
        String configJson = remoteConfig.config(syncType);
        if (Objects.isNull(configJson) || configJson.trim().isEmpty()) {
            return convertByUploadFileList(uploadFileList);
        }
        FileManage bucketManageAPI = null;
        if (Objects.equals("git", syncType)) {
            bucketManageAPI = new GitFileManageImpl(configJson, new ArrayList<>(), session);
        } else if (Objects.equals("s3", syncType)) {
            bucketManageAPI = new S3FileManageImpl(configJson, new ArrayList<>(), session);
        }
        if (Objects.isNull(bucketManageAPI)) {
            return convertByUploadFileList(uploadFileList);
        }
        try {
            for (UploadFile uploadFile : uploadFileList) {
                UploadFileResponseEntry entry = new UploadFileResponseEntry();
                try {
                    entry.setUrl(bucketManageAPI.create(uploadFile.getFile(), uploadFile.getFileKey(), true, syncConfig.isSupportHttpsEnabled()));
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
