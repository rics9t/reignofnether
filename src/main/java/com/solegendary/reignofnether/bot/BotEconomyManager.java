package com.solegendary.reignofnether.bot;

import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingServerEvents;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.building.Buildings;
import com.solegendary.reignofnether.building.buildings.placements.FarmPlacement;
import com.solegendary.reignofnether.faction.Faction;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.player.RTSPlayer;
import com.solegendary.reignofnether.resources.ResourceIndex;
import com.solegendary.reignofnether.resources.ResourceName;
import com.solegendary.reignofnether.resources.ResourceSources;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.interfaces.WorkerUnit;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Rotation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BotEconomyManager {
    private static final int RESOURCE_SEARCH_RANGE = 80;
    private static final int BUILD_SEARCH_STEP = 9;

    public static void serverTick(RTSPlayer bot, BotData data) {
        assignIdleWorkers(bot);
        placeNextBuilding(bot);
    }

    private static void assignIdleWorkers(RTSPlayer bot) {
        List<LivingEntity> workers = getWorkers(bot.name);
        int i = 0;
        for (LivingEntity worker : workers) {
            if (!(worker instanceof WorkerUnit workerUnit) || !WorkerUnit.isIdle(workerUnit)) {
                continue;
            }
            ResourceName target = switch (i % 3) {
                case 0 -> ResourceName.WOOD;
                case 1 -> ResourceName.FOOD;
                default -> ResourceName.ORE;
            };
            assignWorker(worker, target, bot.name);
            i += 1;
        }
    }

    private static void assignWorker(LivingEntity worker, ResourceName resourceName, String ownerName) {
        if (resourceName == ResourceName.FOOD) {
            BuildingPlacement farm = getOwnedFarm(ownerName);
            if (farm != null) {
                UnitServerEvents.addActionItem(ownerName, UnitAction.FARM, -1,
                        new int[] { worker.getId() }, farm.centrePos, BlockPos.ZERO);
                return;
            }
        }

        ServerLevel level = PlayerServerEvents.serverLevel;
        Optional<BlockPos> target = ResourceIndex.get(level).findClosest(level, worker.blockPosition(),
                RESOURCE_SEARCH_RANGE, resourceName,
                bp -> ResourceSources.getBlockResourceName(bp, level) == resourceName);
        target.ifPresent(blockPos -> UnitServerEvents.addActionItem(ownerName, UnitAction.MOVE, -1,
                new int[] { worker.getId() }, blockPos, BlockPos.ZERO));
    }

    private static void placeNextBuilding(RTSPlayer bot) {
        for (Building building : getBuildOrder(bot.faction)) {
            if (hasBuilding(bot.name, building)) {
                continue;
            }
            List<LivingEntity> workers = getIdleWorkers(bot.name);
            if (workers.isEmpty()) {
                return;
            }
            BlockPos origin = findBuildOrigin(bot.name, building);
            if (origin == null) {
                return;
            }
            BuildingPlacement preview = BuildingUtils.getNewBuildingPlacement(building,
                    PlayerServerEvents.serverLevel, origin, Rotation.NONE, bot.name, false);
            if (preview == null || !preview.canAfford(bot.name)) {
                return;
            }
            BuildingServerEvents.placeBuilding(building, origin, Rotation.NONE, bot.name,
                    new int[] { workers.get(0).getId() }, false, false);
            return;
        }
    }

    private static boolean hasBuilding(String ownerName, Building building) {
        for (BuildingPlacement placement : BuildingServerEvents.getBuildings()) {
            if (placement.ownerName.equals(ownerName) && placement.getBuilding().isTypeOf(building)) {
                return true;
            }
        }
        return false;
    }

    private static BlockPos findBuildOrigin(String ownerName, Building building) {
        BlockPos centre = getBaseCentre(ownerName);
        if (centre == null) {
            return null;
        }
        ServerLevel level = PlayerServerEvents.serverLevel;
        for (int radius = BUILD_SEARCH_STEP; radius <= 54; radius += BUILD_SEARCH_STEP) {
            for (int x = -radius; x <= radius; x += BUILD_SEARCH_STEP) {
                for (int z = -radius; z <= radius; z += BUILD_SEARCH_STEP) {
                    if (Math.abs(x) != radius && Math.abs(z) != radius) {
                        continue;
                    }
                    BlockPos bp = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,
                            centre.offset(x, 0, z));
                    BuildingPlacement preview = BuildingUtils.getNewBuildingPlacement(building, level, bp,
                            Rotation.NONE, ownerName, false);
                    if (preview != null && !isOverlapping(preview)) {
                        return bp;
                    }
                }
            }
        }
        return null;
    }

    private static boolean isOverlapping(BuildingPlacement preview) {
        for (BuildingPlacement building : BuildingServerEvents.getBuildings()) {
            if (preview.maxCorner.getX() < building.minCorner.getX() ||
                    preview.minCorner.getX() > building.maxCorner.getX() ||
                    preview.maxCorner.getY() < building.minCorner.getY() ||
                    preview.minCorner.getY() > building.maxCorner.getY() ||
                    preview.maxCorner.getZ() < building.minCorner.getZ() ||
                    preview.minCorner.getZ() > building.maxCorner.getZ()) {
                continue;
            }
            return true;
        }
        return false;
    }

    private static BuildingPlacement getOwnedFarm(String ownerName) {
        for (BuildingPlacement building : BuildingServerEvents.getBuildings()) {
            if (building.ownerName.equals(ownerName) && building.isBuilt && building instanceof FarmPlacement) {
                return building;
            }
        }
        return null;
    }

    static BlockPos getBaseCentre(String ownerName) {
        for (BuildingPlacement building : BuildingServerEvents.getBuildings()) {
            if (building.ownerName.equals(ownerName) && building.isCapitol) {
                return building.centrePos;
            }
        }
        for (BuildingPlacement building : BuildingServerEvents.getBuildings()) {
            if (building.ownerName.equals(ownerName)) {
                return building.centrePos;
            }
        }
        for (LivingEntity unit : UnitServerEvents.getAllUnits()) {
            if (unit instanceof Unit rtsUnit && rtsUnit.getOwnerName().equals(ownerName)) {
                return unit.blockPosition();
            }
        }
        return null;
    }

    static List<LivingEntity> getWorkers(String ownerName) {
        List<LivingEntity> workers = new ArrayList<>();
        for (LivingEntity entity : UnitServerEvents.getAllUnits()) {
            if (entity instanceof Unit unit && entity instanceof WorkerUnit && unit.getOwnerName().equals(ownerName)) {
                workers.add(entity);
            }
        }
        return workers;
    }

    private static List<LivingEntity> getIdleWorkers(String ownerName) {
        List<LivingEntity> idleWorkers = new ArrayList<>();
        for (LivingEntity worker : getWorkers(ownerName)) {
            if (worker instanceof WorkerUnit workerUnit && WorkerUnit.isIdle(workerUnit)) {
                idleWorkers.add(worker);
            }
        }
        return idleWorkers;
    }

    private static List<Building> getBuildOrder(Faction faction) {
        return switch (faction) {
            case VILLAGERS -> List.of(Buildings.TOWN_CENTRE, Buildings.OAK_STOCKPILE,
                    Buildings.VILLAGER_HOUSE, Buildings.WHEAT_FARM, Buildings.BARRACKS,
                    Buildings.VILLAGER_HOUSE, Buildings.BLACKSMITH);
            case MONSTERS -> List.of(Buildings.MAUSOLEUM, Buildings.SPRUCE_STOCKPILE,
                    Buildings.HAUNTED_HOUSE, Buildings.PUMPKIN_FARM, Buildings.GRAVEYARD,
                    Buildings.DUNGEON, Buildings.HAUNTED_HOUSE);
            case PIGLINS -> List.of(Buildings.CENTRAL_PORTAL, Buildings.PORTAL_BASIC,
                    Buildings.NETHERWART_FARM, Buildings.BASTION, Buildings.HOGLIN_STABLES,
                    Buildings.PORTAL_BASIC);
            default -> List.of();
        };
    }
}
