package com.zrlog.plugin.staticplus.sync;

import com.zrlog.plugin.common.LoggerUtil;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Objects;
import java.util.logging.Logger;

public class SyncUtils {

    private static final Logger LOGGER = LoggerUtil.getLogger(SyncUtils.class);
    private static final HttpClient httpClient = HttpClient.newBuilder().build();

    public static boolean checkFileSyncs(String checkUrl, String expectContent) {
        try {
            for (int i = 0; i < 40; i++) {
                String gitBuildJson = checkUrl + "?_" + System.currentTimeMillis();
                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
                        .uri(URI.create(gitBuildJson))
                        .build();
                HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
                if (Objects.isNull(httpResponse.body())) {
                    return true;
                }
                if (Objects.equals(httpResponse.body(), expectContent)) {
                    LOGGER.info("Files sync success");
                    return true;
                }
                Thread.sleep(1000);
            }
            LOGGER.warning("Git push success,but check files timeout");
            return true;
        } catch (Exception e) {
            LOGGER.warning("Check file sync error " + e.getMessage());
            return false;
        }
    }
}
