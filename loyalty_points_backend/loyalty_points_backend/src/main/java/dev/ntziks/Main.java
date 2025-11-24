package dev.ntziks;


import dev.ntziks.loyalty.adapters.PointsQuoteVerticle;
import dev.ntziks.loyalty.adapters.VertxFxClient;
import dev.ntziks.loyalty.adapters.VertxPromoClient;
import dev.ntziks.loyalty.clients.FxClient;
import dev.ntziks.loyalty.clients.PromoClient;
import dev.ntziks.loyalty.domain.LoyaltyPointsCalculator;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;

public class Main {

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();

        // Shared HTTP client for FX / Promo
        WebClient webClient = WebClient.create(vertx);

        // Dependency wiring ("DI by hand")
        LoyaltyPointsCalculator calculator = new LoyaltyPointsCalculator();
        FxClient fxClient = new VertxFxClient(webClient, "localhost", 8081);     // external FX service
        PromoClient promoClient = new VertxPromoClient(webClient, "localhost", 8082); // external promo service

        PointsQuoteVerticle verticle = new PointsQuoteVerticle(
                calculator,
                fxClient,
                promoClient
        );

        vertx.deployVerticle(verticle, new DeploymentOptions(), ar -> {
            if (ar.succeeded()) {
                System.out.println("Loyalty Points Service started successfully.");
                System.out.println("HTTP Endpoint: POST http://localhost:<assigned-port>/v1/points/quote");
                System.out.println("NOTE: Actual port is determined inside the verticle.");
            } else {
                System.err.println("Failed to start service: " + ar.cause());
            }
        });
    }
}
