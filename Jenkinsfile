def BRANCH = 'refactor'
pipeline {
  agent any
  tools {
        maven 'Maven 3.3.9'
		jdk 'jdk11'
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