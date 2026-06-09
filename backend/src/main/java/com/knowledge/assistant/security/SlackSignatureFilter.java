package com.knowledge.assistant.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

@Component
public class SlackSignatureFilter extends OncePerRequestFilter {

    private static final long MAX_AGE_SECONDS = 300;

    @Value("${slack.signing.secret}")
    private String signingSecret;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/slack/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String timestamp = request.getHeader("X-Slack-Request-Timestamp");
        String slackSig = request.getHeader("X-Slack-Signature");

        if (timestamp == null || slackSig == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing Slack signature headers");
            return;
        }

        long ts;
        try {
            ts = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid timestamp");
            return;
        }

        if (Math.abs(Instant.now().getEpochSecond() - ts) > MAX_AGE_SECONDS) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Request timestamp too old");
            return;
        }

        // Cache the body so the controller can read it after we consume the stream
        byte[] bodyBytes = request.getInputStream().readAllBytes();
        String body = new String(bodyBytes, StandardCharsets.UTF_8);

        String baseString = "v0:" + timestamp + ":" + body;
        String expectedSig = "v0=" + hmacSha256Hex(signingSecret, baseString);

        if (!MessageDigest.isEqual(expectedSig.getBytes(StandardCharsets.UTF_8),
                                   slackSig.getBytes(StandardCharsets.UTF_8))) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid Slack signature");
            return;
        }

        chain.doFilter(new CachedBodyRequestWrapper(request, bodyBytes), response);
    }

    private String hmacSha256Hex(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute HMAC-SHA256", e);
        }
    }

    private static class CachedBodyRequestWrapper extends HttpServletRequestWrapper {

        private final byte[] cachedBody;
        private Map<String, String[]> parsedParams;

        CachedBodyRequestWrapper(HttpServletRequest request, byte[] cachedBody) {
            super(request);
            this.cachedBody = cachedBody;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream stream = new ByteArrayInputStream(cachedBody);
            return new ServletInputStream() {
                public int read() { return stream.read(); }
                public boolean isFinished() { return stream.available() == 0; }
                public boolean isReady() { return true; }
                public void setReadListener(ReadListener listener) {}
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }

        // For application/x-www-form-urlencoded requests (e.g. /slack/interactions),
        // the servlet container parses parameters from the input stream. Since we consumed
        // that stream in the filter, we re-parse from the cached bytes here.
        @Override
        public String getParameter(String name) {
            String[] values = getParameterMap().get(name);
            return (values != null && values.length > 0) ? values[0] : null;
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            String contentType = getContentType();
            if (contentType != null && contentType.contains("application/x-www-form-urlencoded")) {
                if (parsedParams == null) {
                    parsedParams = parseFormBody(new String(cachedBody, StandardCharsets.UTF_8));
                }
                return parsedParams;
            }
            return super.getParameterMap();
        }

        @Override
        public Enumeration<String> getParameterNames() {
            return Collections.enumeration(getParameterMap().keySet());
        }

        @Override
        public String[] getParameterValues(String name) {
            return getParameterMap().get(name);
        }

        private Map<String, String[]> parseFormBody(String body) {
            Map<String, List<String>> map = new LinkedHashMap<>();
            for (String pair : body.split("&")) {
                if (pair.isEmpty()) continue;
                String[] kv = pair.split("=", 2);
                String key = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
                String value = kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
                map.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
            }
            Map<String, String[]> result = new LinkedHashMap<>();
            map.forEach((k, v) -> result.put(k, v.toArray(new String[0])));
            return result;
        }
    }
}
