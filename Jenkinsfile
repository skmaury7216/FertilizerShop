pipeline {
  agent any
  triggers { pollSCM('H/3 * * * *') }
  options { disableConcurrentBuilds(); timestamps() }
  environment {
    JAVA_HOME = '/usr/lib/jvm/java-17-openjdk-amd64'
    PATH = "${JAVA_HOME}/bin:${env.PATH}"
  }
  stages {
    stage('Checkout') { steps { checkout scm } }
    stage('Build and Test') { steps { sh 'mvn -B clean verify' } }
    stage('Build Docker Image') { steps { sh 'docker build -t fertilizershop:latest .' } }
    stage('Deploy on Ubuntu VM') {
      steps {
        withCredentials([usernamePassword(credentialsId: 'fertilizershop-admin', usernameVariable: 'SHOP_USER', passwordVariable: 'SHOP_PASS')]) {
          sh '''
            set +x
            docker rm -f fertilizershop 2>/dev/null || true
            docker run -d --name fertilizershop --restart unless-stopped \
              -p 127.0.0.1:8090:8080\
              -v fertilizershop_data:/app/data \
              -e SHOP_ADMIN_USER="$SHOP_USER" \
              -e SHOP_ADMIN_PASSWORD="$SHOP_PASS" \
              fertilizershop:latest
          '''
        }
      }
    }
    stage('Verify') {
      steps {
        sh '''
          for i in $(seq 1 30); do
            if curl -fsS -o /dev/null http://127.0.0.1:8090/login; then
              echo 'FertilizerShop deployment responds on port 8090'
              exit 0
            fi
            sleep 2
          done
          docker logs --tail 80 fertilizershop
          exit 1
        '''
      }
    }
  }
}
