
pipeline {
    agent any

    triggers {
        pollSCM('H/3 * * * *')
    }

    options {
        disableConcurrentBuilds()
        timestamps()
    }

    environment {
        JAVA_HOME = '/usr/lib/jvm/java-17-openjdk-amd64'
        PATH = "${JAVA_HOME}/bin:${env.PATH}"

        IMAGE_NAME = 'fertilizershop:latest'
        CONTAINER_NAME = 'fertilizershop'
        MYSQL_CONTAINER = 'fertilizershop-mysql'
        DOCKER_NETWORK = 'fertilizershop-network'

        DB_URL = 'jdbc:mysql://fertilizershop-mysql:3306/fertilizershop_db'
        DB_USER = 'fertilizershop_user'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build and Test') {
            steps {
                sh 'mvn -B clean verify'
            }
        }

        stage('Build Docker Image') {
            steps {
                sh '''
                    set -eu
                    docker build -t "$IMAGE_NAME" .
                '''
            }
        }

        stage('Check MySQL Database') {
            steps {
                sh '''
                    set -eu

                    docker inspect \
                      -f '{{.State.Running}}' \
                      "$MYSQL_CONTAINER" | grep -qx true

                    docker network inspect "$DOCKER_NETWORK" \
                      >/dev/null

                    docker inspect \
                      -f '{{json .NetworkSettings.Networks}}' \
                      "$MYSQL_CONTAINER" |
                      grep -q "$DOCKER_NETWORK"

                    echo "MySQL container and Docker network are ready"
                '''
            }
        }

        stage('Deploy on Ubuntu VM') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'fertilizershop-admin',
                        usernameVariable: 'SHOP_USER',
                        passwordVariable: 'SHOP_PASS'
                    ),
                    string(
                        credentialsId: 'fertilizershop-db-password',
                        variable: 'MYSQL_PASS'
                    )
                ]) {
                    sh '''
                        set +x
                        set -eu

                        export DB_PASSWORD="$MYSQL_PASS"
                        export SHOP_ADMIN_USER="$SHOP_USER"
                        export SHOP_ADMIN_PASSWORD="$SHOP_PASS"

                        docker rm -f "$CONTAINER_NAME" \
                          2>/dev/null || true

                        docker run -d \
                          --name "$CONTAINER_NAME" \
                          --restart unless-stopped \
                          --network "$DOCKER_NETWORK" \
                          -p 127.0.0.1:8090:8080 \
                          -e DB_URL \
                          -e DB_USER \
                          -e DB_PASSWORD \
                          -e SHOP_ADMIN_USER \
                          -e SHOP_ADMIN_PASSWORD \
                          -e SHOP_DEMO_DATA=false \
                          "$IMAGE_NAME"

                        echo "FertilizerShop deployment started"
                    '''
                }
            }
        }

        stage('Verify') {
            steps {
                sh '''
                    set -eu

                    for i in $(seq 1 40); do
                        if curl -fsS -o /dev/null \
                          http://127.0.0.1:8090/login; then

                            echo "FertilizerShop is responding"
                            docker ps \
                              --filter name=^/fertilizershop$ \
                              --format "table {{.Names}}\\t{{.Status}}\\t{{.Ports}}"
                            exit 0
                        fi

                        sleep 3
                    done

                    echo "Deployment verification failed"
                    docker logs --tail 80 fertilizershop
                    exit 1
                '''
            }
        }
    }

    post {
        success {
            echo 'FertilizerShop CI/CD SUCCESS'
        }

        failure {
            echo 'FertilizerShop CI/CD FAILED'
        }
    }
}
