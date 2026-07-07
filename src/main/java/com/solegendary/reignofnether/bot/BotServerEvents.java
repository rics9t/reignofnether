package com.solegendary.reignofnether.bot;

import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.player.RTSPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;

public class BotServerEvents {
    private static final Map<String, BotData> botData = new HashMap<>();

    public static BotData getData(String botName) {
        return botData.computeIfAbsent(botName, k -> new BotData());
    }

    public static void setDifficulty(String botName, BotDifficulty difficulty) {
        getData(botName).difficulty = difficulty;
    }

    public static BotDifficulty getDifficulty(String botName) {
        return getData(botName).difficulty;
    }

    public static void removeData(String botName) {
        botData.remove(botName);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent evt) {
        if (evt.phase != TickEvent.Phase.END || PlayerServerEvents.serverLevel == null) {
            return;
        }

        synchronized (PlayerServerEvents.rtsPlayers) {
            for (RTSPlayer rtsPlayer : PlayerServerEvents.rtsPlayers) {
                if (!rtsPlayer.isBot()) {
                    continue;
                }
                BotData data = getData(rtsPlayer.name);
                if (PlayerServerEvents.rtsGameTicks - data.lastDecisionTick < data.difficulty.decisionIntervalTicks) {
                    continue;
                }
                data.lastDecisionTick = PlayerServerEvents.rtsGameTicks;

                // TODO(posture-ai): choose a posture before dispatching managers.
                BotEconomyManager.serverTick(rtsPlayer, data);
                BotProductionManager.serverTick(rtsPlayer, data);
                BotMilitaryManager.serverTick(rtsPlayer, data);
            }
        }
    }
}
