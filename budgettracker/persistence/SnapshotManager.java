package budgettracker.persistence;

import budgettracker.data.AppData;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class SnapshotManager {
    private static final String SNAPSHOT_DIR = DataManager.getDataDir() + File.separator + "snapshots";
    private static final int MAX_SNAPSHOTS = 5;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    public static String getSnapshotDir() { return SNAPSHOT_DIR; }

    public static void createSnapshot(AppData data) throws IOException {
        Files.createDirectories(Paths.get(SNAPSHOT_DIR));
        String filename = "snapshot_" + LocalDateTime.now().format(FMT) + ".json";
        Path path = Paths.get(SNAPSHOT_DIR, filename);
        Files.writeString(path, DataManager.toJson(data));
        pruneOldSnapshots();
    }

    private static void pruneOldSnapshots() {
        try {
            List<Path> snapshots = listSnapshotPaths();
            // Sort oldest first
            snapshots.sort(Comparator.comparing(p -> p.getFileName().toString()));
            while (snapshots.size() > MAX_SNAPSHOTS) {
                Files.deleteIfExists(snapshots.remove(0));
            }
        } catch (IOException e) {
            System.err.println("Failed to prune snapshots: " + e.getMessage());
        }
    }

    public static List<Path> listSnapshotPaths() throws IOException {
        Path dir = Paths.get(SNAPSHOT_DIR);
        if (!Files.exists(dir)) return new ArrayList<>();
        try (var stream = Files.list(dir)) {
            return stream
                .filter(p -> p.getFileName().toString().endsWith(".json"))
                .sorted(Comparator.comparing((Path p) -> p.getFileName().toString()).reversed())
                .collect(Collectors.toList());
        }
    }

    public static List<String> listSnapshotNames() {
        try {
            return listSnapshotPaths().stream()
                .map(p -> p.getFileName().toString())
                .collect(Collectors.toList());
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    public static AppData restoreSnapshot(String filename) throws IOException {
        Path path = Paths.get(SNAPSHOT_DIR, filename);
        String json = Files.readString(path);
        return DataManager.fromJson(json);
    }

    public static void deleteSnapshot(String filename) throws IOException {
        Files.deleteIfExists(Paths.get(SNAPSHOT_DIR, filename));
    }

    /** Parse display info from filename: snapshot_YYYY-MM-DD_HH-mm-ss.json */
    public static String formatSnapshotName(String filename) {
        try {
            String ts = filename.replace("snapshot_", "").replace(".json", "");
            LocalDateTime dt = LocalDateTime.parse(ts, FMT);
            return dt.format(DateTimeFormatter.ofPattern("MMM d, yyyy  HH:mm:ss"));
        } catch (Exception e) {
            return filename;
        }
    }
}
