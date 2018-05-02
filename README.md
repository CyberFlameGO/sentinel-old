![FredBoat](https://fred.moe/YY1.png)
# Sentinel [![TeamCity (full build status)](https://img.shields.io/teamcity/https/ci.fredboat.com/e/Sentinel_Build.svg?style=flat-square)](https://ci.fredboat.com/viewType.html?buildTypeId=Sentinel_Build&guest=1) [![Docker Pulls](https://img.shields.io/docker/pulls/fredboat/sentinel.svg)](https://fredboat.com/docs/selfhosting) [![Docker Layers](https://images.microbadger.com/badges/image/fredboat/sentinel:dev-v0.svg)](https://microbadger.com/images/fredboat/sentinel:dev-v0 "Get your own image badge on microbadger.com") [![Docker Version](https://images.microbadger.com/badges/version/fredboat/sentinel:dev-v0.svg)](https://microbadger.com/images/fredboat/sentinel:dev-v0 "Get your own version badge on microbadger.com")
Sentinel is still WIP.

Sentinel is a gateway meant to be used between Discord and FredBoat.

Sentinel uses JDA as its Discord library, and communicates with FredBoat via RabbitMQ.
Sentinel primarily forwards events to FredBoat, but also exposes RPC functions for things like requesting guild info.
Entities are primarily stored on Sentinel. FredBoat only keeps a short-lived cache of guilds, for which Sentinel sends
invalidation notifications when they change.

Both Sentinel and FredBoat use the module `sentinel-shared`, which contains entities, requests and responses.
This module can be considered to be a schema.

Sentinel is meant to scale horizontally, with each instance taking up a range of shards. This will eventually be
supported by using RabbitMQ routing keys. 

## How to run Sentinel
There are two prerequisites for running Sentinel:

1. A RabbitMQ server we can connect to as guest (default server config)
2. A valid `sentinel.yaml`, usually in the working directory. An example is provided in this repo.

Then just run the program, for instance with `java -jar sentinel.jar`
