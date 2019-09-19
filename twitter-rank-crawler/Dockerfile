FROM openjdk:8-jdk

# Add the service itself
ARG JAR_FILE
COPY target/${JAR_FILE} /app.jar
COPY credentials.json /credentials.json
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]