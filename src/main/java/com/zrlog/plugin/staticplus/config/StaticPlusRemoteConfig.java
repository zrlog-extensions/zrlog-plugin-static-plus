package com.zrlog.plugin.staticplus.config;

import java.util.Objects;

public class StaticPlusRemoteConfig {

    private String git;
    private String s3;

    public String config(String syncRemoteType) {
        if (Objects.equals(syncRemoteType, "git")) {
            return git;
        }
        if (Objects.equals(syncRemoteType, "s3")) {
            return s3;
        }
        return null;
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
