package com.zrlog.plugin.staticplus.sync;

import com.zrlog.plugin.IOSession;
import com.zrlog.plugin.client.HttpClientUtils;
import com.zrlog.plugin.common.LoggerUtil;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

public class SyncUtils {

    private static final Logger LOGGER = LoggerUtil.getLogger(SyncUtils.class);

    public static boolean checkFileSyncs(String checkUrl, String expectContent, IOSession session) {
        if (Objects.isNull(session)) {
            LOGGER.warning("Check file sync error, missing session");
            return false;
        }
        try {
            for (int i = 0; i < 40; i++) {
                String gitBuildJson = checkUrl + "?_" + System.currentTimeMillis();
                String body = HttpClientUtils.sendGetRequest(gitBuildJson, String.class, Map.of("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"), session, Duration.ofSeconds(10));
                if (Objects.equals(body, expectContent)) {
                    return true;
                }
                Thread.sleep(1000);
            }
            LOGGER.warning("Git push success, but check files timeout");
            return false;
        } catch (Exception e) {
            LOGGER.warning("Check file sync error " + e.getMessage());
            return false;
        }
    }
}
