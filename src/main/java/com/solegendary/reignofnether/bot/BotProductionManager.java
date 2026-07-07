package com.solegendary.reignofnether.bot;

import com.solegendary.reignofnether.building.BuildingClientboundPacket;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingServerEvents;
import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
import com.solegendary.reignofnether.building.production.ProductionBuilding;
import com.solegendary.reignofnether.building.production.ProductionItem;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.faction.Faction;
import com.solegendary.reignofnether.player.RTSPlayer;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.interfaces.AttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.interfaces.WorkerUnit;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

public class BotProductionManager {
    private static final int WORKER_TARGET = 8;
    private static final int MAX_QUEUE_SIZE = 2;

    public static void serverTick(RTSPlayer bot, BotData data) {
        int workers = getWorkerCount(bot.name);
        int army = getArmyCount(bot.name);
        for (BuildingPlacement building : BuildingServerEvents.getBuildings()) {
            if (!(building instanceof ProductionPlacement production) || !building.ownerName.equals(bot.name) ||
                    !building.isBuilt || production.productionQueue.size() >= MAX_QUEUE_SIZE) {
                continue;
            }
            ProductionItem item = workers < WORKER_TARGET ? getWorkerProduction(bot.faction) :
                    getMilitaryProduction(bot.faction, production, army, data.difficulty.armyTarget);
            if (item != null && production.startProductionItem(item)) {
                BuildingClientboundPacket.startProduction(production.originPos, item);
            }
        }
    }

    private static ProductionItem getWorkerProduction(Faction faction) {
        return switch (faction) {
            case VILLAGERS -> ProductionItems.VILLAGER;
            case MONSTERS -> ProductionItems.ZOMBIE_VILLAGER;
            case PIGLINS -> ProductionItems.GRUNT;
            default -> null;
        };
    }

    private static ProductionItem getMilitaryProduction(Faction faction, ProductionPlacement production,
                                                       int army, int armyTarget) {
        if (army >= armyTarget || !(production.getBuilding() instanceof ProductionBuilding productionBuilding)) {
            return null;
        }
        List<ProductionItem> available = productionBuilding.productions.get();
        for (ProductionItem item : getMilitaryOrder(faction)) {
            if (available.contains(item)) {
                return item;
            }
        }
        return null;
    }

    private static List<ProductionItem> getMilitaryOrder(Faction faction) {
        return switch (faction) {
            case VILLAGERS -> List.of(ProductionItems.VINDICATOR, ProductionItems.PILLAGER);
            case MONSTERS -> List.of(ProductionItems.ZOMBIE, ProductionItems.SKELETON,
                    ProductionItems.CREEPER, ProductionItems.SPIDER);
            case PIGLINS -> List.of(ProductionItems.BRUTE, ProductionItems.HEADHUNTER,
                    ProductionItems.MARAUDER, ProductionItems.HOGLIN, ProductionItems.GRUNT);
            default -> List.of();
        };
    }

    private static int getWorkerCount(String ownerName) {
        int count = 0;
        for (LivingEntity entity : UnitServerEvents.getAllUnits()) {
            if (entity instanceof Unit unit && entity instanceof WorkerUnit && unit.getOwnerName().equals(ownerName)) {
                count += 1;
            }
        }
        return count;
    }

    static int getArmyCount(String ownerName) {
        int count = 0;
        for (LivingEntity entity : UnitServerEvents.getAllUnits()) {
            if (entity instanceof Unit unit && entity instanceof AttackerUnit &&
                    !(entity instanceof WorkerUnit) && unit.getOwnerName().equals(ownerName)) {
                count += 1;
            }
        }
        return count;
    }
}
