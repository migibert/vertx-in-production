This repository contains demo code to showcase principles written in https://blog.teemo.co/vertx-in-production-d5ca9e89d7c6 

# Configuration

## Ideas

Standard strategy for configuration retriever :

* read default configuration from classpath (src/main/resources/default.properties)
* read variables from environment (VARIABLE=... java -jar myjar)
* read variables from system (java -DVARIABLE=... -jar myjar)
* read variables from environment file (/etc/default/myservice)
* read variable from remote configuration system (GCP Runtime Config, Consul, Redis, ...)

## Rules

* Make any store that is not local to the binary optional.
* Give sane values to default configurations.
* Define exhaustive list of configuration keys in default configurations, eventually with no value if it is not possible.

## Testing

A demo endpoint is implement at /ping route.

It answers "Pong from default" if configuration has been read from default.properties, else it is what you defined.

`curl http://localhost:8080/ping`

`> pong from defaults`

Then write a configuration file to override defaults and wait for the propagation (set to 5s in demo app)

```
    echo "PING_RESPONSE=pong from environment file" >>  /etc/default/demo
    curl http://localhost:8080/ping
    > pong from environment file
```

# Request Validations

## Ideas

User input cannot bu trusted so they always need to be validated.

## Rules

The most basic validations are:
* Required parameters are present
* Parameters types are what we expect

In addition, business rules can be applied to validate them (phone number validation, geolocation restriction...)

## Testing

The /greetings/name route uses standard handler with business logic and failure handler but also an additional handler to validate requests.

The validation handler checks that :
* Authentication header is present
* Version header is present and is an integer
* name path parameter is present, begins with an uppercase character and is composed with alphabetical characters

```
    curl -i -H 'Authorization: toto' -H 'Version: 1'  http://localhost:8080/greetings/fds
    > HTTP/1.1 400 Bad Request
    > content-length: 38
    > Name must start with an uppercase char
```


# Design for failure

## Ideas

Any remote system you depend on can fail, whatever it is an API, a database, a messaging system, a remote configuration one...

## Rules

Implement a circuit breaker for each external dependency to fail faster and limit cascading failure risks.

## Testing

Call pokemon api : `curl localhost:8080/pokemons | jq`

<details>
<summary>Response</summary>

```
[
  {
    "name": "bulbasaur",
    "url": "https://pokeapi.co/api/v2/pokemon/1/"
  },
  {
    "name": "ivysaur",
    "url": "https://pokeapi.co/api/v2/pokemon/2/"
  },
  {
    "name": "venusaur",
    "url": "https://pokeapi.co/api/v2/pokemon/3/"
  },
  {
    "name": "charmander",
    "url": "https://pokeapi.co/api/v2/pokemon/4/"
  },
  {
    "name": "charmeleon",
    "url": "https://pokeapi.co/api/v2/pokemon/5/"
  },
  {
    "name": "charizard",
    "url": "https://pokeapi.co/api/v2/pokemon/6/"
  },
  {
    "name": "squirtle",
    "url": "https://pokeapi.co/api/v2/pokemon/7/"
  },
  {
    "name": "wartortle",
    "url": "https://pokeapi.co/api/v2/pokemon/8/"
  },
  {
    "name": "blastoise",
    "url": "https://pokeapi.co/api/v2/pokemon/9/"
  },
  {
    "name": "caterpie",
    "url": "https://pokeapi.co/api/v2/pokemon/10/"
  },
  {
    "name": "metapod",
    "url": "https://pokeapi.co/api/v2/pokemon/11/"
  },
  {
    "name": "butterfree",
    "url": "https://pokeapi.co/api/v2/pokemon/12/"
  },
  {
    "name": "weedle",
    "url": "https://pokeapi.co/api/v2/pokemon/13/"
  },
  {
    "name": "kakuna",
    "url": "https://pokeapi.co/api/v2/pokemon/14/"
  },
  {
    "name": "beedrill",
    "url": "https://pokeapi.co/api/v2/pokemon/15/"
  },
  {
    "name": "pidgey",
    "url": "https://pokeapi.co/api/v2/pokemon/16/"
  },
  {
    "name": "pidgeotto",
    "url": "https://pokeapi.co/api/v2/pokemon/17/"
  },
  {
    "name": "pidgeot",
    "url": "https://pokeapi.co/api/v2/pokemon/18/"
  },
  {
    "name": "rattata",
    "url": "https://pokeapi.co/api/v2/pokemon/19/"
  },
  {
    "name": "raticate",
    "url": "https://pokeapi.co/api/v2/pokemon/20/"
  }
]
```
</details>

Simulate network latency using traffic control

`tc qdisc add dev eth0 root netem delay 2000ms`

And call the API a few times to observe the circuit breaker opening

`for i in {1..100}; do curl localhost:8080/pokemons & done`

Then observe that calling the API returns the fallback value (an empty list).

```
    curl localhost:8080/pokemons
    > []
```

If you wait, you will see the breaker move to half opened state.

Let's remove the latency to simulate a remote service recovery.

`tc qdisc del dev eth0 root netem`

Now you will see that calling the API again will close the circuit breaker and display values from remote service.

`curl localhost:8080/pokemons | jq`

<details>
<summary>Response</summary>
```
[
  {
    "name": "bulbasaur",
    "url": "https://pokeapi.co/api/v2/pokemon/1/"
  },
  {
    "name": "ivysaur",
    "url": "https://pokeapi.co/api/v2/pokemon/2/"
  },
  {
    "name": "venusaur",
    "url": "https://pokeapi.co/api/v2/pokemon/3/"
  },
  {
    "name": "charmander",
    "url": "https://pokeapi.co/api/v2/pokemon/4/"
  },
  {
    "name": "charmeleon",
    "url": "https://pokeapi.co/api/v2/pokemon/5/"
  },
  {
    "name": "charizard",
    "url": "https://pokeapi.co/api/v2/pokemon/6/"
  },
  {
    "name": "squirtle",
    "url": "https://pokeapi.co/api/v2/pokemon/7/"
  },
  {
    "name": "wartortle",
    "url": "https://pokeapi.co/api/v2/pokemon/8/"
  },
  {
    "name": "blastoise",
    "url": "https://pokeapi.co/api/v2/pokemon/9/"
  },
  {
    "name": "caterpie",
    "url": "https://pokeapi.co/api/v2/pokemon/10/"
  },
  {
    "name": "metapod",
    "url": "https://pokeapi.co/api/v2/pokemon/11/"
  },
  {
    "name": "butterfree",
    "url": "https://pokeapi.co/api/v2/pokemon/12/"
  },
  {
    "name": "weedle",
    "url": "https://pokeapi.co/api/v2/pokemon/13/"
  },
  {
    "name": "kakuna",
    "url": "https://pokeapi.co/api/v2/pokemon/14/"
  },
  {
    "name": "beedrill",
    "url": "https://pokeapi.co/api/v2/pokemon/15/"
  },
  {
    "name": "pidgey",
    "url": "https://pokeapi.co/api/v2/pokemon/16/"
  },
  {
    "name": "pidgeotto",
    "url": "https://pokeapi.co/api/v2/pokemon/17/"
  },
  {
    "name": "pidgeot",
    "url": "https://pokeapi.co/api/v2/pokemon/18/"
  },
  {
    "name": "rattata",
    "url": "https://pokeapi.co/api/v2/pokemon/19/"
  },
  {
    "name": "raticate",
    "url": "https://pokeapi.co/api/v2/pokemon/20/"
  }
]
```
</details>

# Observability

## Healthchecks

### Ideas

Applications should always expose their health state through dedicated endpoints.

It will let alerting & monitoring systems track if our application is alive or not and notify the team if necessary.

### Rules

Expose two endpoints for each application:
* /alive to see if the process is running
* /healthy to see if the application runs in optimal conditions or degraded mode

### Testing

Two endpoints are exposed to check application health:
* /alive answers always OK
* /healthy answers OK if all circuit breakers are closed, KO else

## Logs

### Ideas

Logs are the most precise information we have on an error. It gives context, error cause, and everything we need to answer what happens.

They have to contain the whole context, including a correlation id to be truly useful.

For instance, on GCP you can use [this configuration](https://github.com/GoogleCloudPlatform/java-docs-samples/blob/master/logging/logback/src/main/resources/logback.xml)

### Rules

Four rules to power up the logs:
* Centralize them into one place
* Use a format easy to export and to query like Json
* Include static runtime metadata like host, zone, application namen, version, ...
* Include dynamic runtime metadata like correlation id, user id, session id, ...

### Testing

See that logs are output in Json format. To keep this repo easily testable we do not tread centralized logs here.

Static Metadata are handled as MDC labels:
* application name
* version
* release (canary or stable)
* hostname

Observe each logging message includes a metadata with your hostname.

## Metrics

### Ideas

Metrics allow us to measure what is happening in our application and how it moves in time.

They need to be tagged with static and dynamic metadata, just like logs.

Static metadata let us see differences between environment, canary analysis, version regressions etc...

Dynamic metadata let us break down a number (A/B testing, users per geographical zone, etc...)

### Rules

Expose 4 levels of metrics:

*System level*

Things like CPU, RAM or Disk space. They are often reported by a system agent.

*Middleware level*

Things like JVM, Threads or Queues size. They are often reporter by the middleware or a framework.

*Application level*

Things like Connected users, errors breakdown. They are reported directly by the application using Metrics frameworks.

*Business level*

Things like spent money / user or connection frequency. They are reported by the application or by data analytics systems.

### Testing

Dropwizard Metrics is configured to report metrics to Console as an example. Exported metrics are:
* Middleware metrics: verticles, event bus, servers, ..., find exhaustive list [here](https://vertx.io/docs/vertx-dropwizard-metrics/java/#_the_metrics)
* Application metrics: number of validation errors

A Dropwizard Reporter is configured to report metrics to log each minute. Take a look and observe their evolution!

Also note that static metadata are added to any message (application name, version, release and host).

Unfortunately, Dropwizard metrics in the current version does not support labels so we cannot add dynamic metadata to our metrics like correlation id etc...

See [this thread](https://groups.google.com/forum/#!topic/vertx/9rHzX3vVg8Y) for more information/

As a workaround, I added the dynamic metadata directly in log messages.
