package dev.ntziks.loyalty.adapters;

import com.github.tomakehurst.wiremock.WireMockServer;
import dev.ntziks.loyalty.clients.FxClient;
import dev.ntziks.loyalty.clients.PromoClient;
import dev.ntziks.loyalty.domain.LoyaltyPointsCalculator;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(VertxExtension.class)
public class PointsQuoteVerticleTest {

    private static WireMockServer fxServer;
    private static WireMockServer promoServer;

    private int port;

    @BeforeAll
    static void startWireMock() {
        fxServer = new WireMockServer(8081);
        promoServer = new WireMockServer(8082);
        fxServer.start();
        promoServer.start();
    }

    @AfterAll
    static void stopWireMock() {
        fxServer.stop();
        promoServer.stop();
    }

    @BeforeEach
    void deploy(Vertx vertx, VertxTestContext testContext) {
        WebClient client = WebClient.create(vertx);

        FxClient fxClient = new VertxFxClient(client, "localhost", 8081);
        PromoClient promoClient = new VertxPromoClient(client, "localhost", 8082);
        LoyaltyPointsCalculator calc = new LoyaltyPointsCalculator();

        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        router.post("/v1/points/quote")
                .consumes("application/json")
                .produces("application/json")
                .handler(new PointsQuoteVerticle(calc, fxClient, promoClient)::handleQuote);

        vertx.createHttpServer()
                .requestHandler(router)
                .listen(8080, ar -> {
                    port = ar.result().actualPort();
                    testContext.completeNow();
                });
    }

    private WebClient webClient(Vertx vertx) {
        return WebClient.create(vertx);
    }

    @Test
    void success_quote_includes_warnings_and_cap(Vertx vertx, VertxTestContext ctx) {
        // FX stub
        fxServer.stubFor(get(urlPathEqualTo("/fx"))
                .willReturn(okJson("{\"rate\":3.0}")));

        // Promo stub: high bonus + expiringSoon
        promoServer.stubFor(get(urlPathEqualTo("/promo"))
                .willReturn(okJson("{\"bonus\":50000,\"expiringSoon\":true}")));

        JsonObject req = new JsonObject()
                .put("fareAmount", 1000.0)
                .put("currency", "USD")
                .put("cabinClass", "ECONOMY")
                .put("customerTier", "PLATINUM")
                .put("promoCode", "BIG");

        webClient(vertx).post(port, "localhost", "/v1/points/quote")
                .putHeader("Content-Type", "application/json")
                .sendJsonObject(req, ar -> {
                    assertThat(ar.succeeded()).isTrue();
                    HttpResponse<Buffer> resp = ar.result();
                    assertThat(resp.statusCode()).isEqualTo(200);
                    assertThat(resp.getHeader("Content-Type")).contains("application/json");

                    JsonObject body = resp.bodyAsJsonObject();
                    // Cap at 50_000
                    assertThat(body.getInteger("totalPoints")).isEqualTo(50_000);
                    assertThat(body.getJsonArray("warnings"))
                            .contains("PROMO_EXPIRES_SOON");

                    ctx.completeNow();
                });
    }

    @Test
    void validation_rejects_invalid_fare(Vertx vertx, VertxTestContext ctx) {
        JsonObject req = new JsonObject()
                .put("fareAmount", 0.0)
                .put("currency", "USD")
                .put("cabinClass", "ECONOMY");

        webClient(vertx).post(port, "localhost", "/v1/points/quote")
                .putHeader("Content-Type", "application/json")
                .sendJsonObject(req, ar -> {
                    assertThat(ar.succeeded()).isTrue();
                    assertThat(ar.result().statusCode()).isEqualTo(400);
                    ctx.completeNow();
                });
    }

    @Test
    void fx_retry_then_failures_result_in_502(Vertx vertx, VertxTestContext ctx) {
        // First both attempts fail
        fxServer.stubFor(get(urlPathEqualTo("/fx"))
                .willReturn(serverError()));

        JsonObject req = new JsonObject()
                .put("fareAmount", 100.0)
                .put("currency", "USD")
                .put("cabinClass", "ECONOMY");

        webClient(vertx).post(port, "localhost", "/v1/points/quote")
                .putHeader("Content-Type", "application/json")
                .sendJsonObject(req, ar -> {
                    assertThat(ar.succeeded()).isTrue();
                    assertThat(ar.result().statusCode()).isEqualTo(502);
                    ctx.completeNow();
                });
    }

    @Test
    void promo_timeout_falls_back_to_no_bonus(Vertx vertx, VertxTestContext ctx) {
        fxServer.stubFor(get(urlPathEqualTo("/fx"))
                .willReturn(okJson("{\"rate\":1.0}")));

        // Delay to trigger timeout; VertxPromoClient uses .timeout(300)
        promoServer.stubFor(get(urlPathEqualTo("/promo"))
                .willReturn(okJson("{\"bonus\":1000,\"expiringSoon\":true}")
                        .withFixedDelay(1_000)));

        JsonObject req = new JsonObject()
                .put("fareAmount", 100.0)
                .put("currency", "USD")
                .put("cabinClass", "ECONOMY")
                .put("customerTier", "SILVER")
                .put("promoCode", "TIMEOUT");

        webClient(vertx).post(port, "localhost", "/v1/points/quote")
                .putHeader("Content-Type", "application/json")
                .sendJsonObject(req, ar -> {
                    assertThat(ar.succeeded()).isTrue();
                    JsonObject body = ar.result().bodyAsJsonObject();
                    assertThat(body.getInteger("promoBonus")).isEqualTo(0);
                    assertThat(body.getJsonArray("warnings")).isEmpty();
                    ctx.completeNow();
                });
    }
}
