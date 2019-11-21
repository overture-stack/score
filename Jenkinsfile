import groovy.json.JsonOutput

def version = "UNKNOWN"
def commit = "UNKNOWN"
def repo = "UNKNOWN"

def pom(path, target) {
    return [pattern: "${path}/pom.xml", target: "${target}.pom"]
}

def jar(path, target) {
    return [pattern: "${path}/target/*.jar",
            target         : "${target}.jar",
            excludePatterns: ["*-exec.jar"]
            ]
}

def tar(path, target) {
    return [pattern: "${path}/target/*.tar.gz",
            target : "${target}-dist.tar.gz"]
}

def runjar(path, target) {
    return [pattern: "${path}/target/*-exec.jar",
            target : "${target}-exec.jar"]
}

pipeline {
    agent {
        kubernetes {
            label 'score-executor'
            yaml """
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: jdk
    tty: true
    image: openjdk:11
    env: 
      - name: DOCKER_HOST 
        value: tcp://localhost:2375
    volumeMounts:
      - name: maven-cache
        mountPath: "/root/.m2"
  - name: dind-daemon 
    image: docker:18.06-dind
    securityContext: 
        privileged: true 
    volumeMounts: 
      - name: docker-graph-storage 
        mountPath: /var/lib/docker 
  - name: helm
    image: alpine/helm:2.12.3
    command:
    - cat
    tty: true
  - name: docker
    image: docker:18-git
    tty: true
    volumeMounts:
    - mountPath: /var/run/docker.sock
      name: docker-sock
  volumes:
  - name: docker-sock
    hostPath:
      path: /var/run/docker.sock
      type: File
  - name: docker-graph-storage 
    emptyDir: {}
  - name: maven-cache
    emptyDir: {}
"""
        }
    }
    stages {
        stage('Prepare') {
            steps {
                script {
                    commit = sh(returnStdout: true, script: 'git describe --always').trim()
                }
                script {
                    version = readMavenPom().getVersion()
                }
                
            }
        }
        stage('Test') {
            steps {
                container('jdk') {
                    sh "./mvnw test package"
                }
            }
        }
        stage('Build & Publish Develop') {
            when {
                branch "develop"
            }
            steps {
                container('docker') {
                    withCredentials([usernamePassword(credentialsId: 'OvertureDockerHub', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                        sh 'docker login -u $USERNAME -p $PASSWORD'
                    }
                    sh "docker build --target=server --network=host -f Dockerfile . -t overture/score-server:edge -t overture/score-server:${commit}"
                    sh "docker build --target=client --network=host -f Dockerfile . -t overture/score:edge -t overture/score:${commit}"
                    sh "docker push overture/score-server:${commit}"
                    sh "docker push overture/score-server:edge"
                    sh "docker push overture/score:${commit}"
                    sh "docker push overture/score:edge"
                }
            }
        }
        stage('Release & tag') {
            when {
                branch "master"
            }
            steps {
                container('docker') {
                    withCredentials([usernamePassword(credentialsId: 'OvertureBioGithub', passwordVariable: 'GIT_PASSWORD', usernameVariable: 'GIT_USERNAME')]) {
                        sh "git tag ${version}"
                        sh "git push https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/overture-stack/score --tags"
                    }
                    withCredentials([usernamePassword(credentialsId: 'OvertureDockerHub', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                        sh 'docker login -u $USERNAME -p $PASSWORD'
                    }
                    sh "docker build --target=server --network=host -f Dockerfile . -t overture/score-server:latest -t overture/score-server:${version}"
                    sh "docker build --target=client --network=host -f Dockerfile . -t overture/score:latest -t overture/score:${version}"
                    sh "docker push overture/score-server:${version}"
                    sh "docker push overture/score-server:latest"
                    sh "docker push overture/score:${version}"
                    sh "docker push overture/score:latest"
                }
            }
        }

        stage('Deploy to Overture QA') {
            when {
                branch "develop"
            }
            steps {
                container('helm') {
                    withCredentials([file(credentialsId: '4ed1e45c-b552-466b-8f86-729402993e3b', variable: 'KUBECONFIG')]) {
                        sh 'env'
                        sh 'helm init --client-only'
                        sh "helm ls --kubeconfig $KUBECONFIG"
                        sh "helm repo add overture https://overture-stack.github.io/charts-server/"
                        sh """
                            helm upgrade --kubeconfig $KUBECONFIG --install --namespace=overture-qa score-overture-qa \\
                            overture/score --reuse-values --set-string image.tag=${commit}
                           """
                    }
                }
            }
        }

        stage('Deploy to Overture Staging') {
            when {
                branch "master"
            }
            steps {
                container('helm') {
                    withCredentials([file(credentialsId: '4ed1e45c-b552-466b-8f86-729402993e3b', variable: 'KUBECONFIG')]) {
                        sh 'env'
                        sh 'helm init --client-only'
                        sh "helm ls --kubeconfig $KUBECONFIG"
                        sh "helm repo add overture https://overture-stack.github.io/charts-server/"
                        sh """
                            helm upgrade --kubeconfig $KUBECONFIG --install --namespace=overture-staging score-overture-staging \\
                            overture/score --reuse-values --set-string image.tag=${version}
                           """
                    }
                }
            }
        }

        stage('Destination SNAPSHOT') {
            when {
                anyOf {
                    branch 'develop'
                    branch 'test-develop'
                }
            }
            steps {
                script {
                    repo = "dcc-snapshot/bio/overture"
                }
            }
        }

        stage('Destination release') {
            when {
                anyOf {
                    branch 'master'
                    branch 'test-master'
                }
            }
            steps {
                script {
                    repo = "dcc-release/bio/overture"
                }
            }
        }

        stage('Upload Artifacts') {
            when {
                anyOf {
                    branch 'master'
                    branch 'test-master'
                    branch 'develop'
                    branch 'test-develop'
                }
            }
            steps {
                script {
                    
                    project = "score"
                    versionName = "$version"
                    subProjects = ['client', 'core', 'fs', 'server', 'test']

                    files = []
                    files.add([pattern: "pom.xml", target: "$repo/$project/$versionName/$project-$versionName.pom"])

                    for (s in subProjects) {
                        name = "${project}-$s"
                        target = "$repo/$name/$versionName/$name-$versionName"
                        files.add(pom(name, target))
                        files.add(jar(name, target))

                        if (s in ['client', 'server']) {
                            files.add(runjar(name, target))
                            files.add(tar(name, target))
                        }
                    }

                    fileSet = JsonOutput.toJson([files: files])
                    pretty = JsonOutput.prettyPrint(fileSet)
                    print("Uploading files=${pretty}")
                }

                rtUpload(serverId: 'artifactory', spec: fileSet)
            }
        }
    }
}

