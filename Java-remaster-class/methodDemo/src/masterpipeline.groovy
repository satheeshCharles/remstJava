properties([gitLabConnection(''), [$class: 'RebuildSettings', autoRebuild: false, rebuildDisabled: false],
            [$class: 'JobRestrictionProperty'],
            parameters([
                    string(defaultValue: 'http://gitrepop1/simplicity/tara-javaui.git', description: 'provide git repository url for tara app code', name: 'TARA_APP_GIT_REPO',trim: true),
                    string(defaultValue: 'project/P0300863', description: 'provide git branch for tara app code', name: 'TARA_APP_BRANCH',trim: true),
                    string(defaultValue: 'http://gitrepop1/simplicity/tara-javaui.git', description: 'provide git repository url for ui code', name: 'TARA_UI_GIT_REPO',trim: true),
                    string(defaultValue: 'project/P0300863', description: 'provide git branch for ui code', name: 'TARA_UI_BRANCH',trim: true),
                    string(defaultValue: 'http://gitrepop1/simplicity/tara-db.git', description: 'provide git repository url for tara db', name: 'TARA_DB_GIT_REPO',trim: true),
                    string(defaultValue: 'project/P0300863', description: 'provide git branch for tara db', name: 'TARA_DB_BRANCH',trim: true),
                    string(defaultValue: 'project/P0300863', description: 'provide git branch for tara clover', name: 'TARA_CLOVER_SANDBOX_BRANCH',trim: true),
                    booleanParam(name: 'SKIP_TESTS', defaultValue: true, description: 'Do you want to skip tests?'),
                    string(defaultValue: 'azz,axl,pcb,pet,mrc,muz,ssg,pem', description: 'enter the client(s)', name: 'CLIENTS',trim: true),
                    string(defaultValue: 'taradb,desktop-eap,desktop-httpd,iweb-eap,iweb-httpd,idr,idrloader,cloveretl,sftp', description: 'enter the product(s) you want to retag', name: 'PRODUCTS',trim: true),
                    string(defaultValue: 'taraprj', description: 'enter the stream name', name: 'STREAM', trim: true),
                    string(defaultValue: '3.1.01.tar', description: 'enter the tag', name: 'GIT_TAG', trim: true),
                    string(defaultValue: '3.1.00.tar', description: 'enter the baseline tag', name: 'BASELINE_TAG', trim: true),
                    choice(choices: ['OpenJDK11'], description: 'select appropriate application JDK_VERSION version', name: 'APP_JDK'),
                    string(defaultValue: 'ifdsbuilduserssh', description: 'provide required build user', name: 'BUILT_BY', trim: true),
                    string(defaultValue: 'taraapp', description: 'provide appropriate app name', name: 'APP', trim: true),
                    string(defaultValue: '3.1.02.tar-SNAPSHOT', description: 'provide next development version, FORMAT:-SNAPSHOT, EX: 20.4.2-SNAPSHOT', name: 'DEV_VERSION', trim: true),
                    string(defaultValue: '3.1.01.tar', description: 'provide version of release, FORMAT:, EX: 20.4.1', name: 'RELEASE_VERSION', trim: true),
                    string(defaultValue: 'P0300510-1963,[maven-release-plugin]', description: 'provide commit message prefix as per commit message format, FORMAT:[maven-release-plugin], EX:P0300510-962,[maven-release-plugin]', name: 'COMMIT_MESSAGE_PREFIX', trim: true),
                    string(defaultValue: 'http://almnexusp1:8081/nexus/content/repositories/releases', description: 'enter the nexus URL', name: 'NEXUS_REPO_URL',trim: true),
                    string(defaultValue: 'insprint', description: 'enter the namespace to publish release', name: 'NAMESPACE',trim: true),
                    string(defaultValue: 'docker-prod-local.si1ocevar01.clustere.ifdsgroup.ca', description: 'enter jfrog registry url', name: 'JFROG_REGISTRY_URL',trim: true),
                    string(defaultValue: 'jFrogURL_Token', description: 'enter jfrog token', name: 'JFROG_TOKEN',trim: true),
                    string(defaultValue: 'images-dev', description: 'enter namespace to push images', name: 'DOCKER_NAMESPACE',trim: true),
                    string(defaultValue: 'IFDS Build User to access GIT LAB', description: 'provide git credential', name: 'GIT_CREDENTIAL',trim: true),
                    string(defaultValue: 'master', description: 'provide pipeline script branch', name: 'PIPELINE_SCRIPT_BRANCH',trim: true),
                    string(defaultValue: 'ifast_docker_node', description: 'enter the node label', name: 'NODE_LABEL', trim: true),
                    string(defaultValue: 'windows', description: 'enter the windows node label', name: 'WINDOWS_NODE_LABEL', trim: true)
            ]), pipelineTriggers([])])
pipeline
        {
            agent
                    {
                        node
                                {
                                    label NODE_LABEL
                                }
                    }
            environment
                    {
                        DOCKER_HOST = 'tcp://127.0.0.1:2376'
                    }
            stages
                    {
                        stage('TARA Parallel Builds')
                                {
                                    parallel
                                            {
                                                stage('Images re-tagging')
                                                        {
                                                            steps
                                                                    {
                                                                        build job: 'image-retagging',
                                                                                parameters: [
                                                                                        string(name: 'clients', value: CLIENTS),
                                                                                        string(name: 'products', value: PRODUCTS),
                                                                                        string(name: 'source_tag', value: BASELINE_TAG),
                                                                                        string(name: 'dest_tag', value: GIT_TAG),
                                                                                        string(name: 'jfrog_registry_url', value: JFROG_REGISTRY_URL),
                                                                                        string(name: 'docker_namespace', value: DOCKER_NAMESPACE),
                                                                                        string(name: 'pipeline_script_branch', value: PIPELINE_SCRIPT_BRANCH),
                                                                                        string(name: 'slave_node', value: NODE_LABEL)
                                                                                ]
                                                                    }
                                                        }
                                                stage('Copy DB Master tar files')
                                                        {
                                                            steps
                                                                    {
                                                                        build job: 'copy-ifast-db',
                                                                                parameters: [
                                                                                        string(name: 'clients', value: CLIENTS),
                                                                                        string(name: 'stream', value: STREAM.toLowerCase()),
                                                                                        string(name: 'source_tag', value: BASELINE_TAG),
                                                                                        string(name: 'dest_tag', value: GIT_TAG),
                                                                                        string(name: 'pipeline_script_branch', value: PIPELINE_SCRIPT_BRANCH),
                                                                                        string(name: 'slave_node', value: NODE_LABEL)
                                                                                ]
                                                                    }
                                                        }

                                                stage('Build Tara App')
                                                        {
                                                            steps
                                                                    {
                                                                        build job: 'tara-app-release',
                                                                                parameters: [
                                                                                        string(name: 'Slave_node', value: NODE_LABEL),
                                                                                        string(name: 'GIT_URL', value: TARA_APP_GIT_REPO),
                                                                                        string(name: 'GIT_BRANCH', value: TARA_APP_BRANCH),
                                                                                        string(name: 'APP_JDK', value: APP_JDK),
                                                                                        string(name: 'BUILT_BY', value: BUILT_BY),
                                                                                        string(name: 'APP', value: APP),
                                                                                        string(name: 'RELEASE_VERSION', value: RELEASE_VERSION),
                                                                                        string(name: 'DEV_VERSION', value: DEV_VERSION),
                                                                                        string(name: 'COMMIT_MESSAGE_PREFIX', value: COMMIT_MESSAGE_PREFIX),
                                                                                        string(name: 'tagname', value: GIT_TAG),
                                                                                        string(name: 'dockerNamespace', value: DOCKER_NAMESPACE),
                                                                                        string(name: 'git_credential', value: GIT_CREDENTIAL),
                                                                                        string(name: 'jfrogRegistryurl', value: JFROG_REGISTRY_URL),
                                                                                        string(name: 'jfrogToken', value: JFROG_TOKEN),
                                                                                        booleanParam(name: 'SKIP_TESTS', value: SKIP_TESTS),
                                                                                ]
                                                                    }
                                                        }
                                                stage('Build Tara UI')
                                                        {
                                                            steps
                                                                    {
                                                                        build job: 'tara-javaui',
                                                                                parameters: [
                                                                                        string(name: 'Slave_node', value: NODE_LABEL),
                                                                                        string(name: 'uiGitRepoName', value: TARA_UI_GIT_REPO),
                                                                                        string(name: 'tarauiBranchname', value: TARA_UI_BRANCH),
                                                                                        string(name: 'tagname', value: GIT_TAG),
                                                                                        string(name: 'dockerNamespace', value: DOCKER_NAMESPACE),
                                                                                        string(name: 'git_credential', value: GIT_CREDENTIAL),
                                                                                        string(name: 'jfrogRegistryurl', value: JFROG_REGISTRY_URL),
                                                                                        string(name: 'jfrogToken', value: JFROG_TOKEN)
                                                                                ]
                                                                    }
                                                        }
                                                stage('Build Tara DB Populator')
                                                        {
                                                            steps
                                                                    {
                                                                        build job: 'tara-liquibase',
                                                                                parameters: [
                                                                                        string(name: 'Slave_node', value: NODE_LABEL),
                                                                                        string(name: 'dbGitRepoName', value: TARA_DB_GIT_REPO),
                                                                                        string(name: 'taradbBranchName', value: TARA_DB_BRANCH),
                                                                                        string(name: 'current_release', value: GIT_TAG),
                                                                                        string(name: 'dockerNamespace', value: DOCKER_NAMESPACE),
                                                                                        string(name: 'git_credential', value: GIT_CREDENTIAL),
                                                                                        string(name: 'jfrogRegistryurl', value: JFROG_REGISTRY_URL),
                                                                                        string(name: 'jfrogToken', value: JFROG_TOKEN)

                                                                                ]
                                                                    }
                                                        }
                                                stage('Desktop CPP folder copy in R drive')
                                                        {
                                                            steps
                                                                    {
                                                                        build job: 'copy-desktop-cpp',
                                                                                parameters: [
                                                                                        string(name: 'source_tag', value: BASELINE_TAG),
                                                                                        string(name: 'dest_tag', value: GIT_TAG),
                                                                                        string(name: 'pipeline_script_branch', value: PIPELINE_SCRIPT_BRANCH),
                                                                                        string(name: 'windows_node_label', value: WINDOWS_NODE_LABEL)
                                                                                ]
                                                                    }
                                                        }


                                            }
                                    post
                                            {
                                                failure
                                                        {
                                                            println "error occured"

                                                        }
                                            }
                                }

                        stage('Git Checkout - TARA Clover')
                                {
                                    steps
                                            {
                                                deleteDir()
                                                checkout([$class: 'GitSCM',
                                                          branches: [[name: "${TARA_CLOVER_SANDBOX_BRANCH}"]],
                                                          doGenerateSubmoduleConfigurations: false,
                                                          extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: "tara-clover-dir"]],
                                                          gitTool: 'Default',
                                                          submoduleCfg: [],
                                                          userRemoteConfigs: [[credentialsId: 'IFDS Build User to access GIT LAB', url: 'http://gitrepop1/simplicity/tara-clover.git']]])

                                            }

                                }

                        stage('Tagging - TARA clover')
                                {
                                    steps
                                            {
                                                script
                                                        {
                                                            sshagent (credentials: ['ifdsbuilduserssh'])
                                                                    {
                                                                        sh """ cd ${WORKSPACE}/tara-clover-dir
           git tag ${GIT_TAG}
           git remote set-url origin git@gitrepop1:simplicity/tara-clover.git
           git push origin ${GIT_TAG}
           """
                                                                    }
                                                        }

                                            }

                                }


                        stage('IDR Schema Upload')
                                {
                                    steps
                                            {
                                                script
                                                        {
                                                            sshagent (credentials: ['ifdsbuilduserssh'])
                                                                    {
                                                                        sh """ 
           curl -fsSL -o  idr-schema-${BASELINE_TAG}.zip ${NEXUS_REPO_URL}/idr/idr-schema/${BASELINE_TAG}/idr-schema-${BASELINE_TAG}.zip
           if [ \$? -eq 0 ]
           then
                mv idr-schema-${BASELINE_TAG}.zip idr-schema-${GIT_TAG}.zip
                mvn deploy:deploy-file \
                -DgroupId='idr' \
                -DartifactId='idr-schema' \
                -Dversion='${GIT_TAG}' \
                -DgeneratePom=false \
                -Dpackaging='zip' \
                -DrepositoryId='releases' \
                -Durl='${NEXUS_REPO_URL}' \
                -Dfile='idr-schema-${GIT_TAG}.zip'
                echo "IDR schema upload successful"

           else
                echo "Baseline IDR schema - idr-schema-${BASELINE_TAG}.zip download failed"
                exit 1
           fi
           """
                                                                    }
                                                        }

                                            }

                                }
                        stage('Clone Variance stored procedure execution in APE DB')
                                {
                                    steps
                                            {
                                                build job: 'clone-variance',
                                                        parameters: [
                                                                string(name: 'current_version', value: BASELINE_TAG),
                                                                string(name: 'next_version', value: GIT_TAG),
                                                                string(name: 'namespace', value: NAMESPACE),
                                                                string(name: 'pipeline_script_branch', value: PIPELINE_SCRIPT_BRANCH),
                                                                string(name: 'slave_node', value: NODE_LABEL)
                                                        ]
                                            }
                                }
                    }
        }