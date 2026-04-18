package com.example.sre.mcp.service;

import com.example.sre.mcp.model.LogEntry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class LogReader {

    private static final int MAX_LINE_LENGTH = 300;
    private static final int MAX_LIMIT = 50;
    private static final int READ_BUFFER_SIZE = 65536;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Returns up to {@code limit} recent log lines from the given file.
     * Lines are returned in chronological order (oldest first).
     */
    public List<LogEntry> tail(Path logFile, int limit) throws IOException {
        int capped = Math.min(limit, MAX_LIMIT);
        List<String> rawLines = tailRawLines(logFile, capped);
        return rawLines.stream()
                .map(this::parseLine)
                .filter(e -> e != null)
                .toList();
    }

    /**
     * Searches for lines containing {@code query} (case-insensitive) within the given time window.
     * {@code windowMinutes} of 0 means no time filter.
     */
    public List<LogEntry> search(Path logFile, String query, int windowMinutes) throws IOException {
        if (!Files.exists(logFile)) {
            return List.of();
        }

        Instant cutoff = windowMinutes > 0
                ? Instant.now().minusSeconds(windowMinutes * 60L)
                : Instant.EPOCH;
        String lowerQuery = query.toLowerCase();

        List<LogEntry> results = new ArrayList<>();
        List<String> allLines = Files.readAllLines(logFile);
        for (int i = allLines.size() - 1; i >= 0 && results.size() < MAX_LIMIT; i--) {
            String raw = allLines.get(i);
            if (!raw.contains(query) && !raw.toLowerCase().contains(lowerQuery)) {
                continue;
            }
            LogEntry entry = parseLine(raw);
            if (entry == null) continue;
            if (windowMinutes > 0 && !isAfter(entry.timestamp(), cutoff)) continue;
            results.add(entry);
        }
        Collections.reverse(results);
        return results;
    }

    /**
     * Returns all log lines from the last {@code windowMinutes} minutes.
     * Used by HealthChecker for error rate computation.
     */
    public List<LogEntry> inWindow(Path logFile, int windowMinutes) throws IOException {
        if (!Files.exists(logFile)) {
            return List.of();
        }
        Instant cutoff = Instant.now().minusSeconds(windowMinutes * 60L);
        List<LogEntry> results = new ArrayList<>();
        List<String> allLines = Files.readAllLines(logFile);
        for (int i = allLines.size() - 1; i >= 0; i--) {
            LogEntry entry = parseLine(allLines.get(i));
            if (entry == null) continue;
            if (!isAfter(entry.timestamp(), cutoff)) break;
            results.add(entry);
        }
        Collections.reverse(results);
        return results;
    }

    private List<String> tailRawLines(Path logFile, int count) throws IOException {
        if (!Files.exists(logFile)) {
            return List.of();
        }
        List<String> lines = new ArrayList<>(count + 1);
        try (RandomAccessFile raf = new RandomAccessFile(logFile.toFile(), "r")) {
            long fileLength = raf.length();
            if (fileLength == 0) return List.of();

            long pos = fileLength - 1;
            byte[] buffer = new byte[READ_BUFFER_SIZE];
            StringBuilder currentLine = new StringBuilder();

            while (pos >= 0 && lines.size() < count) {
                long readStart = Math.max(0, pos - READ_BUFFER_SIZE + 1);
                int readLen = (int) (pos - readStart + 1);
                raf.seek(readStart);
                raf.readFully(buffer, 0, readLen);

                for (int i = readLen - 1; i >= 0; i--) {
                    char c = (char) buffer[i];
                    if (c == '\n') {
                        String line = currentLine.reverse().toString().trim();
                        currentLine = new StringBuilder();
                        if (!line.isEmpty()) {
                            lines.add(line);
                            if (lines.size() >= count) break;
                        }
                    } else {
                        currentLine.append(c);
                    }
                }
                pos = readStart - 1;
            }

            if (!currentLine.isEmpty() && lines.size() < count) {
                String line = currentLine.reverse().toString().trim();
                if (!line.isEmpty()) lines.add(line);
            }
        }
        Collections.reverse(lines);
        return lines;
    }

    private LogEntry parseLine(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            JsonNode node = objectMapper.readTree(raw);
            String timestamp = node.path("@timestamp").asText(null);
            String level = node.path("level").asText("UNKNOWN");
            String message = truncate(node.path("message").asText(""));
            String service = node.path("service").asText("");
            String logger = node.path("logger_name").asText("");
            if (timestamp == null) return null;
            return new LogEntry(timestamp, level, message, service, logger);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isAfter(String timestamp, Instant cutoff) {
        try {
            Instant ts = timestamp.endsWith("Z")
                    ? Instant.parse(timestamp)
                    : OffsetDateTime.parse(timestamp).toInstant();
            return ts.isAfter(cutoff);
        } catch (DateTimeParseException e) {
            return true;
        }
    }

    private String truncate(String s) {
        return s.length() > MAX_LINE_LENGTH ? s.substring(0, MAX_LINE_LENGTH) + "…" : s;
    }
}
