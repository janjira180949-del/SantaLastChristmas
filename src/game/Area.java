package game;

/** Describes one area and the world-progress point that unlocks it. */
public final class Area {

    public enum Type {
        FOREST,
        VILLAGE,
        CHRISTMAS_TOWN,
        SANTA_BASE
    }

    private final Type type;
    private final String name;
    private boolean unlocked;

    Area(Type type, String name, boolean unlocked) {
        if (type == null || name == null || name.isBlank()) {
            throw new IllegalArgumentException("Invalid area configuration");
        }
        this.type = type;
        this.name = name;
        this.unlocked = unlocked;
    }

    public Type getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public boolean isUnlocked() {
        return unlocked;
    }

    void unlock() {
        unlocked = true;
    }
}