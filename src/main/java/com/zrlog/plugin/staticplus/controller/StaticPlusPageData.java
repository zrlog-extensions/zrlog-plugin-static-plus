package com.zrlog.plugin.staticplus.controller;

public class StaticPlusPageData {

    private String syncTemplate;
    private String syncHtml;
    private String syncAttached;
    private String syncRemoteType;
    private String git;
    private String s3;
    private String syncHistory;
    private String adminColorPrimary;

    public void normalize() {
        if (syncRemoteType == null || syncRemoteType.trim().isEmpty()) {
            syncRemoteType = "git";
        }
    }

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

    public String getSyncHistory() {
        return syncHistory;
    }

    public void setSyncHistory(String syncHistory) {
        this.syncHistory = syncHistory;
    }

    public String getAdminColorPrimary() {
        return adminColorPrimary;
    }

    public void setAdminColorPrimary(String adminColorPrimary) {
        this.adminColorPrimary = adminColorPrimary;
    }
}
