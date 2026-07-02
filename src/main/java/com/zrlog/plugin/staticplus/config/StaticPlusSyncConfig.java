package com.zrlog.plugin.staticplus.config;

public class StaticPlusSyncConfig {

    private String syncTemplate;
    private String syncHtml;
    private String syncRemoteType;
    private String supportHttps;

    public boolean isSyncTemplateEnabled() {
        return "on".equals(syncTemplate);
    }

    public boolean isSyncHtmlEnabled() {
        return "on".equals(syncHtml);
    }

    public boolean isSupportHttpsEnabled() {
        return supportHttps != null && "on".equalsIgnoreCase(supportHttps);
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

    public String getSyncRemoteType() {
        return syncRemoteType;
    }

    public void setSyncRemoteType(String syncRemoteType) {
        this.syncRemoteType = syncRemoteType;
    }

    public String getSupportHttps() {
        return supportHttps;
    }

    public void setSupportHttps(String supportHttps) {
        this.supportHttps = supportHttps;
    }
}
