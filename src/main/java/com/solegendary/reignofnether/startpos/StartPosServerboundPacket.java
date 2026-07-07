package com.solegendary.reignofnether.startpos;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.bot.BotDifficulty;
import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.faction.Faction;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class StartPosServerboundPacket {

    StartPosAction action;
    BlockPos blockPos;
    Faction faction;
    String playerName;
    BotDifficulty botDifficulty;

    public static void reservePos(BlockPos pos, Faction faction, String playerName) {
        PacketHandler.INSTANCE.sendToServer(new StartPosServerboundPacket(StartPosAction.RESERVE, pos, faction, playerName));
    }

    public static void unreservePos(BlockPos pos) {
        PacketHandler.INSTANCE.sendToServer(new StartPosServerboundPacket(StartPosAction.UNRESERVE, pos, Faction.NONE, ""));
    }

    public static void readyPlayer(String playerName) {
        PacketHandler.INSTANCE.sendToServer(new StartPosServerboundPacket(StartPosAction.PLAYER_READY, new BlockPos(0,0,0), Faction.NONE, playerName));
    }

    public static void unreadyPlayer(String playerName) {
        PacketHandler.INSTANCE.sendToServer(new StartPosServerboundPacket(StartPosAction.PLAYER_UNREADY, new BlockPos(0,0,0), Faction.NONE, playerName));
    }

    public static void enablePos(BlockPos pos) {
        PacketHandler.INSTANCE.sendToServer(new StartPosServerboundPacket(StartPosAction.ENABLE, pos, Faction.NONE, ""));
    }

    public static void disablePos(BlockPos pos) {
        PacketHandler.INSTANCE.sendToServer(new StartPosServerboundPacket(StartPosAction.DISABLE, pos, Faction.NONE, ""));
    }

    public static void addBot(BotDifficulty difficulty) {
        PacketHandler.INSTANCE.sendToServer(new StartPosServerboundPacket(StartPosAction.ADD_BOT,
                BlockPos.ZERO, Faction.NONE, "", difficulty));
    }

    public static void removeBot() {
        PacketHandler.INSTANCE.sendToServer(new StartPosServerboundPacket(StartPosAction.REMOVE_BOT,
                BlockPos.ZERO, Faction.NONE, ""));
    }

    public StartPosServerboundPacket(StartPosAction action, BlockPos pos, Faction faction, String playerName) {
        this(action, pos, faction, playerName, BotDifficulty.NORMAL);
    }

    public StartPosServerboundPacket(StartPosAction action, BlockPos pos, Faction faction, String playerName,
                                     BotDifficulty botDifficulty) {
        this.action = action;
        this.blockPos = pos;
        this.faction = faction;
        this.playerName = playerName;
        this.botDifficulty = botDifficulty;
    }

    public StartPosServerboundPacket(FriendlyByteBuf buffer) {
        this.action = buffer.readEnum(StartPosAction.class);
        this.blockPos = buffer.readBlockPos();
        this.faction = buffer.readEnum(Faction.class);
        this.playerName = buffer.readUtf();
        this.botDifficulty = buffer.readEnum(BotDifficulty.class);
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(this.action);
        buffer.writeBlockPos(this.blockPos);
        buffer.writeEnum(this.faction);
        buffer.writeUtf(this.playerName);
        buffer.writeEnum(this.botDifficulty);
    }

    // server-side packet-consuming functions
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);
        ctx.get().enqueueWork(() -> {

            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                ReignOfNether.LOGGER.warn("GameruleServerboundPacket: Sender was null");
                success.set(false);
                return;
            }
            else if ((action == StartPosAction.ENABLE || action == StartPosAction.DISABLE) &&
                    !player.hasPermissions(4)) {
                ReignOfNether.LOGGER.warn("GameruleServerboundPacket: Tried to process packet from " + player.getName() + " with insufficient permissions");
                success.set(false);
                return;
            }
            else if ((action == StartPosAction.ADD_BOT || action == StartPosAction.REMOVE_BOT) &&
                    !player.hasPermissions(2)) {
                ReignOfNether.LOGGER.warn("StartPosServerboundPacket: Tried to process bot action from " + player.getName() + " with insufficient permissions");
                success.set(false);
                return;
            }

            switch (action) {
                case RESERVE -> {
                    if (StartPosServerEvents.isStartingGame())
                        return;
                    for (StartPos startPos : StartPosServerEvents.startPoses) {
                        if (startPos.pos.equals(blockPos) && startPos.enabled) {
                            startPos.reset();
                            startPos.faction = faction;
                            startPos.playerName = playerName;
                            StartPosClientboundPacket.reservePos(blockPos, faction, playerName);
                        } else if (startPos.playerName.equals(playerName)) {
                            startPos.reset();
                        }
                    }
                    StartPosServerEvents.setPlayerReady(playerName, false);
                }
                case UNRESERVE -> {
                    if (StartPosServerEvents.isStartingGame())
                        return;
                    for (StartPos startPos : StartPosServerEvents.startPoses) {
                        if (startPos.pos.equals(blockPos)) {
                            startPos.reset();
                            StartPosClientboundPacket.unreservePos(blockPos);
                            break;
                        }
                    }
                    StartPosServerEvents.setPlayerReady(playerName, false);
                }
                case PLAYER_READY -> StartPosServerEvents.setPlayerReady(playerName, true);
                case PLAYER_UNREADY -> StartPosServerEvents.setPlayerReady(playerName, false);
                case ENABLE -> StartPosServerEvents.setPosEnabled(blockPos, true);
                case DISABLE -> StartPosServerEvents.setPosEnabled(blockPos, false);
                case ADD_BOT -> StartPosServerEvents.addBot(botDifficulty);
                case REMOVE_BOT -> StartPosServerEvents.removeBot();
            }
            success.set(true);
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
