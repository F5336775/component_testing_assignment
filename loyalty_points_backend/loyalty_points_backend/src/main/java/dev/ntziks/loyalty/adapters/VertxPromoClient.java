package dev.ntziks.loyalty.adapters;

import dev.ntziks.loyalty.clients.PromoClient;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;

public class VertxPromoClient implements PromoClient {
    private final WebClient client;
    private final int port;
    private final String host;

    public VertxPromoClient(WebClient client, String host, int port) {
        this.client = client;
        this.host = host;
        this.port = port;
    }

    @Override
    public Future<PromoResult> fetchPromo(String code) {
        Promise<PromoResult> p = Promise.promise();
        client.get(port, host, "/promo?code=" + code)
                .timeout(300L) // resilience: short timeout
                .send(ar -> {
                    if (ar.succeeded() && ar.result().statusCode() == 200) {
                        JsonObject body = ar.result().bodyAsJsonObject();
                        p.complete(new PromoResult(
                                body.getInteger("bonus"),
                                body.getBoolean("expiringSoon")
                        ));
                    } else {
                        // Resilience: treat failure/timeout as no promo
                        p.complete(new PromoResult(0, false));
                    }
                });
        return p.future();
    }
}

