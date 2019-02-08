package co.teemo.blog.verticles;

import co.teemo.blog.ConfigurationKeys;
import co.teemo.blog.EventBusChannels;
import co.teemo.blog.handlers.FailureHandler;
import co.teemo.blog.handlers.GreetingHandler;
import co.teemo.blog.handlers.PingHandler;
import co.teemo.blog.handlers.PokemonHandler;
import co.teemo.blog.security.NameValidator;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.healthchecks.HealthCheckHandler;
import io.vertx.ext.healthchecks.HealthChecks;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.api.validation.HTTPRequestValidationHandler;
import io.vertx.ext.web.api.validation.ParameterType;
import io.vertx.ext.web.handler.BodyHandler;


public class MainVerticle extends AbstractVerticle {

    private static final Logger logger = LoggerFactory.getLogger(MainVerticle.class);

    private PingHandler pingHandler;
    private FailureHandler failureHandler;
    private PokemonHandler pokemonHandler;
    private GreetingHandler greetingHandler;

    @Override
    public void start() {
        this.pingHandler = new PingHandler();
        this.failureHandler = new FailureHandler();
        this.pokemonHandler = new PokemonHandler(vertx);
        this.greetingHandler = new GreetingHandler();

        configureRouteHandlers(config());
        configureEventBus();
        HealthChecks healthChecks = pokemonHandler.getHealthchecks();

        HTTPRequestValidationHandler greetingValidationHandler = HTTPRequestValidationHandler
                .create()
                .addHeaderParam("Authorization", ParameterType.GENERIC_STRING, true)
                .addHeaderParam("Version", ParameterType.INT, true)
                .addPathParamWithCustomTypeValidator("name", new NameValidator(), false);

        Router router = Router.router(vertx);
        router.route().consumes("application/json");
        router.route().produces("application/json");
        router.route().handler(BodyHandler.create());

        router.get("/ping").handler(pingHandler).failureHandler(failureHandler);
        router.get("/pokemons").handler(pokemonHandler).failureHandler(failureHandler);

        router.get("/greetings/:name")
                .handler(greetingValidationHandler)
                .handler(greetingHandler)
                .failureHandler(failureHandler);

        router.get("/alive").handler(HealthCheckHandler.create(vertx));
        router.get("/healthy").handler(HealthCheckHandler.createWithHealthChecks(healthChecks));

        vertx.createHttpServer().requestHandler(router).listen(8080);
    }

    private void configureEventBus() {
        vertx
                .eventBus()
                .<JsonObject>consumer(
                        EventBusChannels.CONFIGURATION_CHANGED.name(),
                        message -> {
                            logger.debug("Configuration has changed, verticle {} is updating...", deploymentID());
                            configureRouteHandlers(message.body());
                            logger.debug(
                                    "Configuration has changed, verticle {} has been updated...", deploymentID());
                        });
    }

    private void configureRouteHandlers(JsonObject configuration) {
        String pingResponse = configuration.getString(ConfigurationKeys.PING_RESPONSE.name());
        pingHandler.setMessage(pingResponse);

        String pokeApiHost = configuration.getString(ConfigurationKeys.POKE_API_HOST.name());
        int pokeApiPort = configuration.getInteger(ConfigurationKeys.POKE_API_PORT.name());
        String pokeApiPath = configuration.getString(ConfigurationKeys.POKE_API_PATH.name());
        pokemonHandler.setPokeApiUrl(pokeApiHost, pokeApiPort, pokeApiPath);
    }
}
