package co.teemo.blog.handlers;

import com.codahale.metrics.Counter;
import com.codahale.metrics.SharedMetricRegistries;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class FailureHandler implements Handler<RoutingContext> {

    private final Counter validationErrorsCounter;

    public FailureHandler() {
        validationErrorsCounter = SharedMetricRegistries.getDefault().counter("validationErrors");
    }

    public void handle(RoutingContext context) {
        Throwable thrown = context.failure();
        recordError(thrown);
        context.response().setStatusCode(500).end(thrown.getMessage());
    }

    private void recordError(Throwable thrown) {
        validationErrorsCounter.inc();
    }
}
