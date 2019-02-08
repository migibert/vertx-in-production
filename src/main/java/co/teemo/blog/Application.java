package co.teemo.blog;

import co.teemo.blog.verticles.MainVerticle;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.Slf4jReporter;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.dropwizard.DropwizardMetricsOptions;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class Application {

    public static void main(String[] args) {

        System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.SLF4JLogDelegateFactory");

        // Initialize metric registry
        String registryName = "registry";
        MetricRegistry registry = SharedMetricRegistries.getOrCreate(registryName);
        SharedMetricRegistries.setDefault(registryName);

        Slf4jReporter reporter = Slf4jReporter.forRegistry(registry)
                .outputTo(LoggerFactory.getLogger(Application.class))
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();
        reporter.start(1, TimeUnit.MINUTES);

        // Initialize vertx with the metric registry
        DropwizardMetricsOptions metricsOptions = new DropwizardMetricsOptions()
                .setEnabled(true)
                .setMetricRegistry(registry);
        VertxOptions vertxOptions = new VertxOptions().setMetricsOptions(metricsOptions);
        Vertx vertx = Vertx.vertx(vertxOptions);

        ConfigRetrieverOptions configRetrieverOptions = getConfigRetrieverOptions();
        ConfigRetriever configRetriever = ConfigRetriever.create(vertx, configRetrieverOptions);

        // getConfig is called for initial loading
        configRetriever.getConfig(
                ar -> {
                    int instances = Runtime.getRuntime().availableProcessors();
                    DeploymentOptions deploymentOptions =
                            new DeploymentOptions().setInstances(instances).setConfig(ar.result());
                    vertx.deployVerticle(MainVerticle.class, deploymentOptions);
                });

        // listen is called each time configuration changes
        configRetriever.listen(
                change -> {
                    JsonObject updatedConfiguration = change.getNewConfiguration();
                    vertx.eventBus().publish(EventBusChannels.CONFIGURATION_CHANGED.name(), updatedConfiguration);
                });
    }

    private static ConfigRetrieverOptions getConfigRetrieverOptions() {
        JsonObject classpathFileConfiguration = new JsonObject().put("path", "default.properties");
        ConfigStoreOptions classpathFile =
                new ConfigStoreOptions()
                        .setType("file")
                        .setFormat("properties")
                        .setConfig(classpathFileConfiguration);

        JsonObject envFileConfiguration = new JsonObject().put("path", "/etc/default/demo");
        ConfigStoreOptions envFile =
                new ConfigStoreOptions()
                        .setType("file")
                        .setFormat("properties")
                        .setConfig(envFileConfiguration)
                        .setOptional(true);

        JsonArray envVarKeys = new JsonArray();
        for (ConfigurationKeys key : ConfigurationKeys.values()) {
            envVarKeys.add(key.name());
        }
        JsonObject envVarConfiguration = new JsonObject().put("keys", envVarKeys);
        ConfigStoreOptions environment = new ConfigStoreOptions()
                .setType("env")
                .setConfig(envVarConfiguration)
                .setOptional(true);

        return new ConfigRetrieverOptions()
                .addStore(classpathFile) // local values : exhaustive list with sane defaults
                .addStore(environment)   // Container / PaaS friendly to override defaults
                .addStore(envFile)       // external file, IaaS friendly to override defaults and config hot reloading
                .setScanPeriod(5000);
    }
}
