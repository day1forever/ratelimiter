package com.ratelimiter.mode;

import com.google.common.collect.ImmutableMap;
import lombok.NonNull;

import java.util.Arrays;
import java.util.Map;

public enum DurationUnit {
    Minite, Second;

    private static Map<String, DurationUnit> unitMap = initUnitMap();

    private static Map<String, DurationUnit> initUnitMap() {
        ImmutableMap.Builder<String, DurationUnit> builder = new ImmutableMap.Builder<>();
        Arrays.stream(DurationUnit.values())
                .forEach(unit -> builder.put(unit.name(), unit));
        return builder.build();
    }

    public static DurationUnit from(@NonNull String unitValue) {
        if (unitMap.containsKey(unitValue)) {
            return unitMap.get(unitValue);
        } else {
            throw new IllegalArgumentException(
                    String.format("Invalid duration unit with display name: %s", unitValue));
        }
    }
}
