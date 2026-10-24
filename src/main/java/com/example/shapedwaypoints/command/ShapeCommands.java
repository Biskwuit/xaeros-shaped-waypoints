package com.example.shapedwaypoints.command;

import com.example.shapedwaypoints.compat.XaeroBridge;
import com.example.shapedwaypoints.shape.*;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.List;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public final class ShapeCommands {
    private ShapeCommands() {}

    public static void register(CommandDispatcher<FabricClientCommandSource> d) {
        d.register(literal("shape")
            .then(literal("square").then(argument("name", word()).then(argument("size", integer(1, 4096))
                .executes(c -> create(c, "square", p -> ShapeGenerator.square(
                        p.getX(), p.getY(), p.getZ(), getInteger(c, "size"))))
                .then(argument("color", integer(0, 15))
                    .executes(c -> create(c, "square", p -> ShapeGenerator.square(
                            p.getX(), p.getY(), p.getZ(), getInteger(c, "size"))))))))

            .then(literal("rect").then(argument("name", word())
                .then(argument("width", integer(1, 4096)).then(argument("length", integer(1, 4096))
                    .executes(c -> create(c, "rect", p -> ShapeGenerator.rectangle(
                            p.getX(), p.getY(), p.getZ(), getInteger(c, "width"), getInteger(c, "length"))))))))

            .then(literal("circle").then(argument("name", word()).then(argument("radius", integer(1, 4096))
                .executes(c -> create(c, "circle", p -> ShapeGenerator.circle(
                        p.getX(), p.getY(), p.getZ(), getInteger(c, "radius"), 16)))
                .then(argument("points", integer(3, 128))
                    .executes(c -> create(c, "circle", p -> ShapeGenerator.circle(
                            p.getX(), p.getY(), p.getZ(), getInteger(c, "radius"), getInteger(c, "points"))))))))

            .then(literal("polygon").then(argument("name", word())
                .then(argument("sides", integer(3, 64)).then(argument("radius", integer(1, 4096))
                    .executes(c -> create(c, "polygon", p -> ShapeGenerator.polygon(
                            p.getX(), p.getY(), p.getZ(), getInteger(c, "sides"), getInteger(c, "radius"), -90))))))))

            .then(literal("star").then(argument("name", word())
                .then(argument("points", integer(3, 32)).then(argument("outer", integer(2, 4096))
                    .then(argument("inner", integer(1, 4096))
                        .executes(c -> create(c, "star", p -> ShapeGenerator.star(
                                p.getX(), p.getY(), p.getZ(), getInteger(c, "points"),
                                getInteger(c, "outer"), getInteger(c, "inner")))))))))

            // Custom shapes: walk around and record your position as vertices
            .then(literal("custom")
                .then(literal("start").then(argument("name", word()).executes(ShapeCommands::customStart)))
                .then(literal("point").executes(ShapeCommands::customPoint))
                .then(literal("undo").executes(c -> {
                    msg(c, ShapeManager.get().undoPoint() ? "§eRemoved last point." : "§cNothing to undo.");
                    return 1;
                }))
                .then(literal("finish").executes(c -> customFinish(c, true)))
                .then(literal("finishopen").executes(c -> customFinish(c, false)))
                .then(literal("cancel").executes(c -> {
                    ShapeManager.get().stopRecording();
                    msg(c, "§eRecording cancelled.");
                    return 1;
                })))

            .then(literal("list").executes(ShapeCommands::list))
            .then(literal("remove").then(argument("name", word()).executes(c -> {
                String n = getString(c, "name");
                msg(c, ShapeManager.get().remove(n) ? "§aRemoved " + n : "§cNo shape " + n);
                return 1;
            })))
            .then(literal("toggle").then(argument("name", word()).executes(c -> {
                Shape s = ShapeManager.get().get(getString(c, "name"));
                if (s == null) { msg(c, "§cShape not found."); return 0; }
                s.visible = !s.visible;
                ShapeManager.get().save();
                msg(c, "§e" + s.name + " preview " + (s.visible ? "shown" : "hidden"));
                return 1;
            })))
            .then(literal("export").then(argument("name", word()).executes(ShapeCommands::export)))
        );
    }

    // ---------- helpers ----------
    private interface Gen { List<Vertex> make(BlockPos center); }

    private static int create(CommandContext<FabricClientCommandSource> c, String type, Gen gen) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return 0;
        String name = getString(c, "name");
        int color = 0;
        try { color = IntegerArgumentType.getInteger(c, "color"); } catch (IllegalArgumentException ignored) {}

        BlockPos p = mc.player.blockPosition();
        Shape s = new Shape(name, type, mc.level.dimension().toString(), color, gen.make(p));
        ShapeManager.get().add(s);
        msg(c, "§aCreated " + type + " '" + name + "' with " + s.vertices.size()
                + " points. §7Use /shape export " + name + " to add to Xaero.");
        return 1;
    }

    private static int customStart(CommandContext<FabricClientCommandSource> c) {
        ShapeManager.get().startRecording(StringArgumentType.getString(c, "name"));
        msg(c, "§aRecording custom shape. Walk to each corner and run §f/shape custom point§a.");
        return 1;
    }

    private static int customPoint(CommandContext<FabricClientCommandSource> c) {
        ShapeManager m = ShapeManager.get();
        Minecraft mc = Minecraft.getInstance();
        if (!m.isRecording()) { msg(c, "§cRun /shape custom start <name> first."); return 0; }
        BlockPos p = mc.player.blockPosition();
        m.addPoint(new Vertex(p.getX(), p.getY(), p.getZ()));
        msg(c, "§7Point #" + m.recordingPoints().size() + " at " + p.getX() + " " + p.getY() + " " + p.getZ());
        return 1;
    }

    private static int customFinish(CommandContext<FabricClientCommandSource> c, boolean closed) {
        ShapeManager m = ShapeManager.get();
        if (!m.isRecording()) { msg(c, "§cNot recording."); return 0; }
        String name = m.recordingName();
        List<Vertex> pts = m.stopRecording();
        if (pts.size() < 2) { msg(c, "§cNeed at least 2 points."); return 0; }
        Shape s = new Shape(name, "custom", Minecraft.getInstance().level.dimension().toString(), 0, pts);
        s.closed = closed;
        m.add(s);
        msg(c, "§aSaved custom shape '" + name + "' (" + pts.size() + " points).");
        return 1;
    }

    private static int list(CommandContext<FabricClientCommandSource> c) {
        var all = ShapeManager.get().all();
        if (all.isEmpty()) { msg(c, "§7No shapes."); return 1; }
        msg(c, "§6Shapes:");
        for (Shape s : all) {
            msg(c, " §e" + s.name + " §7(" + s.type + ", " + s.vertices.size() + " pts"
                    + (s.visible ? "" : ", hidden") + ")");
        }
        return 1;
    }

    private static int export(CommandContext<FabricClientCommandSource> c) {
        Shape s = ShapeManager.get().get(getString(c, "name"));
        if (s == null) { msg(c, "§cShape not found."); return 0; }
        int added = XaeroBridge.addShapeAsWaypoints(s);
        if (added >= 0) {
            msg(c, "§aExported " + added + " waypoints to Xaero's Minimap.");
        } else {
            msg(c, "§cCould not access Xaero's internal API (version changed?). See log.");
        }
        return 1;
    }

    private static void msg(CommandContext<FabricClientCommandSource> c, String text) {
        c.getSource().sendFeedback(Component.literal(text));
    }
}
