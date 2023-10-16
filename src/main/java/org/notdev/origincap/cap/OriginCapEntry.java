package org.notdev.origincap.cap;

import java.util.ArrayList;
import java.util.UUID;

/**
 * The type Origin cap entry.
 */
public class OriginCapEntry extends ArrayList<UUID> {
    private static final long serialVersionUID = 2345325453124L;// Change this value if you want to update changes how serialization works
    private int maxSize;
    private boolean shouldOverrideMax = false;

    /**
     * Instantiates a new Origin cap entry.
     *
     * @param maxSize the max amount of players that can be added to this origin entry
     */
    public OriginCapEntry(int maxSize) {
        this.maxSize = maxSize;
    }

    // adds condition to add so it wont add player if the origin is full and no duplicates
    @Override
    public boolean add(UUID uuid) {
        if (isFull() || contains(uuid)) return false;
        return super.add(uuid);
    }

    

    public int getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }

    /**
     * Checks if this origin entry is at capacity based on {@code maxSize}
     *
     * @return true if full false if not
     */
    public boolean isFull() {
        return size() >= maxSize;
    }

    public boolean shouldOverrideMax() {
        return shouldOverrideMax;
    }

    public void setShouldOverrideMax(boolean should, int max) {
        this.shouldOverrideMax = should;
        setMaxSize(max);
    }


    @Override
    public String toString() {
        if (isEmpty()) return "[]";
        StringBuilder builder = new StringBuilder();
        builder.append("[ ");
        forEach((uuid) -> builder.append(uuid + ", "));
        builder.delete(builder.length() - 2, builder.length());
        builder.append(" ]");
        return builder.toString();
    }

}
