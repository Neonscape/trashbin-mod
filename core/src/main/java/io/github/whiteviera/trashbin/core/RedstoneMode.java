package io.github.whiteviera.trashbin.core;

public enum RedstoneMode {
    IGNORE("none"), WHEN_POWERED("powered"), WHEN_UNPOWERED("unpowered");
    private final String key;
    RedstoneMode(String key) { this.key = key; }
    public String translationKey() { return "mode.trashbin." + key; }
    public RedstoneMode next() { return values()[(ordinal() + 1) % values().length]; }
    public boolean allows(boolean powered) {
        return this == IGNORE || (this == WHEN_POWERED) == powered;
    }
    public static RedstoneMode byId(int id) {
        return id >= 0 && id < values().length ? values()[id] : IGNORE;
    }
}
