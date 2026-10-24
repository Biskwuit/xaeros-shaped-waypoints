package com.example.shapedwaypoints.shape;

import java.util.ArrayList;
import java.util.List;

public final class ShapeGenerator {
    private ShapeGenerator() {}

    public static List<Vertex> rectangle(int cx, int y, int cz, int width, int length) {
        int hw = width / 2, hl = length / 2;
        List<Vertex> v = new ArrayList<>();
        v.add(new Vertex(cx - hw, y, cz - hl));
        v.add(new Vertex(cx + hw, y, cz - hl));
        v.add(new Vertex(cx + hw, y, cz + hl));
        v.add(new Vertex(cx - hw, y, cz + hl));
        return v;
    }

    public static List<Vertex> square(int cx, int y, int cz, int size) {
        return rectangle(cx, y, cz, size, size);
    }

    /** Regular polygon. A circle is just a polygon with many sides. */
    public static List<Vertex> polygon(int cx, int y, int cz, int sides, double radius, double rotationDeg) {
        List<Vertex> v = new ArrayList<>();
        double rot = Math.toRadians(rotationDeg);
        for (int i = 0; i < sides; i++) {
            double a = rot + 2 * Math.PI * i / sides;
            v.add(new Vertex(
                    (int) Math.round(cx + Math.cos(a) * radius), y,
                    (int) Math.round(cz + Math.sin(a) * radius)));
        }
        return v;
    }

    public static List<Vertex> circle(int cx, int y, int cz, double radius, int points) {
        return polygon(cx, y, cz, points, radius, 0);
    }

    public static List<Vertex> star(int cx, int y, int cz, int points, double outer, double inner) {
        List<Vertex> v = new ArrayList<>();
        int total = points * 2;
        for (int i = 0; i < total; i++) {
            double r = (i % 2 == 0) ? outer : inner;
            double a = -Math.PI / 2 + Math.PI * i / points;
            v.add(new Vertex(
                    (int) Math.round(cx + Math.cos(a) * r), y,
                    (int) Math.round(cz + Math.sin(a) * r)));
        }
        return v;
    }
}
