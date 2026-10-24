package com.example.shapedwaypoints;

import com.example.shapedwaypoints.command.ShapeCommands;
import com.example.shapedwaypoints.render.ShapePreview;
import com.example.shapedwaypoints.shape.ShapeManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShapedWaypointsClient implements ClientModInitializer {
    public static final String MOD_ID = "shapedwaypoints";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess) -> ShapeCommands.register(dispatcher));

        ClientTickEvents.END_CLIENT_TICK.register(ShapePreview::tick);

        ClientPlayConnectionEvents.JOIN.register((h, s, client) -> ShapeManager.get().load(client));
        ClientPlayConnectionEvents.DISCONNECT.register((h, client) -> ShapeManager.get().saveAndClear());

        LOGGER.info("Shaped Waypoints loaded");
    }
}
