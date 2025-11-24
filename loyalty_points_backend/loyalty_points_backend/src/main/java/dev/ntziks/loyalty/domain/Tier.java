package dev.ntziks.loyalty.domain;

import java.util.Locale;

// domain/Tier.java
public enum Tier {
    NONE(0.0),
    SILVER(0.15),
    GOLD(0.30),
    PLATINUM(0.50);

    private final double multiplier;

    Tier(double multiplier) {
        this.multiplier = multiplier;
    }

    public double multiplier() {
        return multiplier;
    }

    public static Tier from(String s) {
        try {
            return Tier.valueOf(s.toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return NONE;
        }
    }
}

