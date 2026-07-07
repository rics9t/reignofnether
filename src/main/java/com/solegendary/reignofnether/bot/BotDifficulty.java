package com.solegendary.reignofnether.bot;

public enum BotDifficulty {
    EASY("Easy", 80, 6, 8, 1200),
    NORMAL("Normal", 60, 8, 12, 900),
    HARD("Hard", 40, 10, 18, 700),
    IMPOSSIBLE("Impossible", 25, 12, 24, 500);

    public final String displayName;
    public final int decisionIntervalTicks;
    public final int attackThreshold;
    public final int armyTarget;
    public final int attackCooldownTicks;

    BotDifficulty(String displayName, int decisionIntervalTicks, int attackThreshold, int armyTarget,
                  int attackCooldownTicks) {
        this.displayName = displayName;
        this.decisionIntervalTicks = decisionIntervalTicks;
        this.attackThreshold = attackThreshold;
        this.armyTarget = armyTarget;
        this.attackCooldownTicks = attackCooldownTicks;
    }

    public BotDifficulty next() {
        BotDifficulty[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public BotDifficulty previous() {
        BotDifficulty[] values = values();
        return values[(ordinal() + values.length - 1) % values.length];
    }
}
