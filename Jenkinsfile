def BRANCH = 'refactor'
pipeline {
  agent any
  tools {
        maven 'Default'
  }
  stages {
    stage('build') {
      steps {
		  script {
			sh 'java -version'
			sh 'mvn -version'
			sh 'mvn clean install'
			}
      }
    }

  }
}