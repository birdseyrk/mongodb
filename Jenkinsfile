pipeline {
  agent any

  environment {
    JAR_GLOB = "target/mongo-hybrid-events-1.0.0.jar"
  }

  stages {
    stage('Checkout') {
      steps { checkout scm }
    }

    stage('Build') {
      steps {
        sh 'mvn -v'
        sh 'mvn -q clean package -DskipTests'
      }
    }

    stage('Unit Tests') {
      steps {
        // Requires Docker if using Testcontainers. If not available, change to -DskipTests.
        sh 'mvn -q test'
      }
    }

    stage('Deploy via Ansible') {
      steps {
        withCredentials([
          sshUserPrivateKey(credentialsId: 'linux-deploy-ssh', keyFileVariable: 'SSH_KEY', usernameVariable: 'SSH_USER'),
          string(credentialsId: 'mongo_password', variable: 'MONGO_PASSWORD')
        ]) {
          sh '''
            set -e
            cd ansible

            ansible --version
            ansible-playbook -i inventory/prod.ini playbooks/deploy.yml \
              --private-key "$SSH_KEY" \
              -u "$SSH_USER" \
              --extra-vars "mongo_password=$MONGO_PASSWORD jar_path=../target/mongo-hybrid-events-1.0.0.jar"
          '''
        }
      }
    }
  }

  post {
    always {
      archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
    }
  }
}
