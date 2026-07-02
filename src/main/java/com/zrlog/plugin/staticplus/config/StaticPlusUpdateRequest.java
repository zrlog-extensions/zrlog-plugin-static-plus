package com.zrlog.plugin.staticplus.config;

public class StaticPlusUpdateRequest {

    private String syncTemplate;
    private String syncHtml;
    private String syncAttached;
    private String syncRemoteType;
    private String git;
    private String s3;

    public String getSyncTemplate() {
        return syncTemplate;
    }

    public void setSyncTemplate(String syncTemplate) {
        this.syncTemplate = syncTemplate;
    }

    public String getSyncHtml() {
        return syncHtml;
    }

    public void setSyncHtml(String syncHtml) {
        this.syncHtml = syncHtml;
    }

    public String getSyncAttached() {
        return syncAttached;
    }

    public void setSyncAttached(String syncAttached) {
        this.syncAttached = syncAttached;
    }

    public String getSyncRemoteType() {
        return syncRemoteType;
    }

    public void setSyncRemoteType(String syncRemoteType) {
        this.syncRemoteType = syncRemoteType;
    }

    public String getGit() {
        return git;
    }

    public void setGit(String git) {
        this.git = git;
    }

    public String getS3() {
        return s3;
    }

    public void setS3(String s3) {
        this.s3 = s3;
    }
}
