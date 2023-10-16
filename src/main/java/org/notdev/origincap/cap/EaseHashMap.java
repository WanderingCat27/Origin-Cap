package org.notdev.origincap.cap;

import java.io.*;
import java.util.HashMap;

/**
 * A Hashmap with some ease of use methods and Serializability
 *
 * @param <K> the type parameter
 * @param <V> the type parameter
 */
public abstract class
EaseHashMap<K, V> extends HashMap<K, V> implements Serializable {

    private static final long serialVersionUID = 2753520131247299845L;// Change this value if you want to update changes how serialization works

    /**
     * Find the value at specified key or add a blank value at that position and return
     *
     * @param key the key
     * @return the v
     */
    public V findOrCreateKey(K key) {
        if (!this.containsKey(key))
            this.put(key, createBlankValue());
        return this.get(key);
    }


    /**
     * Creates a default class instance of value's type
     *
     * @return the instance of V
     */
    protected abstract V createBlankValue();


    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("\n");
        forEach((key, value) -> {
            builder.append(key + ": {\n" + value + "\n}\n");
        });

        return builder.toString();
    }

}

