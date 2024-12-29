// LRUCache.java
import java.io.*;
import java.util.*;

public class LRUCache {
    private final int capacity;
    private final Map<String, CacheEntry> cache;
    private final LinkedList<String> lruList;
    private final String cacheDirectory;

    public LRUCache(int capacity) {
        this.capacity = capacity;
        this.cache = new HashMap<>();
        this.lruList = new LinkedList<>();
        this.cacheDirectory = "proxy_cache/";
        createCacheDirectory();
    }

    private void createCacheDirectory() {
        File directory = new File(cacheDirectory);
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }

    public synchronized CacheEntry get(String key) {
        if (cache.containsKey(key)) {
            lruList.remove(key);
            lruList.addFirst(key);
            return cache.get(key);
        }
        return null;
    }

    public synchronized void put(String key, String content, String etag) {
        try {
            // Remove least recently used item if cache is full
            if (cache.size() >= capacity && !cache.containsKey(key)) {
                String lru = lruList.removeLast();
                cache.remove(lru);
                deleteFromDisk(lru);
            }

            // Save content to disk
            String filename = cacheDirectory + key.hashCode() + ".cache";
            try (FileWriter writer = new FileWriter(filename)) {
                writer.write(content);
            }

            // Update cache entry
            CacheEntry entry = new CacheEntry(filename, etag);
            cache.put(key, entry);
            lruList.remove(key); // Remove if exists
            lruList.addFirst(key);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void deleteFromDisk(String key) {
        CacheEntry entry = cache.get(key);
        if (entry != null) {
            new File(entry.filename).delete();
        }
    }

    public static class CacheEntry {
        public final String filename;
        public final String etag;
        public final long timestamp;

        public CacheEntry(String filename, String etag) {
            this.filename = filename;
            this.etag = etag;
            this.timestamp = System.currentTimeMillis();
        }

        public String getContent() throws IOException {
            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
            }
            return content.toString();
        }
    }
}