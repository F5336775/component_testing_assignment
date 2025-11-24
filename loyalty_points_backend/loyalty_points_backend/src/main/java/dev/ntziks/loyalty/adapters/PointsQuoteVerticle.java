package dev.ntziks.loyalty.adapters;

import dev.ntziks.loyalty.api.dto.LoyaltyQuoteRequest;
import dev.ntziks.loyalty.api.dto.LoyaltyQuoteResponse;
import dev.ntziks.loyalty.clients.FxClient;
import dev.ntziks.loyalty.clients.PromoClient;
import dev.ntziks.loyalty.domain.LoyaltyPointsCalculator;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

import java.util.List;

public class PointsQuoteVerticle extends AbstractVerticle {

    private final LoyaltyPointsCalculator calculator;
    private final FxClient fxClient;
    private final PromoClient promoClient;

    public PointsQuoteVerticle(
            LoyaltyPointsCalculator calculator,
            FxClient fxClient,
            PromoClient promoClient
    ) {
        this.calculator = calculator;
        this.fxClient = fxClient;
        this.promoClient = promoClient;
    }

    @Override
    public void start(Promise<Void> startPromise) {
        Router router = Router.router(vertx);
        router.post("/v1/points/quote")
                .consumes("application/json")
                .produces("application/json")
                .handler(this::handleQuote);

        vertx.createHttpServer()
                .requestHandler(router)
                .listen(0, http -> {
                    if (http.succeeded()) {
                        startPromise.complete();
                    } else {
                        startPromise.fail(http.cause());
                    }
                });
    }

    private void handleQuote(RoutingContext ctx) {
        JsonObject json = ctx.body().asJsonObject();
        LoyaltyQuoteRequest req;
        try {
            req = json.mapTo(LoyaltyQuoteRequest.class);
        } catch (Exception e) {
            ctx.response().setStatusCode(400).end("Invalid JSON");
            return;
        }

        // Validation
        if (req.getFareAmount() <= 0) {
            ctx.response().setStatusCode(400).end("Invalid fareAmount");
            return;
        }
        if (!List.of("USD", "EUR", "GBP").contains(req.getCurrency()) ||
                !List.of("ECONOMY", "BUSINESS", "FIRST").contains(req.getCabinClass())) {
            ctx.response().setStatusCode(400).end("Invalid currency or cabinClass");
            return;
        }

        String currency = req.getCurrency();
        String promoCode = req.getPromoCode();

        // Resilience: retry FX once
        fetchFxWithRetry(currency, 1).onComplete(fxRes -> {
            if (fxRes.failed()) {
                ctx.response().setStatusCode(502).end("FX service unavailable");
                return;
            }
            double fxRate = fxRes.result();

            promoClient.fetchPromo(promoCode).onComplete(promoRes -> {
                PromoClient.PromoResult promo = promoRes.result();
                LoyaltyQuoteResponse resp = calculator.calculate(
                        req,
                        fxRate,
                        promo.bonus(),
                        promo.expiringSoon()
                );
                ctx.response()
                        .setStatusCode(200)
                        .putHeader("Content-Type", "application/json")
                        .end(JsonObject.mapFrom(resp).encode());
            });
        });
    }

    private Future<Double> fetchFxWithRetry(String currency, int retries) {
        Promise<Double> p = Promise.promise();
        fxClient.fetchRate(currency).onComplete(ar -> {
            if (ar.succeeded()) {
                p.complete(ar.result());
            } else if (retries > 0) {
                fetchFxWithRetry(currency, retries - 1).onComplete(p);
            } else {
                p.fail(ar.cause());
            }
        });
        return p.future();
    }
}

