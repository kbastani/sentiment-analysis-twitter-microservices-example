#!/usr/bin/env sh
set -x

function tearDown() {
    docker run --rm -ti -v $(pwd -P)/integration-test/aws-volume:/root \
      --env AWS_ACCESS_KEY_ID=$AWS_ACCESS_KEY_ID \
      --env AWS_SECRET_ACCESS_KEY=$AWS_SECRET_ACCESS_KEY \
      --env PROJECT_NAME=$PROJECT_NAME \
      --name=aws kbastani/docker-run-ec2 \
      sh ./aws-delete-instance.sh
}

# Export EC2 public IP
export PUBLIC_IP="$(cat $(pwd -P)/integration-test/aws-volume/public_ip | sed 's/\( -\)//1')"

set -e

export SPRING_NEO4J_HOST=$PUBLIC_IP
export SPRING_RABBITMQ_HOST=$PUBLIC_IP
export EUREKA_CLIENT_SERVICEURL_DEFAULTZONE="http://$PUBLIC_IP:8761/eureka/"
export SPRING_CLOUD_CONFIG_URI="http://$PUBLIC_IP:8888"

# Run tests and tear down
mvn clean install || tearDown || die "'mvn clean install' failed" 1

tearDown