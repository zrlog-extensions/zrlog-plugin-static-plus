package com.zrlog.plugin.staticplus.sync;

import com.google.gson.Gson;
import com.zrlog.plugin.IOSession;
import com.zrlog.plugin.common.LoggerUtil;
import com.zrlog.plugin.common.vo.UploadFile;
import com.zrlog.plugin.staticplus.sync.vo.S3RemoteInfo;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class S3FileManageImpl implements FileManage {

    private static final Logger LOGGER = LoggerUtil.getLogger(S3FileManageImpl.class);

    private static final int IO_BUFFER_SIZE = 8192;
    private static final String DEFAULT_REGION = "us-east-1";

    private static final String X_AMZ_CONTENT_SHA256 = "x-amz-content-sha256";
    private static final String X_AMZ_SECURITY_TOKEN = "x-amz-security-token";

    private final S3RemoteInfo s3RemoteInfo;
    private final List<UploadFile> syncFiles;
    private final HttpClient httpClient;
    private final AwsSigV4Signer signer = new AwsSigV4Signer();

    public S3FileManageImpl(String configJsonStr, List<UploadFile> syncFiles, IOSession session) {
        this.s3RemoteInfo = new Gson().fromJson(configJsonStr, S3RemoteInfo.class);
        this.syncFiles = syncFiles == null ? new ArrayList<>() : syncFiles;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public List<UploadFile> doSync() {
        return doSyncByUploadFiles(syncFiles);
    }

    private List<UploadFile> doSyncByUploadFiles(List<UploadFile> files) {
        if (Objects.isNull(files) || files.isEmpty() || !isConfigReady()) {
            return new ArrayList<>();
        }
        List<UploadFile> uploadedFiles = new ArrayList<>();
        for (UploadFile uploadFile : files) {
            if (Objects.isNull(uploadFile) || Objects.isNull(uploadFile.getFile()) || !uploadFile.getFile().exists()) {
                continue;
            }
            try {
                create(uploadFile.getFile(), uploadFile.getFileKey(), true, true);
                uploadedFiles.add(uploadFile);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "upload file failed, key=" + uploadFile.getFileKey(), e);
            }
        }
        return uploadedFiles;
    }

    @Override
    public String create(File file, String key, boolean deleteRepeat, boolean supportHttps) throws Exception {
        if (Objects.isNull(file) || !file.exists()) {
            return key;
        }
        if (!isConfigReady()) {
            throw new IllegalStateException("S3 config is incomplete");
        }

        String objectKey = normalizeKey(key);
        URI endpointUri = getEndpointUri();
        URI objectUri = buildObjectUri(endpointUri, objectKey);
        String objectKeyEncoded = AwsSigV4Signer.uriEncode(objectKey, false);
        String payloadSha256 = sha256Hex(file);
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(X_AMZ_CONTENT_SHA256, payloadSha256);
        if (!isBlank(s3RemoteInfo.getSessionToken())) {
            headers.put(X_AMZ_SECURITY_TOKEN, s3RemoteInfo.getSessionToken());
        }
        String contentType = detectContentType(file.getName());
        if (!isBlank(contentType)) {
            headers.put("Content-Type", contentType);
        }

        Map<String, String> signedHeaders = signer.sign(
                "PUT",
                objectUri,
                headers,
                payloadSha256,
                "s3",
                getRegion(),
                s3RemoteInfo.getAccessKey(),
                s3RemoteInfo.getSecretKey(),
                s3RemoteInfo.getSessionToken()
        );

        HttpRequest.Builder builder = HttpRequest.newBuilder(objectUri)
                .timeout(Duration.ofMinutes(10))
                .PUT(HttpRequest.BodyPublishers.ofFile(file.toPath()));
        applySignedHeaders(builder, signedHeaders);
        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        if (!isSuccess(response.statusCode())) {
            throw new IllegalStateException("S3 put failed, status=" + response.statusCode() + ", body="
                    + trimBody(response.body()));
        }

        return buildPublicUrl(endpointUri, objectKeyEncoded, supportHttps);
    }

    @Override
    public void close() {
    }

    private boolean isConfigReady() {
        return !isBlank(s3RemoteInfo.getAccessKey())
                && !isBlank(s3RemoteInfo.getSecretKey())
                && !isBlank(s3RemoteInfo.getBucket());
    }

    private String getRegion() {
        return isBlank(s3RemoteInfo.getRegion()) ? DEFAULT_REGION : s3RemoteInfo.getRegion().trim();
    }

    private URI getEndpointUri() {
        String endpoint = isBlank(s3RemoteInfo.getEndpoint()) ? defaultEndpoint(getRegion()) : s3RemoteInfo.getEndpoint().trim();
        if (!endpoint.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*")) {
            endpoint = "https://" + endpoint;
        }
        return URI.create(endpoint);
    }

    private URI buildObjectUri(URI endpointUri, String objectKey) {
        String encodedObjectKey = AwsSigV4Signer.uriEncode(normalizeKey(objectKey), false);
        String objectPath = buildObjectPath(endpointUri, encodedObjectKey);
        String host = shouldUsePathStyle() ? endpointUri.getHost() : s3RemoteInfo.getBucket() + "." + endpointUri.getHost();
        StringBuilder sb = new StringBuilder();
        sb.append(endpointUri.getScheme()).append("://").append(host);
        if (endpointUri.getPort() > 0) {
            sb.append(":").append(endpointUri.getPort());
        }
        sb.append(objectPath);
        return URI.create(sb.toString());
    }

    private String buildObjectPath(URI endpointUri, String encodedObjectKey) {
        String path = trimSlash(endpointUri.getPath());
        StringBuilder sb = new StringBuilder();
        if (!path.isEmpty()) {
            sb.append("/").append(path);
        }
        if (shouldUsePathStyle()) {
            sb.append("/").append(s3RemoteInfo.getBucket());
        }
        sb.append("/").append(encodedObjectKey);
        return sb.toString();
    }

    private String buildPublicUrl(URI endpointUri, String encodedObjectKey, boolean supportHttps) {
        String publicBaseUrl = isBlank(s3RemoteInfo.getBaseUrl()) ? null : trimSlash(s3RemoteInfo.getBaseUrl());
        if (publicBaseUrl != null) {
            return publicBaseUrl + "/" + encodedObjectKey;
        }
        String scheme = supportHttps ? "https" : "http";
        String host = shouldUsePathStyle() ? endpointUri.getHost() : s3RemoteInfo.getBucket() + "." + endpointUri.getHost();
        if (endpointUri.getPort() > 0) {
            host = host + ":" + endpointUri.getPort();
        }
        String objectPath = buildObjectPath(endpointUri, encodedObjectKey);
        if (shouldUsePathStyle()) {
            return scheme + "://" + host + objectPath;
        }
        return scheme + "://" + host + "/"
                + trimSlash(objectPath);
    }

    private void applySignedHeaders(HttpRequest.Builder builder, Map<String, String> headers) {
        headers.forEach((k, v) -> {
            if ("Host".equalsIgnoreCase(k)) {
                return;
            }
            builder.header(k, v);
        });
    }

    private String trimBody(String body) {
        if (isBlank(body)) {
            return "";
        }
        return body.length() > 500 ? body.substring(0, 500) : body;
    }

    private boolean isSuccess(int statusCode) {
        return statusCode >= 200 && statusCode < 300;
    }

    private boolean shouldUsePathStyle() {
        return Objects.equals(Boolean.TRUE, s3RemoteInfo.getPathStyle());
    }

    private String normalizeKey(String key) {
        if (isBlank(key)) {
            return "";
        }
        String normalized = key.trim();
        return normalized.startsWith("/") ? normalized.substring(1) : normalized;
    }

    private String trimSlash(String value) {
        if (isBlank(value)) {
            return "";
        }
        String result = value.trim();
        while (result.startsWith("/")) {
            result = result.substring(1);
        }
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private String detectContentType(String name) {
        if (isBlank(name)) {
            return "application/octet-stream";
        }
        String lower = name.toLowerCase();
        if (lower.endsWith(".css")) {
            return "text/css";
        }
        if (lower.endsWith(".js")) {
            return "application/javascript";
        }
        if (lower.endsWith(".html") || lower.endsWith(".htm")) {
            return "text/html";
        }
        if (lower.endsWith(".json")) {
            return "application/json";
        }
        if (lower.endsWith(".xml")) {
            return "application/xml";
        }
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (lower.endsWith(".gif")) {
            return "image/gif";
        }
        if (lower.endsWith(".svg")) {
            return "image/svg+xml";
        }
        if (lower.endsWith(".ico")) {
            return "image/x-icon";
        }
        if (lower.endsWith(".woff") || lower.endsWith(".woff2")) {
            return "font/woff2";
        }
        if (lower.endsWith(".ttf")) {
            return "font/ttf";
        }
        if (lower.endsWith(".mp4")) {
            return "video/mp4";
        }
        return "application/octet-stream";
    }

    private String sha256Hex(File file) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            try (FileInputStream input = new FileInputStream(file)) {
                byte[] buffer = new byte[IO_BUFFER_SIZE];
                int n;
                while ((n = input.read(buffer)) >= 0) {
                    messageDigest.update(buffer, 0, n);
                }
            }
            return toHex(messageDigest.digest());
        } catch (Exception e) {
            throw new IllegalStateException("Calculate object hash failed", e);
        }
    }

    private String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static String defaultEndpoint(String region) {
        return "https://s3." + region + ".amazonaws.com";
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
