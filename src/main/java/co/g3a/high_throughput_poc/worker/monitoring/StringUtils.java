package co.g3a.high_throughput_poc.worker.monitoring;

public class StringUtils {
    public static boolean hasText(String str) {
        return str != null && !str.trim().isEmpty();
    }
}