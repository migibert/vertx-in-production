package co.teemo.blog.handlers;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class GreetingHandler implements Handler<RoutingContext> {

    @Override
    public void handle(RoutingContext routingContext) {
        // Thanks to the validation handler, we are sure required parameters are present
        String name = routingContext.request().getParam("name");
        String authorization = routingContext.request().getHeader("Authorization");
        int version = Integer.valueOf(routingContext.request().getHeader("Version"));

        String response = String.format("Hello %s, you are using version %d of this api and authenticated with %s", name, version, authorization);
        routingContext.response().setStatusCode(200).end(response);
    }
}
