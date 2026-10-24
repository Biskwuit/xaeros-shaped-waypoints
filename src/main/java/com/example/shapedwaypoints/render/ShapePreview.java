package com.example.shapedwaypoints.render;

import com.example.shapedwaypoints.shape.Shape;
import com.example.shapedwaypoints.shape.ShapeManager;
import com.example.shapedwaypoints.shape.Vertex;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;

import java.util.List;

public final class ShapePreview {
    private static final double MAX_DIST_SQ = 96 * 96;
    private static int tick;

    private ShapePreview() {}

    public static void tick(Minecraft client) {
        if (client.level == null || client.player == null) return;
        if (++tick % 10 != 0) return;

        String dim = client.level.dimension().toString();
        ShapeManager mgr = ShapeManager.get();

        for (Shape s : mgr.all()) {
            if (s.visible && dim.equals(s.dimension)) {
                drawOutline(client, s.vertices, s.closed, ParticleTypes.END_ROD);
            }
        }
        if (mgr.isRecording()) {
            drawOutline(client, mgr.recordingPoints(), false, ParticleTypes.HAPPY_VILLAGER);
        }
    }

    private static void drawOutline(Minecraft c, List<Vertex> pts, boolean closed, ParticleOptions particle) {
        int n = pts.size();
        if (n == 0) return;
        int edges = closed ? n : n - 1;
        if (n == 1) { spawn(c, particle, pts.get(0).x() + .5, pts.get(0).y() + 1, pts.get(0).z() + .5); return; }

        for (int i = 0; i < edges; i++) {
            Vertex a = pts.get(i), b = pts.get((i + 1) % n);
            double dx = b.x() - a.x(), dy = b.y() - a.y(), dz = b.z() - a.z();
            int steps = Math.max(1, (int) Math.ceil(Math.sqrt(dx * dx + dy * dy + dz * dz)));
            for (int s = 0; s <= steps; s++) {
                double t = (double) s / steps;
                spawn(c, particle, a.x() + .5 + dx * t, a.y() + 1 + dy * t, a.z() + .5 + dz * t);
            }
        }
    }

    private static void spawn(Minecraft c, ParticleOptions p, double x, double y, double z) {
        if (c.player.distanceToSqr(x, y, z) > MAX_DIST_SQ) return;
        c.level.addParticle(p, x, y, z, 0, 0, 0);
    }
}
