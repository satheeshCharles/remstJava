properties([gitLabConnection(''), [$class: 'RebuildSettings', autoRebuild: false, rebuildDisabled: false], [$class: 'JobRestrictionProperty'],
            parameters([
                    string(defaultValue: 'git@gitrepop1:simplicity/tara-javaui.git', description: 'provide git repository url for tara app code', name: 'GIT_URL',trim: true),
                    string(defaultValue: 'feature/P0300510-1963_my_test', description: 'provide git branch for checking out tara app code', name: 'GIT_BRANCH',trim: true),
                    choice(choices: ['OpenJDK11'], description: 'select appropriate application JDK_VERSION version', name: 'APP_JDK'),
                    string(defaultValue: 'ifdsbuilduserssh', description: 'provide required build user', name: 'BUILT_BY', trim: true),
                    string(defaultValue: 'taraapp', description: 'provide appropriate app name', name: 'APP', trim: true),
                    string(defaultValue: '', description: 'provide next development version, FORMAT:-SNAPSHOT, EX: 20.4.2-SNAPSHOT', name: 'DEV_VERSION', trim: true),
                    string(defaultValue: '', description: 'provide version of release, FORMAT:, EX: 20.4.1', name: 'RELEASE_VERSION', trim: true),
                    string(defaultValue: '', description: 'provide commit message prefix as per commit message format, FORMAT:[maven-release-plugin], EX:P0300510-962,[maven-release-plugin]', name: 'COMMIT_MESSAGE_PREFIX', trim: true),
                    string(defaultValue: 'ifast_docker_node', description: 'provide jenkins slave node', name: 'SLAVE_NODE', trim: true),
                    string(defaultValue: 'docker-registry-https-default.apps.apetest2.ifglobalproducts.com', description: 'Docker Registry URL', name: 'dockerregistryurl', trim: false),
                    string(defaultValue: 'ifast', description: 'Docker Namespace', name: 'dockerNamespace', trim: false),
                    string(defaultValue: 'DockerTokenApeTest2', description: 'Docker Token', name: 'dockertoken', trim: false),
                    string(defaultValue: 'docker-prod-local.si1ocevar01.clustere.ifdsgroup.ca', description: 'Jfrog Registry URL', name: 'jfrogRegistryurl', trim: false),
                    string(defaultValue: 'jFrogURL_Token', description: 'Please Provide Jfrog Registry Token', name: 'jfrogToken',trim: false),
                    booleanParam(name: 'SKIP_TESTS', defaultValue: false, description: 'Do you want to skip tests?'),
            ]), pipelineTriggers([])])


pipeline {
    agent {
        node {
            label SLAVE_NODE
        }
    }
    options { timestamps() }
    environment {
        GIT_COMMIT_HASH = ""
        GIT_CREDENTIAL = ""
        DOCKER_TOKEN = ""
        JFROG_TOKEN = ""
        DOCKER_REGISTRY_URL = ""
        JFROG_REGISTRY_URL = ""
        DOCKER_PATH = ""
        RELEASE = ""
        JDK_VERSION = ""
    }

    stages {

        stage('Checkout GIT Repo') {
            steps {
                script {
                    def config = readJSON file: "${env.WORKSPACE}/TARA-app/jenkins_config.json"
                    GIT_CREDENTIAL = config.credentials.git_credential.toString()
                    DOCKER_TOKEN = config.credentials.docker_token.toString()
                    JFROG_TOKEN = config.credentials.jfrog_token.toString()
                    DOCKER_REGISTRY_URL = config.registries.docker_registry_url.toString()
                    JFROG_REGISTRY_URL = config.registries.jfrog_registry_url.toString()
                    DOCKER_PATH = config.registries.docker_path.toString()
                    git branch: '${GIT_BRANCH}',
                            changelog: false,
                            credentialsId: GIT_CREDENTIAL,
                            poll: false,
                            url: '${GIT_URL}'
                    check_parameter_is_empty('GIT_URL', GIT_URL)
                    check_parameter_is_empty('GIT_BRANCH', GIT_BRANCH)
                    check_parameter_is_empty('APP_JDK', APP_JDK)
                    check_parameter_is_empty('BUILT_BY', BUILT_BY)
                    check_parameter_is_empty('APP', APP)
                    check_parameter_is_empty('COMMIT_MESSAGE_PREFIX', COMMIT_MESSAGE_PREFIX)
                }
            }
        }

        stage('Build TARA Package') {
            steps {
                script {
                    GIT_COMMIT_HASH = sh(script: "git log -n 1 --pretty=format:'%h'", returnStdout: true)
                    def jdk = tool name: APP_JDK
                    withEnv(["JAVA_HOME=${jdk}", "PATH=${jdk}/bin:${env.PATH}"]) {
                        sh """
                    java -version
                    cd ${env.WORKSPACE}/TARA-app/ """
                        BUILD_JDK = sh(script: "java -version 2>&1 | awk -F '\"' 'NR==1 {print \$2}'", returnStdout: true)
                        JDK_VERSION = BUILD_JDK.trim()
                        APP_VERSION = sh(script: ' cd TARA-app/ && mvn help:evaluate -q -Dexec.executable=echo -Dexec.args=\'${project.version}\' --non-recursive exec:exec | awk -F\'-SNAPSHOT\' \'{ print $1 }\'', returnStdout: true)
                        echo "Build jdk is ${BUILD_JDK}"
                        echo "APP_Version is ${APP_VERSION}"
                        if (RELEASE_VERSION?.trim()) {
                            RELEASE = RELEASE_VERSION.trim()
                        } else {
                            RELEASE = APP_VERSION.trim()
                        }
                        configFileProvider([configFile(fileId: '30af60c0-e14a-4902-a142-ade46eeba3e7', variable: 'MAVEN_SETTINGS')]) {
                            sh """

                            echo "Building ${APP} RELEASE JAR File"
                            cd ${env.WORKSPACE}/TARA-app/

                            mvn -B clean release:clean release:prepare release:perform -X \
                            -s $MAVEN_SETTINGS \
                            -Dmaven.repo.local=${WORKSPACE}\\lib \
                            -DignoreSnapshots \
                            -DscmCommentPrefix=${COMMIT_MESSAGE_PREFIX} \
                            -DdevelopmentVersion=${DEV_VERSION} \
                            -DcheckModificationExcludeList=pom.xml,.maven/spy.log \
                            -DautoVersionSubmodules=true \
                            -Dresume=false \
                            -Dmaven.javadoc.failOnError=false \
                            -Darguments="-Dmaven.repo.local=${WORKSPACE}\\TARA-app -Dbuilt_by=${BUILT_BY} -Dgit_commit_id=${GIT_COMMIT_HASH} -DskipTests=${SKIP_TESTS} -Dbuilt_jdk=${JDK_VERSION} -Dapp_release=${RELEASE}" \
                            -DreleaseVersion=${RELEASE} 

                        """
                        }
                    }
                }
            }
        }

        stage('Docker build and push') {
            steps {
                script {
                    withCredentials([string(credentialsId: 'jFrogURL_Token', variable: 'JFROGURL_TOKEN')]){
                        sh """
                                cd ${env.WORKSPACE}/TARA-app/
                                $sudo docker login -u scmapedeploy ${JFROGURL_TOKEN} ${jfrogRegistryurl}
                                $sudo docker build \\
                                --build-arg RELEASE=${RELEASE} \\
                                --build-arg APP=${APP} \\
                                --build-arg GIT_COMMIT_BRANCH=${GIT_BRANCH} \\
                                --build-arg GIT_COMMIT_ID=${GIT_COMMIT_HASH} \\
                                --build-arg GIT_COMMIT_TAG=${APP}-${RELEASE} \\
                                --build-arg BUILD_JDK=${JDK_VERSION} \\
                                -t ${APP} .

                                $sudo docker tag ${APP} ${jfrogRegistryurl}/${dockerNamespace}/${RELEASE}_taraapp
                                $sudo docker push ${jfrogRegistryurl}/${dockerNamespace}/${RELEASE}_taraapp
                            """
                    }
                }
            }
        }


    }
    post {
        always {
            cleanWs()
            println("End of Pipeline")
        }
    }

}
def check_parameter_is_empty(key, value) {
    if (value?.trim()) {
        println("${key}: ${value}")
    } else {
        error("Paramter ${key} is EMPTY/NULL")
    }
}