# Sentiment Analysis Twitter Microservices Example

A sample application that demonstrates how to build a graph processing platform to analyze sources of emotional influence on Twitter. [A guided tutorial](http://www.kennybastani.com) is provided with this sample project.

This repository is actively being worked on and has not yet transformed into its final state. Please check back periodically to see updates. If you have trouble running the example, please post to the issue tracker.

## Architecture

The diagram below shows each component and microservice as a part of this sample application. The connections are communication points between each service, describing what protocol is used.

![Twitter Crawler Architecture Diagram](https://i.imgur.com/Jp4I1tp.png)

The two Spring Boot applications that are colored in blue are stateless services. Stateless services will not attach a persistent backing service or need to worry about managing state locally.

The Spring Boot application that is colored in green is the _Twitter Crawler_ service. Components that are colored in green will typically have an attached backing service. These backing services are responsible for managing state locally, and will either persist state to disk or in-memory.

The services colored in red are external APIs that are used to collect data and to run sentiment analysis and other natural language machine learning algorithms.

### Spring Boot Services

- Rank Dashboard
- Discovery Service
- Configuration Service
- Twitter Crawler

### Backing Services

- Neo4j (BOLT)
- RabbitMQ (AMQP)
- Twitter API (HTTP)
- Google Cloud Language API (HTTP)

## Graph Processing Platform

Neo4j is a graph database that includes graph processing algorithms from a community plugin called [APOC](https://neo4j.com/developer/graph-algorithms/).

## Graph Data Model

The graph data model in Neo4j will be created using the following diagram.

![Twitter Graph Data Model](https://i.imgur.com/U1eK3vi.png)

### Sentiment Analysis

When Twitter data is imported, a user's tweets will be analyzed using the GCP Natural Language API.

![Twitter Graph Data Model](https://i.imgur.com/LkdSk6p.png)

### Category

Categories will be inferred over time by analyzing the top ranked phrases and submitting the group of tweets as a document to GCP's classification API.

![Twitter Graph Example Model](https://i.imgur.com/6yJTJuE.png)


## External APIs

To be able to run the example, you'll need to provide API authorization for both the GCP NLP API and Twitter Developer API.

- Check out consumerKey and accessToken at the [Twitter developer site](https://dev.twitter.com)
- Check out GCP token authorization file at [GCP developer documentation](https://cloud.google.com/docs/authentication/production)
- Fill out Twitter properties on `docker-compose.yaml`
- Add your GCP authorization to `twitter-rank-crawler/credentials.json`
 - _Please do not check-in your private secrets to public GitHub!_
- Run `mvn clean install -DskipTests` in your terminal with Docker running
- Run `docker-compose up` in your terminal console

## License

This library is an open source product licensed under Apache License 2.0.
