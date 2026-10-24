package com.example.shapedwaypoints.shape;

import com.example.shapedwaypoints.ShapedWaypointsClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ShapeManager {
    private static final ShapeManager INSTANCE = new ShapeManager();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Map<String, Shape> shapes = new LinkedHashMap<>();
    private Path file;

    // Custom-shape recording session
    private List<Vertex> recording;
    private String recordingName;

    public static ShapeManager get() { return INSTANCE; }

    public Collection<Shape> all() { return shapes.values(); }
    public Shape get(String name) { return shapes.get(name.toLowerCase()); }

    public void add(Shape s) { shapes.put(s.name.toLowerCase(), s); save(); }
    public boolean remove(String name) {
        boolean r = shapes.remove(name.toLowerCase()) != null;
        save();
        return r;
    }

    // ---- custom shape recording ----
    public void startRecording(String name) { recordingName = name; recording = new ArrayList<>(); }
    public boolean isRecording() { return recording != null; }
    public String recordingName() { return recordingName; }
    public List<Vertex> recordingPoints() { return recording; }
    public void addPoint(Vertex v) { if (recording != null) recording.add(v); }
    public boolean undoPoint() {
        if (recording == null || recording.isEmpty()) return false;
        recording.remove(recording.size() - 1);
        return true;
    }
    public List<Vertex> stopRecording() {
        List<Vertex> r = recording;
        recording = null;
        recordingName = null;
        return r;
    }

    // ---- persistence ----
    public void load(Minecraft client) {
        shapes.clear();
        String id;
        if (client.getCurrentServer() != null) {
            id = "mp_" + client.getCurrentServer().ip;
        } else if (client.getSingleplayerServer() != null) {
            id = "sp_" + client.getSingleplayerServer().getWorldData().getLevelName();
        } else {
            id = "unknown";
        }
        id = id.replaceAll("[^a-zA-Z0-9._-]", "_");
        file = FabricLoader.getInstance().getConfigDir().resolve("shapedwaypoints").resolve(id + ".json");

        if (!Files.exists(file)) return;
        try (Reader r = Files.newBufferedReader(file)) {
            List<Shape> list = GSON.fromJson(r, new TypeToken<List<Shape>>() {}.getType());
            if (list != null) list.forEach(s -> shapes.put(s.name.toLowerCase(), s));
        } catch (Exception e) {
            ShapedWaypointsClient.LOGGER.error("Failed to load shapes", e);
        }
    }

    public void save() {
        if (file == null) return;
        try {
            Files.createDirectories(file.getParent());
            try (Writer w = Files.newBufferedWriter(file)) {
                GSON.toJson(new ArrayList<>(shapes.values()), w);
            }
        } catch (Exception e) {
            ShapedWaypointsClient.LOGGER.error("Failed to save shapes", e);
        }
    }

    public void saveAndClear() {
        save();
        shapes.clear();
        recording = null;
        file = null;
    }
}
