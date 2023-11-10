import groovy.json.JsonOutput

def pom(path, target) {
    return [pattern: "${path}/pom.xml", target: "${target}.pom"]
}

def jar(path, target) {
    return [pattern: "${path}/target/*.jar",
            target         : "${target}.jar",
            excludePatterns: ['*-exec.jar']
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

String podSpec = '''
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
    image: docker:20.10-dind
    securityContext:
        privileged: true
        runAsUser: 0
    volumeMounts:
      - name: docker-graph-storage
        mountPath: /var/lib/docker
    env:
    - name: TLS_VERIFY
      value: false
  - name: helm
    image: alpine/helm:2.12.3
    command:
    - cat
    tty: true
  - name: docker
    image: docker:20-git
    tty: true
    env:
    - name: DOCKER_HOST
      value: tcp://localhost:2375
    - name: HOME
      value: /home/jenkins/agent
  - name: curl
    image: curlimages/curl
    command:
    - cat
    tty: true
  securityContext:
    runAsUser: 1000
  volumes:
  - name: docker-graph-storage
    emptyDir: {}
  - name: maven-cache
    emptyDir: {}
'''

pipeline {
    agent {
        kubernetes {
            yaml podSpec
        }
    }

    environment {
        dockerHubRepo = 'overture/score'
        gitHubRegistry = 'ghcr.io'
        gitHubRepo = 'overture-stack/score'
        chartsServer = 'https://overture-stack.github.io/charts-server/'

        commit = sh(
            returnStdout: true,
            script: 'git describe --always'
        ).trim()

        version = readMavenPom().getVersion()
    }

    options {
        timeout(time: 30, unit: 'MINUTES')
        timestamps()
    }

    stages {
/*
        stage('Compile & Test') {
            steps {
                container('jdk') {
                    sh './mvnw package'
                }
            }
        }
*/
        stage('Build images') {
            when {
                anyOf {
                    branch 'develop'
                    branch 'master'
                    branch 'test*'
                    branch 'dind-tshooting'
                }
            }
            steps {
                container('docker') {
                    sh 'sleep infinity'
                    sh "docker build \
                        --target server \
                        --network host \
                        -f Dockerfile \
                        -t server:${commit} ."
                    sh "docker build \
                        --target client \
                        --network host \
                        -f Dockerfile \
                        -t client:${commit} ."
                }
            }
        }

        stage('Push images') {
            when {
                anyOf {
                    branch 'develop'
                    branch 'master'
                    branch 'test*'
                }
            }
            parallel {
                stage('...to dockerhub') {
                    steps {
                        container('docker') {
                            withCredentials([usernamePassword(
                                credentialsId:'OvertureDockerHub',
                                passwordVariable: 'PASSWORD',
                                usernameVariable: 'USERNAME'
                            )]) {
                                sh "docker login -u $USERNAME -p $PASSWORD"

                                script {
                                    if (env.BRANCH_NAME ==~ 'master') { // push latest and version tags
                                        sh "docker tag server:${commit} ${dockerHubRepo}-server:${version}"
                                        sh "docker push ${dockerHubRepo}-server:${version}"

                                        sh "docker tag client:${commit} ${dockerHubRepo}:${version}"
                                        sh "docker push ${dockerHubRepo}:${version}"

                                        sh "docker tag server:${commit} ${dockerHubRepo}-server:latest"
                                        sh "docker push ${dockerHubRepo}-server:latest"

                                        sh "docker tag client:${commit} ${dockerHubRepo}:latest"
                                        sh "docker push ${dockerHubRepo}:latest"
                                    } else { // push commit tag
                                        sh "docker tag server:${commit} ${dockerHubRepo}-server:${commit}"
                                        sh "docker push ${dockerHubRepo}-server:${commit}"

                                        sh "docker tag client:${commit} ${dockerHubRepo}:${commit}"
                                        sh "docker push ${dockerHubRepo}:${commit}"
                                    }

                                    if (env.BRANCH_NAME ==~ 'develop') { // push edge tag
                                        sh "docker tag server:${commit} ${dockerHubRepo}-server:edge"
                                        sh "docker push ${dockerHubRepo}-server:edge"

                                        sh "docker tag client:${commit} ${dockerHubRepo}:edge"
                                        sh "docker push ${dockerHubRepo}:edge"
                                    }
                                }
                            }
                        }
                    }
                }

                stage('...to github') {
                    steps {
                        container('docker') {
                            withCredentials([usernamePassword(
                                credentialsId:'OvertureBioGithub',
                                passwordVariable: 'PASSWORD',
                                usernameVariable: 'USERNAME'
                            )]) {
                                sh "docker login ${gitHubRegistry} -u $USERNAME -p $PASSWORD"

                                script {
                                    if (env.BRANCH_NAME ==~ 'master') { //push edge and commit tags
                                        sh "docker tag server:${commit} ${gitHubRegistry}/${gitHubRepo}-server:${version}"
                                        sh "docker push ${gitHubRegistry}/${gitHubRepo}-server:${version}"

                                        sh "docker tag client:${commit} ${gitHubRegistry}/${gitHubRepo}:${version}"
                                        sh "docker push ${gitHubRegistry}/${gitHubRepo}:${version}"

                                        sh "docker tag server:${commit} ${gitHubRegistry}/${gitHubRepo}-server:latest"
                                        sh "docker push ${gitHubRegistry}/${gitHubRepo}-server:latest"

                                        sh "docker tag client:${commit} ${gitHubRegistry}/${gitHubRepo}:latest"
                                        sh "docker push ${gitHubRegistry}/${gitHubRepo}:latest"
                                    } else { // push commit tag
                                        sh "docker tag server:${commit} ${gitHubRegistry}/${gitHubRepo}-server:${commit}"
                                        sh "docker push ${gitHubRegistry}/${gitHubRepo}-server:${commit}"

                                        sh "docker tag client:${commit} ${gitHubRegistry}/${gitHubRepo}:${commit}"
                                        sh "docker push ${gitHubRegistry}/${gitHubRepo}:${commit}"
                                    }

                                    if (env.BRANCH_NAME ==~ 'develop') { // push edge tag
                                        sh "docker tag server:${commit} ${gitHubRegistry}/${gitHubRepo}-server:edge"
                                        sh "docker push ${gitHubRegistry}/${gitHubRepo}-server:edge"

                                        sh "docker tag client:${commit} ${gitHubRegistry}/${gitHubRepo}:edge"
                                        sh "docker push ${gitHubRegistry}/${gitHubRepo}:edge"
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        stage('Release & tag') {
            when {
                branch 'master'
            }
            steps {
                container('docker') {
                    withCredentials([usernamePassword(
                        credentialsId: 'OvertureBioGithub',
                        passwordVariable: 'GIT_PASSWORD',
                        usernameVariable: 'GIT_USERNAME'
                    )]) {
                        sh "git tag ${version}"
                        sh "git push https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/${gitHubRepo} --tags"
                    }
                }
            }
        }

        stage('Deploy to Overture QA') {
            when {
                anyOf {
                    branch 'develop'
                    // branch "test"
                }
            }
            steps {
                build(job: '/Overture.bio/provision/helm', parameters: [
                        [$class: 'StringParameterValue', name: 'OVERTURE_ENV', value: 'qa' ],
                        [$class: 'StringParameterValue', name: 'OVERTURE_CHART_NAME', value: 'score'],
                        [$class: 'StringParameterValue', name: 'OVERTURE_RELEASE_NAME', value: 'score'],
                        [$class: 'StringParameterValue', name: 'OVERTURE_HELM_CHART_VERSION', value: ''], // use latest
                        [$class: 'StringParameterValue', name: 'OVERTURE_HELM_REPO_URL', value: chartsServer],
                        [$class: 'StringParameterValue', name: 'OVERTURE_HELM_REUSE_VALUES', value: 'true' ],
                        [$class: 'StringParameterValue', name: 'OVERTURE_ARGS_LINE', value: "--set-string image.tag=${commit}" ]
                ])
            }
        }

        stage('Deploy to Overture Staging') {
            when {
                branch 'master'
            }
            steps {
                build(job: '/Overture.bio/provision/helm', parameters: [
                        [$class: 'StringParameterValue', name: 'OVERTURE_ENV', value: 'staging' ],
                        [$class: 'StringParameterValue', name: 'OVERTURE_CHART_NAME', value: 'score'],
                        [$class: 'StringParameterValue', name: 'OVERTURE_RELEASE_NAME', value: 'score'],
                        [$class: 'StringParameterValue', name: 'OVERTURE_HELM_CHART_VERSION', value: ''], // use latest
                        [$class: 'StringParameterValue', name: 'OVERTURE_HELM_REPO_URL', value: chartsServer],
                        [$class: 'StringParameterValue', name: 'OVERTURE_HELM_REUSE_VALUES', value: 'true' ],
                        [$class: 'StringParameterValue', name: 'OVERTURE_ARGS_LINE', value: "--set-string image.tag=${version}" ]
                ])
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
                    repo = 'dcc-snapshot/bio/overture'
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
                    repo = 'dcc-release/bio/overture'
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
                    project = 'score'
                    versionName = "$version"
                    subProjects = ['client', 'core', 'fs', 'server', 'test']

                    files = []
                    files.add([pattern: 'pom.xml', target: "$repo/$project/$versionName/$project-${versionName}.pom"])

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

    post {
        fixed {
            withCredentials([string(
                credentialsId: 'OvertureSlackJenkinsWebhookURL',
                variable: 'fixed_slackChannelURL'
            )]) {
                container('curl') {
                    script {
                        if (env.BRANCH_NAME ==~ /(develop|master|test\S*)/) {
                            sh "curl \
                                -X POST \
                                -H 'Content-type: application/json' \
                                --data '{ \
                                    \"text\":\"Build Fixed: ${env.JOB_NAME}#${commit} \
                                    \n[Build ${env.BUILD_NUMBER}] (${env.BUILD_URL})\" \
                                }' \
                                ${fixed_slackChannelURL}"
                        }
                    }
                }
            }
        }

        success {
            withCredentials([string(
                credentialsId: 'OvertureSlackJenkinsWebhookURL',
                variable: 'success_slackChannelURL'
            )]) {
                container('curl') {
                    script {
                        if (env.BRANCH_NAME ==~ /(test\S*)/) {
                            sh "curl \
                                -X POST \
                                -H 'Content-type: application/json' \
                                --data '{ \
                                    \"text\":\"Build tested: ${env.JOB_NAME}#${commit} \
                                    \n[Build ${env.BUILD_NUMBER}] (${env.BUILD_URL})\" \
                                }' \
                                ${success_slackChannelURL}"
                        }
                    }
                }
            }
        }

        unsuccessful {
            withCredentials([string(
                credentialsId: 'OvertureSlackJenkinsWebhookURL',
                variable: 'failed_slackChannelURL'
            )]) {
                container('curl') {
                    script {
                        if (env.BRANCH_NAME ==~ /(develop|master|test\S*)/) {
                            sh "curl \
                                -X POST \
                                -H 'Content-type: application/json' \
                                --data '{ \
                                    \"text\":\"Build Failed: ${env.JOB_NAME}#${commit} \
                                    \n[Build ${env.BUILD_NUMBER}] (${env.BUILD_URL})\" \
                                }' \
                                ${failed_slackChannelURL}"
                        }
                    }
                }
            }
        }
    }
}
