package com.zrlog.plugin.staticplus.task;

public class SyncHistoryRecord {

    private String id;
    private String time;
    private boolean success;
    private int filesCount;
    private String message;
    private long uploadTimeMs;
    private String syncRemoteType;

    public SyncHistoryRecord() {
    }

    public static SyncHistoryRecord create(String id, String time, boolean success, int filesCount, String message,
                                           long uploadTimeMs, String syncRemoteType) {
        SyncHistoryRecord record = new SyncHistoryRecord();
        record.setId(id);
        record.setTime(time);
        record.setSuccess(success);
        record.setFilesCount(filesCount);
        record.setMessage(message);
        record.setUploadTimeMs(uploadTimeMs);
        record.setSyncRemoteType(syncRemoteType);
        return record;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public int getFilesCount() {
        return filesCount;
    }

    public void setFilesCount(int filesCount) {
        this.filesCount = filesCount;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getUploadTimeMs() {
        return uploadTimeMs;
    }

    public void setUploadTimeMs(long uploadTimeMs) {
        this.uploadTimeMs = uploadTimeMs;
    }

    public String getSyncRemoteType() {
        return syncRemoteType;
    }

    public void setSyncRemoteType(String syncRemoteType) {
        this.syncRemoteType = syncRemoteType;
    }
}
