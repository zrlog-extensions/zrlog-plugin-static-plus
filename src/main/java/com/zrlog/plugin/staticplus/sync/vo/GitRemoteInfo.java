package com.zrlog.plugin.staticplus.sync.vo;

public class GitRemoteInfo {

    private String url;
    private String branch;
    private String password;
    private String username;
    private String gitCommitterEmail;
    private String gitCommitterUsername;
    private String accessBaseUrl;
    private String proxyHttpHost;
    private Integer proxyHttpPort;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAccessBaseUrl() {
        return accessBaseUrl;
    }

    public void setAccessBaseUrl(String accessBaseUrl) {
        this.accessBaseUrl = accessBaseUrl;
    }

    public String getProxyHttpHost() {
        return proxyHttpHost;
    }

    public void setProxyHttpHost(String proxyHttpHost) {
        this.proxyHttpHost = proxyHttpHost;
    }

    public Integer getProxyHttpPort() {
        return proxyHttpPort;
    }

    public void setProxyHttpPort(Integer proxyHttpPort) {
        this.proxyHttpPort = proxyHttpPort;
    }

    public String getGitCommitterEmail() {
        return gitCommitterEmail;
    }

    public void setGitCommitterEmail(String gitCommitterEmail) {
        this.gitCommitterEmail = gitCommitterEmail;
    }

    public String getGitCommitterUsername() {
        return gitCommitterUsername;
    }

    public void setGitCommitterUsername(String gitCommitterUsername) {
        this.gitCommitterUsername = gitCommitterUsername;
    }
}
