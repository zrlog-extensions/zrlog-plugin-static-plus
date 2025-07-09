package com.zrlog.plugin.staticplus.sync;

import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.SystemReader;

public class NoConfigSystemReader extends SystemReader {
    @Override
    public String getHostname() {
        return "localhost";
    }

    @Override
    public String getenv(String variable) {
        return System.getenv(variable);
    }

    @Override
    public String getProperty(String key) {
        return System.getProperty(key);
    }

    @Override
    public FileBasedConfig openSystemConfig(Config parent, FS fs) {
        return new FileBasedConfig(null, fs) {
            @Override
            public void load() {
                // Skip loading system config
            }
        };
    }

    @Override
    public FileBasedConfig openJGitConfig(Config parent, FS fs) {
        return new FileBasedConfig(null, fs) {
            @Override
            public void load() {
                // Do nothing: prevent JGit config from loading
            }
        };
    }

    @Override
    public FileBasedConfig openUserConfig(Config parent, FS fs) {
        return new FileBasedConfig(null, fs) {
            @Override
            public void load() {
                // Skip loading user config
            }
        };
    }

    @Override
    public long getCurrentTime() {
        return System.currentTimeMillis();
    }

    @Override
    public int getTimezone(long when) {
        return 0;
    }
}