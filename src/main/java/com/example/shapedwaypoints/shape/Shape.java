package com.example.shapedwaypoints.shape;

import java.util.ArrayList;
import java.util.List;

public class Shape {
    public String name;
    public String type;          // square, rect, circle, polygon, star, custom
    public String dimension;     // stored as a string to stay version-independent
    public int color;            // Xaero color index 0-15
    public boolean visible = true;
    public boolean closed = true;
    public List<Vertex> vertices = new ArrayList<>();

    public Shape() {} // for Gson

    public Shape(String name, String type, String dimension, int color, List<Vertex> vertices) {
        this.name = name;
        this.type = type;
        this.dimension = dimension;
        this.color = color;
        this.vertices = vertices;
    }
}
