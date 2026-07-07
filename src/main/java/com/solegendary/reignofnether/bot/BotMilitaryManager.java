package com.solegendary.reignofnether.bot;

import com.solegendary.reignofnether.alliance.AlliancesServerEvents;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingServerEvents;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.player.RTSPlayer;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.interfaces.AttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.interfaces.WorkerUnit;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

public class BotMilitaryManager {
    public static void serverTick(RTSPlayer bot, BotData data) {
        List<LivingEntity> army = getArmy(bot.name);
        if (army.size() < data.difficulty.attackThreshold ||
                PlayerServerEvents.rtsGameTicks - data.lastAttackTick < data.difficulty.attackCooldownTicks) {
            return;
        }
        BlockPos target = getAttackTarget(bot.name);
        if (target == null) {
            return;
        }
        int[] ids = army.stream().mapToInt(LivingEntity::getId).toArray();
        UnitServerEvents.addActionItem(bot.name, UnitAction.ATTACK_MOVE, -1, ids, target, BlockPos.ZERO);
        data.lastAttackTick = PlayerServerEvents.rtsGameTicks;
    }

    private static List<LivingEntity> getArmy(String ownerName) {
        List<LivingEntity> army = new ArrayList<>();
        for (LivingEntity entity : UnitServerEvents.getAllUnits()) {
            if (entity instanceof Unit unit && entity instanceof AttackerUnit && !(entity instanceof WorkerUnit) &&
                    unit.getOwnerName().equals(ownerName)) {
                army.add(entity);
            }
        }
        return army;
    }

    private static BlockPos getAttackTarget(String ownerName) {
        for (BuildingPlacement building : BuildingServerEvents.getBuildings()) {
            if (building.isCapitol && isHostileHuman(ownerName, building.ownerName)) {
                return building.centrePos;
            }
        }
        for (BuildingPlacement building : BuildingServerEvents.getBuildings()) {
            if (isHostileHuman(ownerName, building.ownerName)) {
                return building.centrePos;
            }
        }
        for (LivingEntity entity : UnitServerEvents.getAllUnits()) {
            if (entity instanceof Unit unit && isHostileHuman(ownerName, unit.getOwnerName())) {
                return entity.blockPosition();
            }
        }
        return null;
    }

    private static boolean isHostileHuman(String ownerName, String otherName) {
        RTSPlayer other = PlayerServerEvents.getRTSPlayer(otherName);
        return other != null && !other.isBot() && !AlliancesServerEvents.isAllied(ownerName, otherName);
    }
}
