package dev.ntziks.loyalty.adapters;

import dev.ntziks.loyalty.clients.FxClient;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.ext.web.client.WebClient;

public class VertxFxClient implements FxClient {
    private final WebClient client;
    private final int port;
    private final String host;

    public VertxFxClient(WebClient client, String host, int port) {
        this.client = client;
        this.host = host;
        this.port = port;
    }

    @Override
    public Future<Double> fetchRate(String currency) {
        Promise<Double> p = Promise.promise();
        client.get(port, host, "/fx?currency=" + currency)
                .send(ar -> {
                    if (ar.succeeded() && ar.result().statusCode() == 200) {
                        double rate = ar.result().bodyAsJsonObject().getDouble("rate");
                        p.complete(rate);
                    } else {
                        p.fail(ar.cause() != null ? ar.cause() : new RuntimeException("FX error"));
                    }
                });
        return p.future();
    }
}
