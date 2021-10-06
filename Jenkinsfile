def BRANCH = 'refactor'
pipeline {
  agent any
  tools {
        maven 'mvn3.8.3'
		jdk 'jdk11'
  }
  stages {
    stage('build') {
      steps {
		  script {
			sh 'java -version'
			sh 'mvn -version'
			sh 'mvn clean install -Dmaven.test.failure.ignore=true'
			}
      }
    }

  }
}