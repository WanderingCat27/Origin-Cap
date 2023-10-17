package org.notdev.origincap.server;

import static com.mojang.brigadier.builder.LiteralArgumentBuilder.literal;
import static com.mojang.brigadier.builder.RequiredArgumentBuilder.argument;

import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

import org.notdev.origincap.cap.OriginCapEntry;
import org.notdev.origincap.global.CapHandler;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import io.github.apace100.origins.command.LayerArgumentType;
import io.github.apace100.origins.command.OriginArgumentType;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class InitializeOriginCapServer implements DedicatedServerModInitializer {
  /**
   * Runs the mod initializer on the client environment.
   */

  private static final String help = "/cap\n" + //
      "    > clear (clearing removes the cap data from this mod but does not force players to rechoose their origin)\n"
      + //
      "        > all (resets entire cap)\n" + //
      "        > layer (resets just one layer of cap)\n" + //
      "        > origin (resets a specfic origin on a specific layer)\n" + //
      "    > ignore (A blacklist of sorts, allows anyone to choose from the origin or layer specified)\n" + //
      "        > layer (blacklist a layer from being capped)\n" + //
      "        > origin (blacklist an origin from being capped)\n" + //
      "\n" + //
      "    > overrideMax (Allows for you to specify the maximum number of players can choose globally, per layer, or per origin)\n"
      + //
      "        > set (set the override for each of the following)\n" + //
      "            > globalDef (max for all non overrided layers and origins)\n" + //
      "            > layer (per layer) \n" + //
      "            > origin (per origin)\n" + //
      "        > remove (remove an override setting it to its parents default)\n" + //
      "            > layer (returns the layer to follow the global def)\n" + //
      "            > origin (returns the origin to follow its layer if the layer overrides or the global max)\n" + //
      "        > printOverrides (prints your changes and overrides)\n" + //
      "\n" + //
      "    > print (prints the state of the cap, uses uuids of players)\n" + //
      "    > removePlayer (Clear a player from the cap)\n" + //
      "        > offline (if the player is offline, the mod attempts to fetch the uuid of the player from mojang servers, so type their username in as an argument)\n"
      + //
      "        > online (if the player is on the server, you can just type their username with autocomplete)";

  @Override

  public void onInitializeServer() {
    CapHandler.serverInit();

    // wait to register cap until server starts so that origins is initialized
    ServerLifecycleEvents.SERVER_STARTED.register(server -> {
      CapHandler.registerServerPackets();
    });

    registerCommands();
  }

  private void registerCommands() {

    CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
      dispatcher.register(
          (LiteralArgumentBuilder) literal("cap")
              .requires(source -> ((ServerCommandSource) source)
                  .hasPermissionLevel(4))
              .then(printCap())
              .then(clearCap())
              .then(ignore())
              .then(removePlayer())
              .then(overrideMax())
              .then(help()));
    });
  }

  private ArgumentBuilder help() {
    return literal("help").executes(context -> {
      ((ServerCommandSource) context.getSource())
          .sendFeedback(() -> Text.literal(help),
              false);
      return 1;
    });
  }

  private ArgumentBuilder overrideMax() {
    return literal("overrideMax")
        .then(literal("set")
            .then(literal("globalDef")
                .then(argument("max", IntegerArgumentType.integer())
                    .executes(context -> {
                      int max = IntegerArgumentType.getInteger(context,
                          "max");
                      CapHandler.originCap.setDefaultCapMaxSize(max);
                      ((ServerCommandSource) context.getSource())
                          .sendFeedback(() -> Text.literal(
                              "Set global cap default to "
                                  + max),
                              true);

                      return 1;
                    }

                    )))
            .then(literal("layer")
                .then(argument("layer", LayerArgumentType.layer())
                    .then(argument("max", IntegerArgumentType.integer())
                        .executes(context -> {
                          String layerId = LayerArgumentType
                              .getLayer((CommandContext) context,
                                  "layer")
                              .getIdentifier().toString();

                          int max = IntegerArgumentType
                              .getInteger(context, "max");
                          CapHandler.originCap
                              .findOrCreateKey(layerId)
                              .setShouldOverrideMax(true,
                                  max);
                          ((ServerCommandSource) context
                              .getSource())
                              .sendFeedback(() -> Text
                                  .literal("Set max of " + layerId + " to " + max),
                                  true);

                          return 1;
                        }

                        ))))

            .then(literal("origin")
                .then(argument("layer", LayerArgumentType.layer())
                    .then(argument("origin", OriginArgumentType.origin())
                        .then(argument("max", IntegerArgumentType.integer())
                            .executes(context -> {
                              String layerId = LayerArgumentType
                                  .getLayer((CommandContext) context,
                                      "layer")
                                  .getIdentifier().toString();
                              String originId = OriginArgumentType
                                  .getOrigin((CommandContext) context,
                                      "origin")
                                  .getIdentifier().toString();

                              int max = IntegerArgumentType
                                  .getInteger(context, "max");
                              CapHandler.originCap
                                  .findOrCreateKey(layerId).findOrCreateKey(originId)
                                  .setShouldOverrideMax(true,
                                      max);
                              ((ServerCommandSource) context
                                  .getSource())
                                  .sendFeedback(() -> Text
                                      .literal("Set max of " + originId + " in layer " + layerId
                                          + " to " + max),
                                      true);

                              return 1;
                            }

                            ))))

            ))
        .then(literal("remove")
            .then(literal("layer")
                .then(argument("layer", LayerArgumentType.layer())
                    .executes(context -> {
                      String layerId = LayerArgumentType
                          .getLayer((CommandContext) context,
                              "layer")
                          .getIdentifier().toString();
                      CapHandler.originCap
                          .findOrCreateKey(layerId)
                          .setShouldOverrideMax(false);

                      CapHandler.originCap.updateOriginMaxes();
                      ((ServerCommandSource) context
                          .getSource())
                          .sendFeedback(() -> Text
                              .literal("Removed max override of " + layerId),
                              true);

                      return 1;
                    }

                    )))

            .then(literal("origin")
                .then(argument("layer", LayerArgumentType.layer())
                    .then(argument("origin", OriginArgumentType.origin())
                            .executes(context -> {
                              String layerId = LayerArgumentType
                                  .getLayer((CommandContext) context,
                                      "layer")
                                  .getIdentifier().toString();
                              String originId = OriginArgumentType
                                  .getOrigin((CommandContext) context,
                                      "origin")
                                  .getIdentifier().toString();

                              CapHandler.originCap
                                  .findOrCreateKey(layerId).findOrCreateKey(originId)
                                  .setShouldOverrideMax(false);
                              CapHandler.originCap.updateOriginMaxes();
                              ((ServerCommandSource) context
                                  .getSource())
                                  .sendFeedback(() -> Text
                                      .literal("removed max of " + originId + " in layer " + layerId),
                                      true);

                              return 1;
                            }

                            )))))

        .then(literal("printOverrides")
            .executes(context -> {
              StringBuilder builder = new StringBuilder();
              builder.append("Global Default Max = " + CapHandler.originCap.getDefaultCapMaxSize());
              CapHandler.originCap.forEach((layerId, layer) -> {
                if (layer.shouldOverrideMax())
                  builder.append("\n").append(layerId).append(" overrides global with max of ")
                      .append(layer.getDefaultLayerCap());

                layer.forEach((originId, origin) -> {
                  if (origin.shouldOverrideMax())
                    builder.append("\n\t").append(originId)
                        .append(" overrides global and layer max with max of ").append(origin.getMaxSize());
                });

                ((ServerCommandSource) context
                    .getSource())
                    .sendFeedback(() -> Text
                        .literal(builder.toString()), false);
              });
              return 1;
            }));
  }

  private ArgumentBuilder removePlayer() {
    return literal("removePlayer")
        .then(literal("online")
            .then(argument("player", EntityArgumentType.player())
                .executes(context -> {
                  ServerPlayerEntity player = EntityArgumentType.getPlayer(
                      (CommandContext) context,
                      "player");
                  CapHandler.originCap.removePlayerFromList(player.getUuid());
                  ((ServerCommandSource) context.getSource()).sendFeedback(
                      () -> Text.literal("removed " + player
                          .getDisplayName().getString()),
                      true);
                  return 1;
                })))
        .then(literal("offline")
            .then(argument("player", StringArgumentType.string())
                .executes(context -> {
                  String playerName = StringArgumentType
                      .getString((CommandContext) context,
                          "player");
                  System.out.println(playerName);
                  UUID uuid;
                  try {
                    uuid = UUIDTools.playerNameToUUID(
                        playerName);
                    System.out.println(uuid);
                  } catch (IOException e) {
                    System.err.println(
                        "IO exception while trying to attain uuid from player name in command");
                    uuid = null;
                  }
                  if (uuid == null) {
                    ((ServerCommandSource) context
                        .getSource())
                        .sendFeedback(() -> Text
                            .literal(
                                "Could not find uuid from name provided, check for case and typos"),
                            true);
                    return 0;
                  } else {
                    CapHandler.originCap
                        .removePlayerFromList(
                            uuid);

                    ((ServerCommandSource) context
                        .getSource())
                        .sendFeedback(() -> Text
                            .literal("removed "
                                + playerName),
                            true);
                    return 1;
                  }
                })));
  }

  private ArgumentBuilder printCap() {
    return literal("print")
        .executes(context -> {
          ServerCommandSource source = (ServerCommandSource) context.getSource();
          source.sendMessage(Text.literal(CapHandler.originCap.toString()));
          source.sendFeedback(() -> Text.literal("Cap printed successfully."), true);
          return 1;
        });
  }

  private ArgumentBuilder clearCap() {
    return literal("clear")
        .then(literal("all")
            .executes(context -> {
              CapHandler.originCap.clear();
              SaveLoadCap.save(CapHandler.originCap);
              ServerCommandSource source = (ServerCommandSource) context
                  .getSource();
              source.sendFeedback(
                  () -> Text.literal("Cap cleared successfully."),
                  true);
              return 1;
            }))
        .then(literal("layer")
            .then(argument("layer", LayerArgumentType.layer())
                .executes(context -> {
                  String layerId = LayerArgumentType
                      .getLayer(((CommandContext) context),
                          "layer")
                      .getIdentifier()
                      .toString();
                  if (CapHandler.originCap.containsKey(layerId)) {
                    CapHandler.originCap.get(layerId)
                        .clear();
                    SaveLoadCap.save(CapHandler.originCap);
                  }
                  ServerCommandSource source = (ServerCommandSource) context
                      .getSource();
                  source.sendFeedback(
                      () -> Text.literal(
                          "Cap cleared successfully."),
                      true);
                  return 1;
                })))
        .then(literal("origin")
            .then(argument("layer", LayerArgumentType.layer())
                .then(argument("origin", OriginArgumentType.origin())
                    .executes(context -> {
                      String layerId = LayerArgumentType
                          .getLayer(((CommandContext) context),
                              "layer")
                          .getIdentifier()
                          .toString();

                      String originId = OriginArgumentType
                          .getOrigin(((CommandContext) context),
                              "layer")
                          .getIdentifier()
                          .toString();
                      if (CapHandler.originCap
                          .containsKey(layerId)) {
                        OriginCapEntry e = CapHandler.originCap
                            .get(layerId)
                            .get(originId);
                        if (e != null) {
                          e.clear();
                          SaveLoadCap.save(
                              CapHandler.originCap);
                        }
                      }
                      ServerCommandSource source = (ServerCommandSource) context
                          .getSource();
                      source.sendFeedback(
                          () -> Text.literal(
                              "Cap cleared successfully."),
                          true);
                      return 1;
                    }))));
  }

  private ArgumentBuilder ignore() {
    return literal("ignore")
        .then(literal("layer")
            .then(literal("add")
                .then(argument("layer", LayerArgumentType.layer())
                    .executes(context -> {
                      String layerId = LayerArgumentType
                          .getLayer(((CommandContext) context),
                              "layer")
                          .getIdentifier()
                          .toString();
                      CapHandler.originCap.ignoreLayers
                          .add(layerId);
                      ServerCommandSource source = (ServerCommandSource) context
                          .getSource();
                      source.sendFeedback(() -> Text
                          .literal("Layer added successfully."),
                          true);
                      return 1;
                    })))
            .then(literal("remove")
                .then(argument("layer", LayerArgumentType.layer())
                    .executes(context -> {
                      String layerId = LayerArgumentType
                          .getLayer(((CommandContext) context),
                              "layer")
                          .getIdentifier()
                          .toString();
                      CapHandler.originCap.ignoreLayers
                          .remove(layerId);
                      ServerCommandSource source = (ServerCommandSource) context
                          .getSource();
                      source.sendFeedback(() -> Text
                          .literal("Layer removed successfully."),
                          true);
                      return 1;
                    })))
            .then(literal("list").executes(context -> {
              ServerCommandSource source = (ServerCommandSource) context
                  .getSource();
              source.sendMessage(Text.literal(Arrays.toString(
                  CapHandler.originCap.ignoreLayers.toArray())));
              source.sendFeedback(
                  () -> Text.literal(
                      "Layers listed successfully."),
                  true);
              return 1;
            }))
            .then(literal("clear").executes(context -> {
              CapHandler.originCap.ignoreLayers.clear();
              ServerCommandSource source = (ServerCommandSource) context
                  .getSource();
              source.sendFeedback(() -> Text
                  .literal("commands.execute.conditional.pass"),
                  true);
              source.sendFeedback(
                  () -> Text.literal(
                      "Layers cleared successfully."),
                  true);
              return 1;
            })))
        .then(literal("origin")
            .then(literal("add")
                .then(argument("layer", LayerArgumentType.layer())
                    .then(argument("origin",
                        OriginArgumentType
                            .origin())
                        .executes(context -> {
                          String layerId = LayerArgumentType
                              .getLayer(((CommandContext) context),
                                  "layer")
                              .getIdentifier()
                              .toString();
                          String originId = OriginArgumentType
                              .getOrigin(((CommandContext) context),
                                  "origin")
                              .getIdentifier()
                              .toString();
                          CapHandler.originCap
                              .findOrCreateKey(
                                  layerId).ignoreOrigins
                              .add(originId);
                          ServerCommandSource source = (ServerCommandSource) context
                              .getSource();
                          source.sendFeedback(
                              () -> Text.literal(
                                  "Origin added successfully."),
                              true);
                          return 1;
                        }))))
            .then(literal("remove")
                .then(argument("layer", LayerArgumentType.layer())
                    .then(argument("origin",
                        OriginArgumentType
                            .origin())
                        .executes(context -> {
                          String layerId = LayerArgumentType
                              .getLayer(((CommandContext) context),
                                  "layer")
                              .getIdentifier()
                              .toString();
                          String originId = OriginArgumentType
                              .getOrigin(((CommandContext) context),
                                  "origin")
                              .getIdentifier()
                              .toString();

                          if (CapHandler.originCap
                              .containsKey(layerId))
                            CapHandler.originCap
                                .get(layerId).ignoreOrigins
                                .remove(originId);
                          ServerCommandSource source = (ServerCommandSource) context
                              .getSource();
                          source.sendFeedback(
                              () -> Text.literal(
                                  "Origin removed successfully."),
                              true);
                          return 1;
                        }))))
            .then(literal("list")
                .then(argument("layer", LayerArgumentType.layer())
                    .executes(context -> {
                      String layerId = LayerArgumentType
                          .getLayer(((CommandContext) context),
                              "layer")
                          .getIdentifier()
                          .toString();
                      String text = "[]";

                      if (CapHandler.originCap
                          .containsKey(layerId))
                        text = Arrays.toString(
                            CapHandler.originCap
                                .get(layerId).ignoreOrigins
                                .toArray());
                      ServerCommandSource source = (ServerCommandSource) context
                          .getSource();
                      source.sendMessage(
                          Text.literal(text));
                      source.sendFeedback(() -> Text
                          .literal("Origins listed successfully."),
                          true);
                      return 1;
                    })))
            .then(literal("clear")
                .then(argument("layer", LayerArgumentType.layer())
                    .executes(context -> {
                      String layerId = LayerArgumentType
                          .getLayer(((CommandContext) context),
                              "layer")
                          .getIdentifier()
                          .toString();

                      if (CapHandler.originCap
                          .containsKey(layerId))
                        CapHandler.originCap
                            .get(
                                layerId).ignoreOrigins
                            .clear();
                      ServerCommandSource source = (ServerCommandSource) context
                          .getSource();
                      source.sendFeedback(() -> Text
                          .literal("Origins cleared successfully."),
                          true);
                      return 1;
                    }))));
  }

}