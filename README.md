# Spring Boot Graph Processing Example

A sample application that demonstrates how to build a graph processing platform to create a ranking dashboard of influential Twitter profiles. [A guided tutorial](http://www.kennybastani.com) is provided with this sample project.

## Architecture

The diagram below shows each component and microservice as a part of this sample application. The connections are communication points between each service, describing what protocol is used.


![Twitter Crawler Architecture Diagram](http://i.imgur.com/Efdhofo.png)


The three Spring Boot applications that are colored in blue are stateless services. Stateless services will not attach a persistent backing service or need to worry about managing state locally. The Spring Boot application that is colored in green is the _Twitter Crawler_ service. Components that are colored in green will typically have an attached backing service. These backing services are responsible for managing state locally, and will either persist state to disk or in-memory.

### Spring Boot Services

- Ranking Dashboard
- Discovery Service
- Configuration Service
- Twitter Crawler

### Backing Services

- Neo4j (GraphDB)
- Hadoop (HDFS)
- Analysis Service (Apache Spark)
- RabbitMQ (AMQP)
- Twitter API (HTTP)

## Graph Processing Platform

The diagram below details the graph processing platform that is used in this sample project. This diagram is based on [Neo4j Mazerunner](http://www.github.com/neo4j-contrib/neo4j-mazerunner).

![Graph Processing Platform](http://i.imgur.com/VuQhIG8.png)

We can see from the diagram that new job requests are sent from Neo4j to RabbitMQ. Before Neo4j sends a message to RabbitMQ requesting a new job, it will export a graph replica to HDFS. The analysis service, which is the hexagon that is colored in purple, has an embedded standalone instance of Apache Spark, and will listen for messages from RabbitMQ containing new job requests. Each message that is received by the analysis service contains information about where the exported graph replica is stored on HDFS and what graph algorithm to execute.

After the analysis service has completed execution of a job, it sends a message to RabbitMQ that will be received by a listener on Neo4j. The message will contain a path on HDFS of the resulting graph that was saved by the analysis service. Neo4j will then import the results from HDFS back into the database without interrupting or impacting transactions that are being made by other database clients.

## How to get it up and running 

- Check out consumerKey and accessToken at [Twitter developer site](https://dev.twitter.com)
- Fill out Twitter properties on ```docker-compose.yml```
- If you are running it on a macOS system, reminder to change the fixed IPs in 
all ```application.yml``` files for ```192.168.99.100```, that is the default for docker-machine in the macOS
- Run the ```docker-compose up``` in your terminal console

## License

This library is licensed under the Apache License, Version 2.0.
