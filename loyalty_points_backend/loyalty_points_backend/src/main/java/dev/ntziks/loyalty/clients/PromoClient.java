package dev.ntziks.loyalty.clients;

import io.vertx.core.Future;

public interface PromoClient {
    Future<PromoResult> fetchPromo(String code);

    record PromoResult(int bonus, boolean expiringSoon) { }
}

