#!/usr/bin/env sh
set -x

function setup() {
    mkdir -p $(pwd -P)/integration-test/aws-volume
    docker run --rm -ti -v $(pwd -P)/integration-test/aws-volume:/root \
      --env AWS_ACCESS_KEY_ID=$AWS_ACCESS_KEY_ID \
      --env AWS_SECRET_ACCESS_KEY=$AWS_SECRET_ACCESS_KEY \
      --env PROJECT_NAME=$PROJECT_NAME \
      --env EC2_INSTANCE_TYPE=$EC2_INSTANCE_TYPE \
      --name=aws kbastani/docker-run-ec2 \
      sh ./aws-create-instance.sh
}

function tearDown() {
    docker run --rm -ti -v $(pwd -P)/integration-test/aws-volume:/root \
      --env AWS_ACCESS_KEY_ID=$AWS_ACCESS_KEY_ID \
      --env AWS_SECRET_ACCESS_KEY=$AWS_SECRET_ACCESS_KEY \
      --env PROJECT_NAME=$PROJECT_NAME \
      --name=aws kbastani/docker-run-ec2 \
      sh ./aws-delete-instance.sh
}

function run() {
    docker run --rm -ti -v $(pwd -P)/integration-test/aws-volume:/root \
      --env AWS_ACCESS_KEY_ID=$AWS_ACCESS_KEY_ID \
      --env AWS_SECRET_ACCESS_KEY=$AWS_SECRET_ACCESS_KEY \
      --env PROJECT_NAME=$PROJECT_NAME \
      --env DOCKER_COMMAND="$1" \
      --name=aws kbastani/docker-run-ec2 \
      sh ./ssh-docker-run.sh

    # Continue until the health check connection is ready
    if [ $2 ]; then
        while [ "$HEALTH_CHECK_READY" = "" ]; do
          echo "Creating container..."
          HEALTH_CHECK_READY=$(curl -s "$PUBLIC_IP:$2/$3"; echo);
          sleep 1
        done
    fi
}

function health_check() {
    HEALTH_CHECK_READY=
    while [ "$HEALTH_CHECK_READY" = "" ]; do
      echo "Creating container..."
      HEALTH_CHECK_READY=$(curl -s "$PUBLIC_IP:$1/$2"; echo);
      sleep 1
    done
}

function compose() {
    scp -i $INSTALL_PATH/aws-volume/$PROJECT_NAME.pem -o StrictHostKeyChecking=no $INSTALL_PATH/docker-compose.yml ec2-user@$PUBLIC_IP:/home/ec2-user

    # Run a docker compose command
    ssh -tt -i $INSTALL_PATH/aws-volume/$PROJECT_NAME.pem -o StrictHostKeyChecking=no ec2-user@$PUBLIC_IP "docker-compose $1"
}

# Create the EC2 instance
setup

# Export EC2 public IP
export PUBLIC_IP="$(cat $(pwd -P)/integration-test/aws-volume/public_ip | sed 's/\( -\)//1')"

compose "up -d"

declare -a ports=("7474" "8761/health" "8888/admin/health")
# Do health checks on compose services
for i in "${ports[@]}"
do
   health_check "$i"
done
