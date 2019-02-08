package co.teemo.blog.handlers;

import com.codahale.metrics.Counter;
import com.codahale.metrics.SharedMetricRegistries;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.api.validation.ValidationException;

public class FailureHandler implements Handler<RoutingContext> {

    private final Counter validationErrorsCounter;
    private static final Logger logger = LoggerFactory.getLogger(FailureHandler.class);

    public FailureHandler() {
        validationErrorsCounter = SharedMetricRegistries.getDefault().counter("validationErrors");
    }

    public void handle(RoutingContext context) {

        Throwable thrown = context.failure();
        String userId = context.request().getHeader("Authorization");
        recordError(userId, thrown);

        if(thrown instanceof ValidationException) {
            context.response().setStatusCode(400).end(thrown.getMessage());
        } else {
            context.response().setStatusCode(500).end(thrown.getMessage());
        }
    }

    private void recordError(String userId, Throwable thrown) {
        String dynamicMetadata = "";
        if(userId != null) {
            dynamicMetadata = String.format("userId=%s ", userId);
        }

        validationErrorsCounter.inc();
        logger.error(dynamicMetadata + thrown.getMessage());
    }
}
