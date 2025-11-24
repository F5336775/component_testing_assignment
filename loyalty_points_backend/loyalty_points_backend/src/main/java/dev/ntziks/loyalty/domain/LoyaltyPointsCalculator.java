package dev.ntziks.loyalty.domain;

import dev.ntziks.loyalty.api.dto.LoyaltyQuoteRequest;
import dev.ntziks.loyalty.api.dto.LoyaltyQuoteResponse;

import java.util.ArrayList;
import java.util.List;

public class LoyaltyPointsCalculator {

    private static final int MAX_POINTS = 50_000;

    public LoyaltyQuoteResponse calculate(
            LoyaltyQuoteRequest req,
            double fxRate,
            int promoBonus,
            boolean promoExpiringSoon
    ) {
        int basePoints = (int) Math.floor(req.getFareAmount() * fxRate);

        Tier tier = Tier.from(req.getCustomerTier());
        int tierBonus = (int) Math.floor(basePoints * tier.multiplier());

        int total = basePoints + tierBonus + promoBonus;
        if (total > MAX_POINTS) {
            total = MAX_POINTS;
        }

        List<String> warnings = new ArrayList<>();
        if (promoExpiringSoon) {
            warnings.add("PROMO_EXPIRES_SOON");
        }

        return new LoyaltyQuoteResponse(
                basePoints,
                tierBonus,
                promoBonus,
                total,
                fxRate,
                warnings
        );
    }
}

