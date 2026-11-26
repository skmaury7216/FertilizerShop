
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

        stage('Prepare Version') {
            steps {
                script {
                    def nextVersion = sh(
                        script: '''
                            set -eu

                            VERSION_FILE="$JENKINS_HOME/fertilizershop-deployed-version.txt"

                            test -w "$JENKINS_HOME" || {
                                echo "ERROR: Jenkins home is not writable"
                                exit 1
                            }

                            if [ ! -f "$VERSION_FILE" ]; then
                                echo "1.0"
                            else
                                LAST_VERSION=$(cat "$VERSION_FILE")

                                echo "$LAST_VERSION" |
                                    grep -Eq '^[0-9]+[.][0-9]+$'

                                echo "$LAST_VERSION" |
                                    awk -F. '{print $1 "." ($2 + 1)}'
                            fi
                        ''',
                        returnStdout: true
                    ).trim()

                    if (!(nextVersion ==~ /^[0-9]+\\.[0-9]+$/)) {
                        error("Invalid deployment version: ${nextVersion}")
                    }

                    env.DEPLOY_VERSION = nextVersion

                    echo "Preparing version V ${env.DEPLOY_VERSION}"
                }
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

                    docker network inspect \
                        "$DOCKER_NETWORK" >/dev/null

                    docker inspect \
                        -f '{{json .NetworkSettings.Networks}}' \
                        "$MYSQL_CONTAINER" |
                        grep -q "$DOCKER_NETWORK"

                    echo "MySQL container and network are ready"
                '''
            }
        }

        stage('Validate Deployment Settings') {
            steps {
                sh '''
                    set -eu

                    test -n "${DEPLOY_VERSION:-}" || {
                        echo "ERROR: DEPLOY_VERSION is missing"
                        exit 1
                    }

                    echo "$DEPLOY_VERSION" |
                        grep -Eq '^[0-9]+[.][0-9]+$'

                    docker image inspect "$IMAGE_NAME" >/dev/null

                    echo "Deployment settings valid: V $DEPLOY_VERSION"
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

                        # Remove only the application container.
                        # MySQL and its volumes remain untouched.
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
                            -e SHOP_APP_VERSION="$DEPLOY_VERSION" \
                            "$IMAGE_NAME"

                        echo "FertilizerShop V $DEPLOY_VERSION started"
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

        stage('Record Successful Version') {
            steps {
                sh '''
                    set -eu

                    VERSION_FILE="$JENKINS_HOME/fertilizershop-deployed-version.txt"

                    TEMP_FILE=$(mktemp \
                        "$JENKINS_HOME/.fertilizershop-version.XXXXXX")

                    printf '%s\\n' "$DEPLOY_VERSION" > "$TEMP_FILE"

                    mv -f "$TEMP_FILE" "$VERSION_FILE"

                    echo "Successfully recorded V $DEPLOY_VERSION"
                '''
            }
        }
    }

    post {
        success {
            echo "FertilizerShop CI/CD SUCCESS: V ${env.DEPLOY_VERSION}"
        }

        failure {
            echo 'FertilizerShop CI/CD FAILED'
        }
    }
}
