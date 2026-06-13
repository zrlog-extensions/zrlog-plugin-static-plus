package com.zrlog.plugin.staticplus.sync;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;

public class AwsSigV4Signer {

    private static final String ALGORITHM = "AWS4-HMAC-SHA256";

    private static final java.time.format.DateTimeFormatter AMZ_DATE_FORMATTER =
            java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
                    .withZone(java.time.ZoneOffset.UTC);

    private static final java.time.format.DateTimeFormatter DATE_FORMATTER =
            java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")
                    .withZone(java.time.ZoneOffset.UTC);

    public Map<String, String> sign(String method, URI uri, Map<String, String> headers, String payloadSha256,
                                    String service, String region, String accessKey, String secretKey, String sessionToken) {
        if (isBlank(accessKey) || isBlank(secretKey)) {
            throw new IllegalArgumentException("Missing aws access credentials");
        }

        java.time.Instant now = java.time.Instant.now();
        String amzDate = AMZ_DATE_FORMATTER.format(now);
        String dateStamp = DATE_FORMATTER.format(now);

        Map<String, String> signedHeaders = new LinkedHashMap<>(headers);
        signedHeaders.put("Host", hostHeader(uri));
        signedHeaders.put("X-Amz-Date", amzDate);
        if (!isBlank(sessionToken)) {
            signedHeaders.put("X-Amz-Security-Token", sessionToken);
        }

        TreeMap<String, String> canonicalHeaders = canonicalHeaders(signedHeaders);
        String signedHeaderNames = String.join(";", canonicalHeaders.keySet());
        String canonicalRequest = method + "\n" +
                canonicalUri(uri) + "\n" +
                canonicalQuery(uri) + "\n" +
                canonicalHeadersString(canonicalHeaders) + "\n" +
                signedHeaderNames + "\n" +
                payloadSha256;

        String credentialScope = dateStamp + "/" + region + "/" + service + "/aws4_request";
        String stringToSign = ALGORITHM + "\n" +
                amzDate + "\n" +
                credentialScope + "\n" +
                sha256Hex(canonicalRequest.getBytes(StandardCharsets.UTF_8));
        byte[] signingKey = signingKey(secretKey, dateStamp, region, service);
        String signature = hex(hmac(signingKey, stringToSign));

        signedHeaders.put("Authorization", ALGORITHM + " Credential=" + accessKey + "/" + credentialScope
                + ", SignedHeaders=" + signedHeaderNames + ", Signature=" + signature);
        return signedHeaders;
    }

    public static String sha256Hex(byte[] bytes) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            return hex(messageDigest.digest(bytes));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static String uriEncode(String value, boolean encodeSlash) {
        StringBuilder sb = new StringBuilder();
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        for (byte b : bytes) {
            int ch = b & 0xff;
            if ((ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9') ||
                    ch == '-' || ch == '_' || ch == '.' || ch == '~') {
                sb.append((char) ch);
            } else if (ch == '/' && !encodeSlash) {
                sb.append('/');
            } else {
                sb.append('%');
                char high = Character.toUpperCase(Character.forDigit((ch >>> 4) & 0xf, 16));
                char low = Character.toUpperCase(Character.forDigit(ch & 0xf, 16));
                sb.append(high).append(low);
            }
        }
        return sb.toString();
    }

    private static String canonicalUri(URI uri) {
        String rawPath = uri.getRawPath();
        if (isBlank(rawPath)) {
            return "/";
        }
        return rawPath;
    }

    private static String canonicalQuery(URI uri) {
        String rawQuery = uri.getRawQuery();
        if (isBlank(rawQuery)) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        Collections.addAll(parts, rawQuery.split("&"));
        Collections.sort(parts);
        return String.join("&", parts);
    }

    private static TreeMap<String, String> canonicalHeaders(Map<String, String> headers) {
        TreeMap<String, String> canonicalHeaders = new TreeMap<>();
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            canonicalHeaders.put(entry.getKey().toLowerCase(Locale.ROOT), normalizeHeaderValue(entry.getValue()));
        }
        return canonicalHeaders;
    }

    private static String canonicalHeadersString(TreeMap<String, String> headers) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            sb.append(entry.getKey()).append(':').append(entry.getValue()).append('\n');
        }
        return sb.toString();
    }

    private static String normalizeHeaderValue(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }

    private static String hostHeader(URI uri) {
        int port = uri.getPort();
        String host = uri.getHost();
        if (port < 0 || ("https".equalsIgnoreCase(uri.getScheme()) && port == 443) ||
                ("http".equalsIgnoreCase(uri.getScheme()) && port == 80)) {
            return host;
        }
        return host + ":" + port;
    }

    private static byte[] signingKey(String secretAccessKey, String dateStamp, String region, String service) {
        byte[] kDate = hmac(("AWS4" + secretAccessKey).getBytes(StandardCharsets.UTF_8), dateStamp);
        byte[] kRegion = hmac(kDate, region);
        byte[] kService = hmac(kRegion, service);
        return hmac(kService, "aws4_request");
    }

    private static byte[] hmac(byte[] key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String hex(byte[] bytes) {
        StringJoiner joiner = new StringJoiner("");
        for (byte b : bytes) {
            joiner.add(String.format("%02x", b));
        }
        return joiner.toString();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
