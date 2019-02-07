package co.teemo.blog.handlers;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class FailureHandler implements Handler<RoutingContext> {

  private String message = "error";

  public void handle(RoutingContext context) {
    Throwable thrown = context.failure();
    recordError(thrown);
    context.response().setStatusCode(500).end(message);
  }

  public void setMessage(String message) {
    this.message = message;
  }

  private void recordError(Throwable thrown) {
    // TODO
  }
}
