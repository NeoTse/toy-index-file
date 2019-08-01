package me.johntse.toy.index.common;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 计数器。
 *
 * @author John Tse
 */
final class Counter {
    private Map<String, Integer> count = new LinkedHashMap<>();

    public void increment(String name, int increment) {
        Integer n = count.get(name);
        if (n == null) {
            n = increment;
        } else {
            n += increment;
        }

        count.put(name, n);
    }

    public void add(String name, int n) {
        count.put(name, n);
    }

    public void remove(String name) {
        count.remove(name);
    }

    public int get(String name) {
        Integer n = count.get(name);
        return n == null ? -1 : n;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> entry : count.entrySet()) {
            sb.append(entry.getKey()).append(" = ").append(entry.getValue()).append("\n");
        }

        return sb.toString();
    }
}
