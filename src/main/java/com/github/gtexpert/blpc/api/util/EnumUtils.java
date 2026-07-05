package com.github.gtexpert.blpc.api.util;

/** Small parsing helpers for enum constants received over the wire or from external mod data. */
public final class EnumUtils {

    private EnumUtils() {}

    /** {@link Enum#valueOf}, falling back to {@code defaultValue} for an unknown or {@code null} name. */
    public static <E extends Enum<E>> E parseOrDefault(Class<E> type, String name, E defaultValue) {
        if (name == null) return defaultValue;
        try {
            return Enum.valueOf(type, name);
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }
}
