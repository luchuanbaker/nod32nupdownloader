package com.clu.tools.utils;

import java.io.Serializable;
import java.util.Map;

/**
 * 代替Map.Entry和Map.SimpleEntry
 * @param <K>
 * @param <V>
 */
public class Entry<K, V> implements Map.Entry<K, V>, Serializable {

    private K key;

    private V value;

    public Entry(K key, V value) {
        this.key = key;
        this.value = value;
    }

    public static <VI, KI> Entry<KI,VI> of(Map.Entry<KI,VI> entry) {
        return new Entry<>(entry.getKey(), entry.getValue());
    }

    public K getKey() {
        return key;
    }

    public Entry<K, V> setKey(K key) {
        this.key = key;
        return this;
    }

    public V getValue() {
        return value;
    }

    public V setValue(V value) {
        V oldValue = this.value;
        this.value = value;
        return oldValue;
    }
}
