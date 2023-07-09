# Overview

<head>
  <meta charset="UTF-8" />
  <meta name="description" content="ZIO Temporal serialization" />
  <meta name="keywords" content="ZIO Temporal serialization, Scala Temporal serialization" />
</head>

Temporal applications communicate with the orchestrator - the Temporal Cluster.  
The Worker process (running workflow) and the client process (invoking workflows) may be different processes running on different machines.  
While invoking workflows & activities may look like simple method invocation, it's a remote procedure call over the network.  
Therefore, ZIO-Temporal (if more precisely, the Java SDK) serializes workflow & activity method parameters before sending them over network.

ZIO-Temporal provides two default ways to serialize data:
- As JSON objects using [Jackson](https://github.com/FasterXML/jackson-module-scala) library
- In Protobuf binary format using [Scalapb](https://scalapb.github.io/) library
