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
		  bat 'java -version'
		  bat 'mvn -version'
		  bat 'mvn clean install -Dmaven.test.failure.ignore=true'
	  }
    }
    stage('unit tests') {
	  steps {
		  bat 'mvn test'
	  }
    }
  }
}