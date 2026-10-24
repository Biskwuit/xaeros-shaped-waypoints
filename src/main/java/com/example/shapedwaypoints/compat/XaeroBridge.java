package com.example.shapedwaypoints.compat;

import com.example.shapedwaypoints.ShapedWaypointsClient;
import com.example.shapedwaypoints.shape.Shape;
import com.example.shapedwaypoints.shape.Vertex;
import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Reflection bridge to Xaero's Minimap internals. These are NOT a public API.
 * If exporting fails after a Xaero update, adjust the class/method names here.
 */
public final class XaeroBridge {
    private XaeroBridge() {}

    public static boolean isLoaded() {
        return FabricLoader.getInstance().isModLoaded("xaerominimap")
            || FabricLoader.getInstance().isModLoaded("xaerobetterpvp");
    }

    /** @return number of waypoints added, or -1 on failure */
    public static int addShapeAsWaypoints(Shape shape) {
        if (!isLoaded()) return -1;
        try {
            // Path used by Xaero's Minimap 24.x/25.x: BuiltInHudModules.MINIMAP.getCurrentSession()
            Class<?> modules = Class.forName("xaero.hud.minimap.BuiltInHudModules");
            Object minimap = modules.getField("MINIMAP").get(null);
            Object session = call(minimap, "getCurrentSession");
            Object worldManager = call(session, "getWorldManager");
            Object world = call(worldManager, "getCurrentWorld");
            Object set = call(world, "getCurrentWaypointSet");

            Class<?> wpClass = Class.forName("xaero.common.minimap.waypoints.Waypoint");
            Constructor<?> ctor = findCtor(wpClass);
            Method add = findMethod(set.getClass(), "add", wpClass);

            String initials = shape.name.substring(0, 1).toUpperCase();
            int i = 1;
            for (Vertex v : shape.vertices) {
                Object color = colorArg(ctor.getParameterTypes()[5], shape.color);
                Object wp = ctor.newInstance(v.x(), v.y(), v.z(),
                        shape.name + " #" + i, initials + i, color);
                add.invoke(set, wp);
                i++;
            }

            // Persist to disk
            Object io = call(session, "getWorldManagerIO");
            findMethod(io.getClass(), "saveWorld", world.getClass()).invoke(io, world);
            return shape.vertices.size();
        } catch (Throwable t) {
            ShapedWaypointsClient.LOGGER.error("Xaero waypoint export failed", t);
            return -1;
        }
    }

    // ---------- reflection helpers ----------
    private static Object call(Object target, String name) throws Exception {
        Method m = target.getClass().getMethod(name);
        return m.invoke(target);
    }

    private static Method findMethod(Class<?> cls, String name, Class<?> argType) throws NoSuchMethodException {
        for (Method m : cls.getMethods()) {
            if (m.getName().equals(name) && m.getParameterCount() == 1
                    && m.getParameterTypes()[0].isAssignableFrom(argType)) return m;
        }
        throw new NoSuchMethodException(cls.getName() + "." + name);
    }

    /** Waypoint(int x, int y, int z, String name, String initials, int|WaypointColor color) */
    private static Constructor<?> findCtor(Class<?> wp) throws NoSuchMethodException {
        for (Constructor<?> c : wp.getConstructors()) {
            Class<?>[] p = c.getParameterTypes();
            if (p.length == 6 && p[0] == int.class && p[1] == int.class && p[2] == int.class
                    && p[3] == String.class && p[4] == String.class) return c;
        }
        throw new NoSuchMethodException("Waypoint(int,int,int,String,String,color)");
    }

    /** Older Xaero versions take an int color; newer ones take a WaypointColor enum. */
    private static Object colorArg(Class<?> type, int index) {
        if (type == int.class) return index;
        if (type.isEnum()) {
            Object[] values = type.getEnumConstants();
            return values[Math.floorMod(index, values.length)];
        }
        return index;
    }
}
