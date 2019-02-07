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

`echo "PING_RESPONSE=pong from environment file" >>  /etc/default/demo`

`curl http://localhost:8080/ping`

`> pong from environment file`

# Design for failure

## Ideas

Any remote system you depend on can fail, whatever it is an API, a database, a messaging system, a remote configuration one...

## Rules

Implement a circuit breaker for each external dependency to fail faster and limit cascading failure risks.

## Testing

Call pokemon api : `curl localhost:8080/pokemons | jq`

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

Simulate network latency using traffic control

`tc qdisc add dev eth0 root netem delay 2000ms`

And call the API a few times to observe the circuit breaker opening

`for i in {1..100}; do curl localhost:8080/pokemons & done`

Then observe that calling the API returns the fallback value (an empty list).

`curl localhost:8080/pokemons`

`> []`

If you wait, you will see the breaker move to half opened state.

Let's remove the latency to simulate a remote service recovery.

`tc qdisc del dev eth0 root netem`

Now you will see that calling the API again will close the circuit breaker and display values from remote service.

`curl localhost:8080/pokemons | jq`

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

# Observability

## Healthchecks

Two endpoints are exposed to check application health:
* /alive to see if the process is running
* /healthy to see if the application runs in optimal conditions or degraded mode
