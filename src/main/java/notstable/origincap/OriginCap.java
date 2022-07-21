package notstable.origincap;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.apace100.origins.command.LayerArgumentType;
import io.github.apace100.origins.command.OriginArgumentType;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;


public class OriginCap implements DedicatedServerModInitializer {


    public static final String MODID = "origin-cap";
    public static final String HANDSHAKE_CHECK = MODID + "handshake-1.0";


    @Override
    public void onInitializeServer() {
        registerCommands();

        // wait to register cap until server starts so that origins is initialized
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            OriginCapList.initialize();
        });

        OriginCapPackets.registerServer();


    }


    private void registerCommands() {
        CommandRegistrationCallback.EVENT.register(new CommandRegistrationCallback() {
            @Override
            public void register(CommandDispatcher<ServerCommandSource> dispatcher, boolean dedicated) {
                dispatcher.register(literal("origincap")
                        .requires(source -> source.hasPermissionLevel(4))
                        // reload cap json
                        .then(literal("reload").executes(context -> {
                            OriginCapList.reloadCap();
                            context.getSource().sendFeedback(new LiteralText("origin cap file reloaded"), true);
                            return 1;
                        }))
                        .then(literal("enforce-whitelist").executes(context -> {
                            String l = OriginCapList.enforceWhitelist(context.getSource().getServer());
                            System.out.println(l);
                            if(l.equals("nonexistant"))
                                context.getSource().sendError(new LiteralText("whitelisted file not found"));
                            else if (l.equals("disabled"))
                                context.getSource().sendError(new LiteralText("whitelisted is not enabled"));

                            else if (l.isBlank())
                                context.getSource().sendFeedback(new LiteralText("no players to remove"), true);
                            else
                                context.getSource().sendFeedback(new LiteralText("removed non whitelisted players from origin cap: " + l), true);
                            return 1;
                        }))
                        // set cap
                        .then(literal("cap")
                                .then(literal("get").executes(context -> {
                                    context.getSource().sendFeedback(new LiteralText("current origin cap is: " + OriginCapList.getCap()), true);

                                    return 1;
                                }))
                                .then(literal("set")
                                        .then(argument("num", IntegerArgumentType.integer(1)).executes(context -> {
                                            int newCap = IntegerArgumentType.getInteger(context, "num");
                                            OriginCapList.setCap(newCap);
                                            context.getSource().sendFeedback(new LiteralText("set cap to: " + newCap), true);

                                            return 1;
                                        }))))

                        /* ==== blacklist origin ==== */
                        .then(literal("blacklist")
                                .then(literal("origin")
                                        // list origins in blacklist
                                        .then(literal("list").executes(context -> {
                                            if (OriginCapList.originBlacklist.isEmpty()) {
                                                context.getSource().sendFeedback(new LiteralText("Origin blacklist is empty"), false);
                                                return 0;
                                            }


                                            String list = "origin blacklist: ";
                                            for (String originID : OriginCapList.originBlacklist.blackList) {
                                                try {
                                                    list += "\n" + originID;
                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                            context.getSource().sendFeedback(new LiteralText(list), false);

                                            return 1;
                                        }))
                                        // add blacklist origin
                                        .then(literal("add")
                                                .then(argument("origin", OriginArgumentType.origin()).executes(context -> {
                                                    String originID = OriginArgumentType.getOrigin(context, "origin").getIdentifier().toString();
                                                    try {
                                                        OriginCapList.originBlacklist.add(originID);
                                                        context.getSource().sendFeedback(new LiteralText("added " + originID), true);
                                                        return 1;

                                                    } catch (Exception e) {
                                                        e.printStackTrace();
                                                    }
                                                    context.getSource().sendError(new LiteralText("failed to add" + originID));

                                                    return 0;


                                                })))

                                        // remove blacklist origin
                                        .then(literal("remove")
                                                .then(argument("origin", OriginArgumentType.origin()).executes(context -> {
                                                    if (OriginCapList.originBlacklist.isEmpty()) {
                                                        context.getSource().sendFeedback(new LiteralText("Origin blacklist is empty"), false);
                                                        return 0;
                                                    }
                                                    String originID = OriginArgumentType.getOrigin(context, "origin").getIdentifier().toString();
                                                    try {
                                                        OriginCapList.layerBlacklist.remove(originID);
                                                        context.getSource().sendFeedback(new LiteralText("removed " + originID), true);
                                                        return 1;

                                                    } catch (Exception e) {
                                                        e.printStackTrace();
                                                    }
                                                    context.getSource().sendError(new LiteralText("failed to remove" + originID));

                                                    return 0;
                                                })))
                                        .then(literal("clear").executes(context -> {
                                            if (OriginCapList.originBlacklist.isEmpty()) {
                                                context.getSource().sendFeedback(new LiteralText("Origin blacklist is empty"), false);
                                                return 0;
                                            }

                                            OriginCapList.originBlacklist.clear();
                                            context.getSource().sendFeedback(new LiteralText("cleared origin blacklist"), true);
                                            return 1;
                                        })))
                                /* ==== blacklist origin layer ==== */
                                .then(literal("layer")
                                        // list layers in blacklist
                                        .then(literal("list").executes(context -> {
                                            if (OriginCapList.layerBlacklist.isEmpty()) {
                                                context.getSource().sendFeedback(new LiteralText("Layer blacklist is empty"), false);
                                                return 0;
                                            }

                                            String list = "layer blacklist: ";
                                            for (String layerID : OriginCapList.layerBlacklist.blackList) {
                                                try {
                                                    list += "\n" + layerID;
                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                            context.getSource().sendFeedback(new LiteralText(list), false);

                                            return 1;
                                        }))
                                        // add blacklist layer
                                        .then(literal("add")
                                                .then(argument("layer", LayerArgumentType.layer()).executes(context -> {
                                                    String layerID = LayerArgumentType.getLayer(context, "layer").getIdentifier().toString();
                                                    try {
                                                        OriginCapList.layerBlacklist.add(layerID);
                                                        context.getSource().sendFeedback(new LiteralText("added " + layerID), true);
                                                        return 1;

                                                    } catch (Exception e) {
                                                        e.printStackTrace();
                                                    }
                                                    context.getSource().sendError(new LiteralText("failed to add" + layerID));

                                                    return 0;


                                                })))

                                        // remove blacklist layer
                                        .then(literal("remove")
                                                .then(argument("layer", LayerArgumentType.layer()).executes(context -> {
                                                    if (OriginCapList.layerBlacklist.isEmpty()) {
                                                        context.getSource().sendFeedback(new LiteralText("Layer blacklist is empty"), false);
                                                        return 0;
                                                    }
                                                    String layerID = LayerArgumentType.getLayer(context, "layer").getIdentifier().toString();
                                                    try {
                                                        OriginCapList.layerBlacklist.remove(layerID);
                                                        context.getSource().sendFeedback(new LiteralText("removed " + layerID), true);
                                                        return 1;

                                                    } catch (Exception e) {
                                                        e.printStackTrace();
                                                    }
                                                    context.getSource().sendError(new LiteralText("failed to remove" + layerID));

                                                    return 0;
                                                })))
                                        .then(literal("clear").executes(context -> {
                                            if (OriginCapList.layerBlacklist.isEmpty()) {
                                                context.getSource().sendFeedback(new LiteralText("Layer blacklist is empty"), false);
                                                return 0;
                                            }
                                            OriginCapList.layerBlacklist.clear();
                                            context.getSource().sendFeedback(new LiteralText("cleared layer blacklist"), true);
                                            return 1;
                                        })))
                                /* ==== blacklist player ==== */

                                .then(literal("player")
                                        // list players in blacklist
                                        .then(literal("list").executes(context -> {
                                            if (OriginCapList.playerUUIDBlacklist.isEmpty()) {
                                                context.getSource().sendFeedback(new LiteralText("Player blacklist is empty"), false);
                                                return 0;
                                            }
                                            String list = "player blacklist: ";
                                            for (String uuid : OriginCapList.playerUUIDBlacklist.blackList) {
                                                try {
                                                    list += "\n" + UUIDTools.UUIDToPlayerName(uuid);
                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                            context.getSource().sendFeedback(new LiteralText(list), false);

                                            return 1;
                                        }))
                                        // add blacklist player
                                        .then(literal("add")
                                                .then(argument("playerName", StringArgumentType.string()).executes(context -> {
                                                    String playerName = StringArgumentType.getString(context, "playerName");
                                                    try {
                                                        String uuid = UUIDTools.playerNameToUUID(playerName);
                                                        if (uuid.isEmpty()) {
                                                            context.getSource().sendError(new LiteralText("failed to add " + playerName));
                                                            return 0;
                                                        }
                                                        OriginCapList.playerUUIDBlacklist.add(uuid);
                                                        context.getSource().sendFeedback(new LiteralText("added " + playerName), true);
                                                        return 1;

                                                    } catch (Exception e) {
                                                        e.printStackTrace();
                                                    }
                                                    context.getSource().sendError(new LiteralText("failed to add " + playerName));

                                                    return 0;


                                                })))

                                        // remove blacklist player
                                        .then(literal("remove")
                                                .then(argument("playerName", StringArgumentType.string()).executes(context -> {
                                                    if (OriginCapList.playerUUIDBlacklist.isEmpty()) {
                                                        context.getSource().sendFeedback(new LiteralText("Player blacklist is empty"), false);
                                                        return 0;
                                                    }
                                                    String playerName = StringArgumentType.getString(context, "playerName");
                                                    try {
                                                        String uuid = UUIDTools.playerNameToUUID(playerName);
                                                        if (uuid.isEmpty()) {
                                                            context.getSource().sendError(new LiteralText("failed to add " + playerName));
                                                            return 0;
                                                        }
                                                        OriginCapList.playerUUIDBlacklist.remove(uuid);
                                                        context.getSource().sendFeedback(new LiteralText("removed " + playerName), true);
                                                        return 1;

                                                    } catch (Exception e) {
                                                        e.printStackTrace();
                                                    }
                                                    context.getSource().sendError(new LiteralText("failed to remove" + playerName));

                                                    return 0;
                                                })))
                                        .then(literal("clear").executes(context -> {
                                            if (OriginCapList.playerUUIDBlacklist.isEmpty()) {
                                                context.getSource().sendFeedback(new LiteralText("Player blacklist is empty"), false);
                                                return 0;
                                            }
                                            OriginCapList.playerUUIDBlacklist.clear();
                                            context.getSource().sendFeedback(new LiteralText("cleared player blacklist"), true);
                                            return 1;
                                        }))))
                        .then(literal("remove")
                                .then(literal("everything").executes(context -> {
                                    OriginCapList.clearFullCap();
                                    context.getSource().sendFeedback(new LiteralText("cleared cap"), true);
                                    return 1;
                                }))
                                .then(literal("origin")
                                        .then(argument("origin", OriginArgumentType.origin()).executes(context -> {
                                            String id = OriginArgumentType.getOrigin(context, "origin").getIdentifier().toString();
                                            OriginCapList.clearCapOrigin(id);
                                            context.getSource().sendFeedback(new LiteralText("cleared " + id + " from cap"), true);
                                            return 1;
                                        })))
                                .then(literal("layer")
                                        .then(argument("layer", LayerArgumentType.layer()).executes(context -> {
                                            String id = LayerArgumentType.getLayer(context, "layer").getIdentifier().toString();
                                            OriginCapList.clearCapLayer(id);
                                            context.getSource().sendFeedback(new LiteralText("cleared " + id + " from cap"), true);
                                            return 1;
                                        })))
                                .then(literal("player")
                                        .then(argument("playerName", StringArgumentType.string()).executes(context -> {
                                            String playerName = StringArgumentType.getString(context, "playerName");
                                            try {
                                                String uuid = UUIDTools.playerNameToUUID(playerName);
                                                if (uuid.isEmpty()) {
                                                    context.getSource().sendError(new LiteralText("could not find" + playerName));
                                                    return 0;
                                                }
                                                OriginCapList.removePlayerAllLayers(uuid);
                                                context.getSource().sendFeedback(new LiteralText("cleared " + playerName + " from cap"), true);
                                                return 1;

                                            } catch (Exception e) {
                                                e.printStackTrace();
                                                return 0;
                                            }
                                        })))));
            }
        });
    }

    /**
     * Runs the mod initializer on the server environment.
     */

    public enum ButtonStatus {
        FULL,
        LOADING,
        CHOOSABLE,
        FETCHING
    }



/*
            .then(argument("layer", LayerArgumentType.layer())
			.then(argument("origin", OriginArgumentType.origin())

 */
}
