package dev.ntziks.loyalty.clients;

import io.vertx.core.Future;

public interface FxClient {
    Future<Double> fetchRate(String currency);
}
