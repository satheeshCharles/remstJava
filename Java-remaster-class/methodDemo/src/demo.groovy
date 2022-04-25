import java.lang.ProcessBuilder.Redirect
import hudson.model.*
import hudson.util.*
import hudson.scm.*
import hudson.FilePath
import hudson.scm.SubversionChangeLogSet.LogEntry
import groovy.lang.GroovyObjectSupport
import groovy.util.BuilderSupport
import groovy.util.AntBuilder
import org.apache.commons.lang.StringUtils

properties(
        [gitLabConnection(''), [$class: 'RebuildSettings', autoRebuild: false, rebuildDisabled: false],
         [$class: 'JobRestrictionProperty'],
         parameters([
                 string(defaultValue: '', description: 'Please enter ifastbase Source branch', name: 'ifast_Source_branch', trim: false),
                 string(defaultValue: '', description: 'Please enter ifastbase Target branch', name: 'ifast_Target_branch', trim: false),
                 choice(choices: ['ina','bmo','slf','axl','aim','ina,axl','axl,ina','axl,ina,slf'], description: 'Please select client code', name: 'clients'),
                 choice(choices: ['packageFull'], description: 'Please enter package type', name: 'package'),
                 choice(choices: ['DEV', 'QA', 'UAT', 'PROD', 'P0275866','redleaf','europeantransformation','BMOPROJQA','CF','INT','insprint','int','cfdevmr','cfdev','cfhotfixmr','cfhotfix','cfrelmr','cfrel'], description: 'Please select SDLC', name: 'SDLC'),
                 string(defaultValue: '', description: 'Please enter Release value', name: 'Release_Value', trim: false),
                 string(defaultValue: '', description: 'Please enter Previous release value', name: 'Previous_Release_Value', trim: false),
                 string(defaultValue: 'NO', description: 'Please select product change option as per requirment', name: 'Product_Changes', trim: false),
                 choice(choices: ['YES','NO'], description: 'Please select DBUPGRADE option as per requirment', name: 'DBUPGRADE'),
                 choice(choices: ['YES','NO'], description: 'Please select ImageUpgrade option as per requirment', name: 'ImageUpgrade'),
                 choice(choices: ['11.7.3.5','11.7.3.6','11.7.3.5-CF','11.7.3.7-adhoc','11.7.3.8-docker','11.7.3.7','V11.7.3.7'], description: 'Please select Tag_BaseImage option as per requirment', name: 'Tag_BaseImage'),
                 string(defaultValue: '4', description: 'Please enter Number thread', name: 'Num_Threads', trim: false),
                 string(defaultValue: 'docker-dev-local.si1ocevar01.clustere.ifdsgroup.ca', description: 'Please enter JFrog URL', name: 'jfrogRegistryurl', trim: false),
                 choice(choices: ['30af60c0-e14a-4902-a142-ade46eeba3e7', 'ab2898c7-d547-4e71-9335-c89604d0f2ad'], description: 'Please select respective maven settings file id', name: 'maven_settings_file_id', trim: false),
                 string(defaultValue: '', description: 'Please provide list of files to be whitelisted from unit tests', name: 'white_list_files', trim: false),
                 booleanParam(name: 'skipUnitTests', defaultValue: false, description: 'Do you want to skip unit tests?'),
                 booleanParam(name: 'skipDpSyntaxChecks', defaultValue: false, description: 'Do you want to skip data patch checks?'),
                 string(defaultValue: '', description: 'Please enter JAVA CRS Transform branch for git checkout(http://gitrepop1/iFastUtilities/JavaCRSXMLTransform.git)', name: 'JAVA_CRSTransform_branch', trim: false),
                 string(defaultValue: '', description: 'Please enter JAVA FATCA Transform branch for git checkout(http://gitrepop1/iFastUtilities/JavaXMLTransform.git)', name: 'JAVA_FATCATransform_branch', trim: false),
                 string(defaultValue: '', description: 'Please enter NSCC branch for git checkout(http://gitrepop1/iFastUtilities/NSCCGenerator.git)', name: 'NSCC_branch', trim: false),
                 string(defaultValue: 'http://gitrepop1/iFastSandbox/iFastBaseRepo.git', description: 'Please enter ifastbase repo to checkout', name: 'base_repo', trim: false),
                 string(defaultValue: 'http://gitrepop1/iFastUtilities/JavaCRSXMLTransform.git', description: 'Please enter JAVA CRS Transform repo for git checkout', name: 'JAVA_CRSTransform_repo', trim: false),
                 string(defaultValue: 'http://gitrepop1/iFastUtilities/JavaXMLTransform.git', description: 'Please enter JAVA FATCA Transform repo for git checkout', name: 'JAVA_FATCATransform_repo', trim: false),
                 string(defaultValue: 'http://gitrepop1/iFastUtilities/NSCCGenerator.git', description: 'Please enter NSCC repo for git checkout', name: 'NSCC_repo', trim: false),
                 string(defaultValue: 'apews-exp-cf.apps.chimp2.dev-ifglobalproducts.com:80', description: 'Please enter APEWS URL along with port no. like apews-exp-dev01.apps.chimp2.dev-ifglobalproducts.com:80', name: 'APEWS_URL_PORT', trim: false),
                 string(defaultValue: '/dbrepo', description: 'Please enter /dbrepo mount path where ifastdb tar saved', name: 'dbrepo_mount_path', trim: false),
                 choice(choices: ['Current Workspace','/scmbuild'], description: 'Please select PCT package saved mount path/workspace like /scmbuild', name: 'PCT_pkg_mount_path'),
                 string(defaultValue: 'ifastbase/build', description: 'Please enter Base image name', name: 'BaseImageName', trim: false),
                 string(defaultValue: 'almnexusp1:8081', description: 'Please enter Nexus URL', name: 'nexusUrl', trim: false),
                 string(defaultValue: '', description: 'Please enter Nexus dependency git repo', name: 'nexus_dependency_script_repo', trim: false),
                 string(defaultValue: '', description: 'Please enter Nexus dependency git branch', name: 'nexus_dependency_script_branch', trim: false),
                 string(defaultValue: '', description: 'Please enter script script repo', name: 'docker_script_repo', trim: false),
                 string(defaultValue: '', description: 'Please enter docker script branch', name: 'docker_script_branch', trim: false),
                 string(defaultValue: 'ifast_docker_node', description: 'Slave_node', name: 'Slave_node', trim: false),
                 string(defaultValue: '', description: 'sendmail', name: 'sendmail', trim: false),
                 string(defaultValue: 'git@gitrepop1:APE/imagebuilder-ifastbase.git', description: 'Please enter Base image git repo in SSH protocol like git@gitrepop1:APE/imagebuilder-ifastbase.git', name: 'OE_GIT_REPO', trim: false),
                 string(defaultValue: 'Build/', description: 'Please enter Base image git repo specific folder to do checkout', name: 'OE_DOCKER_FOLDER', trim: false),
                 string(defaultValue: 'registry.apps.apetest2.ifglobalproducts.com:80', description: 'Please enter Docker registry URL', name: 'dockerRegistry', trim: false),
                 string(defaultValue: 'images-dev', description: 'Please enter Docker Namespace', name: 'dockerNamespace', trim: false)
         ]),pipelineTriggers([])])
pipeline
        {
            agent
                    {
                        node
                                {
                                    label Slave_node
                                }
                    }
            options {
                timestamps()
                throttleJobProperty(
                        categories: ['cf_throttle_nodes'],
                        limitOneJobWithMatchingParams: false,
                        paramsToUseForLimit: '',
                        throttleEnabled: true,
                        throttleOption: 'category',
                )
            }
            environment
                    {
                        DLC="/opt/progress/dlc"
                        myClientsBuild=""
                        mypackage=""
                        myProduct_Changes=""
                        myDBUPGRADE=""
                        relativeFolder=""
                        myWorkspace=""
                        existClassFile=""
                        previousBranch=""
                        relver =""
                        repo_name=""
                        myRelease_Value=""
                        myNum_Threads=""
                        myTag_BaseImage=""
                        myBuildNumber=""
                        myBaseDirectory=""
                        myaction=""
                        myape_env=""
                        splitClient1=""
                        APEWS_URL=""
                        APEWS_PORT=""
                        myAPEWS_URL_PORT=""
                        myImageUpgrade=""
                        PCT_package_path=""
                        devops_group_email='TorontoCICDAutomation@ifdsgroup.com'
                    }

            stages
                    {
                        stage('Set Build Variable')
                                {

                                    steps
                                            {
                                                script
                                                        {

                                                            def myparams = currentBuild.rawBuild.getAction(ParametersAction).getParameters()
                                                            myparams.each
                                                                    {
                                                                        println it
                                                                        if (it.name=='clients')
                                                                        {
                                                                            myClientsBuild=String.valueOf(it).trim()
                                                                            myClientsBuild=myClientsBuild.substring(myClientsBuild.lastIndexOf("=") + 2,myClientsBuild.length()-1)
                                                                            println myClientsBuild
                                                                        }
                                                                        if (it.name=='APEWS_URL_PORT')
                                                                        {
                                                                            myAPEWS_URL_PORT=String.valueOf(it).trim()
                                                                            myAPEWS_URL_PORT=myAPEWS_URL_PORT.substring(myAPEWS_URL_PORT.lastIndexOf("=") + 2,myAPEWS_URL_PORT.length()-1)
                                                                            println myAPEWS_URL_PORT
                                                                        }
                                                                        if (it.name=='package')
                                                                        {
                                                                            mypackage=String.valueOf(it).trim()
                                                                            mypackage=mypackage.substring(mypackage.lastIndexOf("=") + 2,mypackage.length()-1)
                                                                            println mypackage
                                                                        }
                                                                        if (it.name=='DBUPGRADE')
                                                                        {
                                                                            myDBUPGRADE=String.valueOf(it).trim()
                                                                            myDBUPGRADE=myDBUPGRADE.substring(myDBUPGRADE.lastIndexOf("=") + 2,myDBUPGRADE.length()-1)
                                                                            println myDBUPGRADE
                                                                        }
                                                                        if (it.name=='Release_Value')
                                                                        {
                                                                            myRelease_Value =String.valueOf(it).trim()
                                                                            myRelease_Value=myRelease_Value.substring(myRelease_Value.lastIndexOf("=") + 2,myRelease_Value.length()-1)
                                                                            println myRelease_Value
                                                                        }
                                                                        if (it.name=='Previous_Release_Value')
                                                                        {
                                                                            myPreviousRelease =String.valueOf(it).trim()
                                                                            myPreviousRelease=myPreviousRelease.substring(myPreviousRelease.lastIndexOf("=") + 2,myPreviousRelease.length()-1)
                                                                            println myPreviousRelease
                                                                        }
                                                                        if (it.name=='Product_Changes')
                                                                        {
                                                                            myProduct_Changes =String.valueOf(it).trim()
                                                                            myProduct_Changes=myProduct_Changes.substring(myProduct_Changes.lastIndexOf("=") + 2,myProduct_Changes.length()-1)
                                                                            println myProduct_Changes
                                                                        }
                                                                        if (it.name=='Num_Threads')
                                                                        {
                                                                            myNum_Threads =String.valueOf(it).trim()
                                                                            myNum_Threads=myNum_Threads.substring(myNum_Threads.lastIndexOf("=") + 2,myNum_Threads.length()-1)
                                                                            println myNum_Threads
                                                                        }
                                                                        if (it.name=='ImageUpgrade')
                                                                        {
                                                                            myImageUpgrade =String.valueOf(it).trim()
                                                                            myImageUpgrade=myImageUpgrade.substring(myImageUpgrade.lastIndexOf("=") + 2,myImageUpgrade.length()-1)
                                                                            println myImageUpgrade
                                                                        }
                                                                        if (it.name=='Tag_BaseImage')
                                                                        {
                                                                            myTag_BaseImage =String.valueOf(it).trim()
                                                                            myTag_BaseImage=myTag_BaseImage.substring(myTag_BaseImage.lastIndexOf("=") + 2,myTag_BaseImage.length()-1)
                                                                            println myTag_BaseImage
                                                                        }
                                                                    }
                                                            relativeFolder =myRelease_Value
                                                            PreviousrelativeFolder ="TAG_DATA/"+myPreviousRelease
                                                            myWorkspace1 = env.WORKSPACE
                                                            println "relativeFolder="+ relativeFolder
                                                            myBuildNumber=currentBuild.number
                                                            println "myBuildNumber "+myBuildNumber
                                                            myBaseDirectory=pwd()
                                                            println "myBaseDirectory "+myBaseDirectory
                                                            repo_name=base_repo.minus("http://gitrepop1/")
                                                            APEWS_URL=myAPEWS_URL_PORT.substring(0,(myAPEWS_URL_PORT.lastIndexOf(":")))
                                                            println "APEWS_URL= "+APEWS_URL
                                                            APEWS_PORT=myAPEWS_URL_PORT.substring(myAPEWS_URL_PORT.lastIndexOf(":"))
                                                            APEWS_PORT=APEWS_PORT.substring(1);
                                                            println "APEWS_PORT="+APEWS_PORT
                                                            //Based on PCT package choice parameter selection value will be assigned here
                                                            if (String.valueOf(PCT_pkg_mount_path) == 'Current Workspace')
                                                            {
                                                                PCT_package_path=myBaseDirectory
                                                            }
                                                            else
                                                            {
                                                                PCT_package_path=String.valueOf(PCT_pkg_mount_path)
                                                            }

                                                            println "PCT_package_path "+PCT_package_path

                                                        }
                                            }
                                }
                        stage('Git Checkout')
                                {

                                    steps
                                            {
                                                deleteDir()
                                                script{

                                                    if (String.valueOf(ifast_Source_branch) != "" && String.valueOf(ifast_Target_branch) != "")
                                                    {
                                                        println "Sync Merge check for Source branch:"+ifast_Source_branch+" and "+"Target branch:"+ifast_Target_branch
                                                        checkout([$class: 'GitSCM',
                                                                  branches: [[name: "*/${ifast_Target_branch}"]],
                                                                  doGenerateSubmoduleConfigurations: false,
                                                                  extensions: [
                                                                          [  $class: 'RelativeTargetDirectory',
                                                                             relativeTargetDir: "${relativeFolder}"],
                                                                          [
                                                                                  $class: 'PreBuildMerge',
                                                                                  options: [
                                                                                          fastForwardMode: 'FF',
                                                                                          mergeRemote: "${relativeFolder}",
                                                                                          mergeStrategy: 'MergeCommand.Strategy',
                                                                                          mergeTarget: "${ifast_Source_branch}"
                                                                                  ]
                                                                          ],
                                                                          [
                                                                                  $class: 'LocalBranch',
                                                                                  localBranch: "${ifast_Source_branch}"
                                                                          ]],
                                                                  gitTool: 'Default',
                                                                  submoduleCfg: [],
                                                                  userRemoteConfigs: [[credentialsId: 'IFDS Build User to access GIT LAB',name: "${relativeFolder}", url: base_repo]]])
                                                    }
                                                    else
                                                    {
                                                        checkout([$class: 'GitSCM',
                                                                  branches: [[name: '*/${ifast_Source_branch}']],
                                                                  doGenerateSubmoduleConfigurations: false,
                                                                  extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: "${relativeFolder}"]],
                                                                  gitTool: 'Default',
                                                                  submoduleCfg: [],
                                                                  userRemoteConfigs: [[credentialsId: 'IFDS Build User to access GIT LAB',name: "${relativeFolder}", url: base_repo]]])

                                                    }

                                                    checkout([$class: 'GitSCM',
                                                              branches: [[name: "refs/tags/${myPreviousRelease}"]],
                                                              doGenerateSubmoduleConfigurations: false,
                                                              extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: "${PreviousrelativeFolder}"]],
                                                              gitTool: 'Default',
                                                              submoduleCfg: [],
                                                              userRemoteConfigs: [[credentialsId: 'IFDS Build User to access GIT LAB',name: "${PreviousrelativeFolder}", url: base_repo]]])

                                                    sshagent (credentials: ['ifdsbuilduserssh'])
                                                            {
                                                                sh """ cd ${WORKSPACE}/${relativeFolder}
       git tag ${myRelease_Value}
       """
                                                            }
                                                }
                                            }
                                }
                        stage('Build Environment')
                                {

                                    steps
                                            {

                                                script
                                                        {
                                                            env.WORKSPACE = pwd()
                                                            myWorkspace = env.WORKSPACE + "/" + "${relativeFolder}"
                                                            println "myworkspace"+myWorkspace
                                                            def affectedFiles = currentBuild.changeSets.collect({ it.items.collect { it.paths } }).flatten()
                                                            //search for .cls files. We need to find only one
                                                            existClassFile = affectedFiles.find
                                                                    {
                                                                        it.path.substring(it.path.lastIndexOf(".") + 1).toLowerCase() == 'cls'
                                                                    }
                                                            def existDFfiles = affectedFiles.find
                                                                    {
                                                                        //search for .df files. We need to find only one
                                                                        it.path.substring(it.path.lastIndexOf(".") + 1).toLowerCase() == 'df'
                                                                    }

                                                            def map
                                                            previousBranch = "${myPreviousRelease}"
                                                            if (myClientsBuild.toUpperCase().contains("ALL CLIENTS")){
                                                                //println "Generatin list of all clients..."
                                                                //def myClients = getAllClients("${myWorkspace}/build/ClientProducts.xml")
                                                                //println "Done. Generated list: ${myClients}"
                                                                map = [REMOVE_COMPILED_CLS: existClassFile == null ? false : true, REBUILD_DB: existDFfiles == null ? false : true, OLD_RELEASE: previousBranch, RELEASE_SHORT: relativeFolder, SELECTED_CLIENTS: myClients ]
                                                            }
                                                            else{
                                                                def xmlpath =myWorkspace+"/build/ClientProducts.xml"
                                                                //def myClients = getAllClients(xmlpath)
                                                                println "Selected client's list: "+myClientsBuild
                                                                map = [REMOVE_COMPILED_CLS: existClassFile == null ? false : true, REBUILD_DB: existDFfiles == null ? false : true, OLD_RELEASE: previousBranch, RELEASE_SHORT: relativeFolder, SELECTED_CLIENTS: myClientsBuild ]
                                                            }
                                                            println ("print statement before return map")
                                                            return map


                                                        }
                                            }
                                }


                        stage('PreBuildStep')
                                {
                                    steps
                                            {
                                                sh """
                                       #cp /tmp/build/build.xml ./${relativeFolder}/build
                                       find ./ -type f -name ".gitkeep" -delete
                                       cd ./${relativeFolder}/build/
                                       git diff --name-status ${myPreviousRelease} ${myRelease_Value} > diff.txt

                                """
                                            }
                                }
                        stage('Artifact Resolver')
                                {

                                    steps
                                            {
                                                artifactResolver artifacts: [artifact(artifactId: 'IFDS-Ant-Tasks',
                                                        groupId: 'AntTasks', targetFileName: 'IFDS-Ant-Tasks.jar', version: '1.0.3')],
                                                        enableRepoLogging: false,
                                                        failOnError: false,
                                                        releaseUpdatePolicy: 'never',
                                                        snapshotUpdatePolicy: 'never',
                                                        targetDirectory: "${relativeFolder}/build/anttasks"
                                            }
                                }
                        stage('Invoke Ant')
                                {
                                    steps
                                            {
                                                sh """
                                      export PATH=$PATH:/opt/progress/dlc
                                      cd ${myWorkspace}/build/
                                                ant -Dmulticomp=${myClientsBuild} -DBuild_Number=${myBuildNumber} -Dsendmail=${sendmail} -f build.xml preprocess
                                      ant -Dforce.db.rebuild="true" -Dremove.cls.objects=${existClassFile} -DSVN.URL=${myPreviousRelease} -Dmulticomp=${myClientsBuild} -Dver=${relativeFolder} -Dextra.df.folder=${myWorkspace1}/TAG_DATA/${myPreviousRelease}/mfs_base/data/ -Dpackage.type=${mypackage} -DBuild_Number=${myBuildNumber} -Djobname=Generic_ifastbase_Build -Dsendmail=${sendmail} -f build.xml preparedb ${mypackage}"""
                                            }
                                    post {
                                        failure {
                                            echo "Invoke Ant => FAILED"
                                            emailWithoutAttachments(params.dev_recepients,'Invoke Ant',devops_group_email: devops_group_email)
                                        }
                                    }

                                }

                        stage("Execute Data Patch Checks") {
                            steps {
                                script {
                                    // Incase of no parameter set it to false which means it should execute
                                    def doSkipDpSyntaxChecks = params.getOrDefault("skipDpSyntaxChecks", false)
                                    if(doSkipDpSyntaxChecks == false) {
                                        try {
                                            // Execute ANT build target dpsyntax
                                            sh """
                                                      cd ${myWorkspace}/build/
                                                      ant -f build-dev.xml dpsyntax
                                                    """
                                        } catch(err) {
                                            throw err
                                        }
                                    } else {
                                        echo "SKIPPED => Data Patch Checks <= SKIPPED"
                                    }
                                }
                            }
                            post {
                                failure {
                                    echo "Data Patch Syntax Checks => FAILED"
                                    emailWithoutAttachments(params.dev_recepients,'Data Patch Syntax Checks',devops_group_email: devops_group_email)
                                    error "Data Patch Syntax Checks Failed So, Pipeline is Terminated"
                                }
                            }
                        }

                        stage("Execute Unit Test") {
                            steps {
                                script {
                                    // Incase of no parameter set it to false which means it should execute
                                    def doSkipUnitTests = params.getOrDefault("skipUnitTests", false)
                                    if (doSkipUnitTests == false) {
                                        try {
                                            // Execute ANT build target test
                                            if (white_list_files) {
                                                sh """
                                                          cd ${myWorkspace}/build/
                                                          ant -f build-dev-internal.xml -Dwhitelist.files=${white_list_files} testsome
                                                        """
                                            }
                                            else {
                                                sh """
                                                          cd ${myWorkspace}/build/
                                                          ant -f build-dev-internal.xml test
                                                        """
                                            }
                                            // JUnitResultArchiver marks build UNSTABLE in case of failure in JUnit result which will be converted to FAILURE
                                            // Ref: https://support.cloudbees.com/hc/en-us/articles/218866667-how-to-abort-a-pipeline-build-if-junit-tests-fail
                                            //email
                                            step([$class: 'JUnitResultArchiver', testResults: '**/target/test-results/results.xml'])
                                        } catch (err) {
                                            throw err
                                        }
                                    } else {
                                        echo "SKIPPED => Unit Testing <= SKIPPED "
                                    }
                                }
                            }
                            post {
                                changed {
                                    script {
                                        if (currentBuild.result == 'UNSTABLE') {
                                            emailWithAttachments(params.dev_recepients, 'iFast Unit Tests', '**/target/test-results/results.xml', devops_group_email: devops_group_email)
                                            //                                            error "Unit Test Failed So, Pipeline is Terminated"
                                            currentBuild.result = 'SUCCESS'
                                        }
                                    }
                                }
                            }
                        }

                        stage('checkout Java Transform')
                                {
                                    steps
                                            {
                                                println "JavaCRSXMLTransform Branch: ${JAVA_CRSTransform_branch}"
                                                script{
                                                    echo pwd()
                                                }
                                                checkout([$class: 'GitSCM',
                                                          branches: [[name: "*/${JAVA_CRSTransform_branch}"]],
                                                          doGenerateSubmoduleConfigurations: false,
                                                          extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: "${relativeFolder}/CRSTransform"]],
                                                          gitTool: 'Default',
                                                          submoduleCfg: [],
                                                          userRemoteConfigs: [[credentialsId: 'IFDS Build User to access GIT LAB',name: "${relativeFolder}/CRSTransform",  url: JAVA_CRSTransform_repo]]])
                                                checkout([$class: 'GitSCM',
                                                          branches: [[name: "*/${JAVA_FATCATransform_branch}"]],
                                                          doGenerateSubmoduleConfigurations: false,
                                                          extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: "${relativeFolder}/FATCATransform"]],
                                                          gitTool: 'Default',
                                                          submoduleCfg: [],
                                                          userRemoteConfigs: [[credentialsId: 'IFDS Build User to access GIT LAB',name: "${relativeFolder}/FATCATransform",  url: JAVA_FATCATransform_repo]]])
                                                script
                                                        {
                                                            sshagent (credentials: ['ifdsbuilduserssh'])
                                                                    {
                                                                        sh """
                                                                        cd ${WORKSPACE}/${relativeFolder}/CRSTransform
                                                                 git tag ${myRelease_Value}
                                                                 cd ${WORKSPACE}/${relativeFolder}/FATCATransform
                                                                 git tag ${myRelease_Value}
                                                      """
                                                                    }
                                                        }
                                            }
                                }

                        stage('Invoke ant for Java Transform')
                                {
                                    steps
                                            {
                                                sh """
                                                cd ${WORKSPACE}/${relativeFolder}/CRSTransform/
                                                ant -f build.xml clean build
                                                cd ${WORKSPACE}/${relativeFolder}/FATCATransform/
                                                ant -f build.xml clean build test
                                            """

                                            }
                                    post {
                                        failure {
                                            echo "Invoke Ant for Java Transform => FAILED"
                                            emailWithoutAttachments(params.dev_recepients,'Invoke Ant for Java Transform',devops_group_email: devops_group_email)
                                        }
                                    }
                                }


                        stage('Copy jar and Zip files')
                                {
                                    steps
                                            {

                                                script
                                                        {

                                                            zip_name="JavaTransform_APE_Rel_"+relativeFolder
                                                        }
                                                println "Zip_name" +zip_name
                                                sh """
                                                mkdir -p ${myBaseDirectory}/${relativeFolder}
                                                chmod 755 ${WORKSPACE}/${relativeFolder}/FATCATransform/target/*.jar  ${WORKSPACE}/${relativeFolder}/CRSTransform/target/*.jar
                                                zip -j ${myBaseDirectory}/${relativeFolder}/${zip_name}.zip   ${WORKSPACE}/${relativeFolder}/FATCATransform/target/*.jar  ${WORKSPACE}/${relativeFolder}/CRSTransform/target/*.jar

                                                cd ${myWorkspace}/target
                                                echo "Current directory i ${myWorkspace}/target and copying packages"
                                                if [ "${mypackage}" == "packageDelta" ]
                                                    then
                                                        cp -f Data_Package_${relativeFolder}_${myBuildNumber}.zip Delta_Source_${relativeFolder}_${myBuildNumber}.zip ${myBaseDirectory}/${relativeFolder}
                                                    else

                                                        cp -f Data_Package_${relativeFolder}_${myBuildNumber}.zip Full_Source_${relativeFolder}_${myBuildNumber}.zip ${myBaseDirectory}/${relativeFolder}

                                                        splitClient="${myClientsBuild}"
                                                            cli=\$(echo \$splitClient | tr "," "\n")
                                                            for ci in \$cli
                                                            do
                                                                echo "> \${ci}"
                                                                cp -f Full/"\${ci}"/Full_Binary_"\${ci}"_${relativeFolder}_${myBuildNumber}.zip ${myBaseDirectory}/${relativeFolder}
                                                            done
                                                fi
                    """

                                            }
                                }

                        stage('checkout of NSCC code')
                                {
                                    steps
                                            {
                                                checkout([$class: 'GitSCM',
                                                          branches: [[name: "*/${NSCC_branch}"]],
                                                          doGenerateSubmoduleConfigurations: false,
                                                          extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: "${relativeFolder}/NSCCgen"]],
                                                          gitTool: 'Default',
                                                          submoduleCfg: [],
                                                          userRemoteConfigs: [[credentialsId: 'IFDS Build User to access GIT LAB',name: "${relativeFolder}/NSCCgen",  url: NSCC_repo]]])
                                                script
                                                        {
                                                            sshagent (credentials: ['ifdsbuilduserssh'])
                                                                    {
                                                                        sh """
                                                                        cd ${WORKSPACE}/${relativeFolder}/NSCCgen
                                                                 git tag ${myRelease_Value}
                                                                 """
                                                                    }
                                                        }
                                            }
                                }



                        stage('Invoke ant for Nscc generation')
                                {
                                    steps
                                            {

                                                script
                                                        {

                                                            zip_name="nscgen_Rel_"+relativeFolder
                                                        }

                                                sh """
                                                cd ${WORKSPACE}/${relativeFolder}/NSCCgen/mfs_base/product/
                                                zip -r ${myBaseDirectory}/${relativeFolder}/${zip_name}.zip  .
                                      """

                                            }
                                }

                        stage('Checkout Nexus Dependency script')
                                {
                                    steps
                                            {
                                                checkout([$class: 'GitSCM', branches: [[name: "*/${nexus_dependency_script_branch}"]],
                                                          doGenerateSubmoduleConfigurations: false,
                                                          extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: "${relativeFolder}/Dependency"]],
                                                          gitTool: 'Default', submoduleCfg: [],
                                                          userRemoteConfigs: [[credentialsId: 'IFDS Build User to access GIT LAB', name: "${relativeFolder}/Dependency", url: nexus_dependency_script_repo]]])
                                            }
                                }

                        stage('Script for upload dependencies to nexus')
                                {

                                    steps
                                            {
                                                configFileProvider([configFile(fileId: maven_settings_file_id, variable: 'MAVEN_SETTINGS')]) {

                                                    sh """
                                                    cd ${WORKSPACE}/${relativeFolder}/Dependency
                                                    sh buildpom.sh ifastbase ifastbase-${myRelease_Value} ifastbase-${myPreviousRelease} ${Product_Changes} $MAVEN_SETTINGS
                                                    rm -rf pom.xml
                                                    sh getdependency ifastbase-${myRelease_Value} apeupgradetable ${SDLC}
                                                    sh getdependency ifastbase-${myRelease_Value} " " ${SDLC}
                                                """
                                                }
                                            }
                                    post {
                                        failure {
                                            echo "Upload dependencies to nexus => FAILED"
                                            emailWithoutAttachments(devops_group_email, 'Upload dependencies to nexus', devops_group_email: devops_group_email)
                                        }
                                    }
                                }


                        stage('Checkout Docker script')
                                {
                                    steps
                                            {
                                                checkout([$class: 'GitSCM',
                                                          branches: [[name: "*/${docker_script_branch}"]],
                                                          doGenerateSubmoduleConfigurations: false,
                                                          extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: "${relativeFolder}/DockerImageBuild"]],
                                                          gitTool: 'Default', submoduleCfg: [],
                                                          userRemoteConfigs: [[credentialsId: 'SCMJenkins', name: 'IFDSBUILD', url: docker_script_repo]]])

                                            }
                                }

                        stage('Build Docker Images')
                                {
                                    steps
                                            {
                                                script
                                                        {
                                                            withCredentials([usernamePassword(credentialsId: 'jfrog_auth_credential', usernameVariable: 'JFROG_USERNAME', passwordVariable: 'JFROG_PASSWORD')]) {
                                                                sh """
                                                                    cd ${WORKSPACE}
                                                                    mkdir keys
                                                                    cd ${WORKSPACE}/keys
                                                                    curl -v -u ${JFROG_USERNAME}:${JFROG_PASSWORD} -O "http://si1ocevar01.clustere.ifdsgroup.ca/artifactory/generic-local/scmbuild/keys/id_rsa_ifdsgituser"
                                                                    curl -v -u ${JFROG_USERNAME}:${JFROG_PASSWORD} -O "http://si1ocevar01.clustere.ifdsgroup.ca/artifactory/generic-local/scmbuild/keys/known_hosts"
                                                                    chmod 600 id_rsa_ifdsgituser
                                                                    cd ${WORKSPACE}/${relativeFolder}/DockerImageBuild/docker
                                                                    chmod -R 777 ${WORKSPACE}/${relativeFolder}/DockerImageBuild/docker

                                                                    if [ ${myDBUPGRADE} == YES ]
                                                                    then

                                                                    echo "****************************************************************************"
                                                                    echo "${myRelease_Value}"
                                                                    echo "${myBuildNumber}"
                                                                    echo "${myClientsBuild}"
                                                                    echo "${myNum_Threads}"
                                                                    echo "${myImageUpgrade}"
                                                                    echo "${myTag_BaseImage}"
                                                                    echo "${APEWS_URL}"
                                                                    echo "${APEWS_PORT}"
                                                                    echo "${dbrepo_mount_path}"
                                                                    echo "${PCT_pkg_mount_path}"
                                                                    echo "${BaseImageName}"
                                                                    echo "${nexusUrl}"
                                                                    echo "${OE_GIT_REPO}"
                                                                    echo "${OE_DOCKER_FOLDER}"
                                                                    echo "${dockerRegistry}"
                                                                    echo "${dockerNamespace}"
                                                                    echo "**************************************************************************"

                                                                    sh dockerimagebuild.sh "${myRelease_Value}" "${myBuildNumber}" "${myClientsBuild}" "${myNum_Threads}" DB "${myImageUpgrade}" "${myTag_BaseImage}" "${APEWS_URL}" "${APEWS_PORT}" "${dbrepo_mount_path}" "${WORKSPACE}" "${BaseImageName}" "${nexusUrl}" "${OE_GIT_REPO}" "${OE_DOCKER_FOLDER}" "${dockerRegistry}" "${dockerNamespace}"|tee -a ${myRelease_Value}.builddelta.DB.log
                                                                    else
                                                                    sh dockerimagebuild.sh "${myRelease_Value}" "${myBuildNumber}" "${myClientsBuild}" "${myNum_Threads}" "" "${myImageUpgrade}" "${myTag_BaseImage}" "${APEWS_URL}" "${APEWS_PORT}" "${dbrepo_mount_path}" "${WORKSPACE}" "${BaseImageName}" "${nexusUrl}" "${OE_GIT_REPO}" "${OE_DOCKER_FOLDER}" "${dockerRegistry}" "${dockerNamespace}"|tee -a ${myRelease_Value}.builddelta.DB.log
                                                                    fi
                                                                """
                                                            }
                                                        }
                                            }
                                    post {
                                        failure {
                                            echo "Build Docker Images => FAILED"
                                            emailWithoutAttachments(devops_group_email,'Build Docker Images',devops_group_email: devops_group_email)
                                        }
                                    }
                                }

                        stage('Push ifastbase image to Jfrog Artifactory')
                                {
                                    steps {
                                        withCredentials([string(credentialsId: "jFrogURL_Token", variable: 'jFrogURL_Token')])
                                                {
                                                    script
                                                            {
                                                                splitClient1 = clients.split(',')
                                                                for(i =0; i < splitClient1.size(); i++)
                                                                {
                                                                    println splitClient1[i]
                                                                    myClientsBuild =splitClient1[i]

                                                                    sh """
                                                                    $sudo docker login -u scmapedeploy ${jFrogURL_Token} ${jfrogRegistryurl} &&
                                                               $sudo docker tag  ifastbase/${myRelease_Value}_${myClientsBuild} ${myRelease_Value}_${myClientsBuild} &&
                                                                    $sudo docker tag  ${myRelease_Value}_${myClientsBuild} ${jfrogRegistryurl}/${dockerNamespace}/${myRelease_Value}_${myClientsBuild} &&
                                                                    $sudo docker push ${jfrogRegistryurl}/${dockerNamespace}/${myRelease_Value}_${myClientsBuild}
                                                          """
                                                                }
                                                            }
                                                }
                                    }
                                    post {
                                        failure {
                                            echo "Push ifastbase image to Jfrog Artifactory => FAILED"
                                            emailWithoutAttachments(devops_group_email,'Push ifastbase image to Jfrog Artifactory',devops_group_email: devops_group_email)
                                        }
                                    }
                                }

                        stage('Pushing Git tags')
                                {
                                    steps{
                                        script
                                                {
                                                    sshagent (credentials: ['ifdsbuilduserssh'])
                                                            {
                                                                sh """
                                                                cd ${myWorkspace}
                                                         git remote add origin git@gitrepop1:${repo_name}
                                                         git push origin ${myRelease_Value}
                                                         cd ${WORKSPACE}/${relativeFolder}/CRSTransform
                                                         git remote add origin git@gitrepop1:iFastUtilities/JavaCRSXMLTransform.git
                                                         git push origin ${myRelease_Value}
                                                         cd ${WORKSPACE}/${relativeFolder}/FATCATransform
                                                         git remote add origin git@gitrepop1:iFastUtilities/JavaXMLTransform.git
                                                         git push origin ${myRelease_Value}
                                                         cd ${WORKSPACE}/${relativeFolder}/NSCCgen
                                                         git remote add origin git@gitrepop1:iFastUtilities/NSCCGenerator.git
                                                         git push origin ${myRelease_Value}
                                                   """
                                                            }
                                                }
                                    }
                                    post {
                                        failure {
                                            echo "ifastbase Git Tag creation and push to git repo => FAILED"
                                            emailWithoutAttachments(devops_group_email,'ifastbase Git Tag creation and push to git repo',devops_group_email: devops_group_email)
                                        }
                                    }
                                }
                    }
        }


//Parse ClientProduct-like xml file and return all clients found
def getAllClients(xmlFile)
{
    def modifiedXml

    try
    {
        println "inside client function"
        if (env['NODE_NAME'] == null)
        {
            error "envvar NODE_NAME is not set, probably not inside an node {} or running an older version of Jenkins!";
        }
        else if (env['NODE_NAME'].equals("master"))
        {
            fp = new FilePath(new File(xmlFile));
        }
        else
        {
            fp= new FilePath(Jenkins.getInstance().getComputer(env['NODE_NAME']).getChannel(), xmlFile);
        }
        modifiedXml = fp.readToString()
        println "modifiedxml is ${modifiedXml}"
    }
    catch(Exception e)
    {
        println(e.toString())
        println(e.getMessage())
        throw e
    }

    def list = []
    def clientproduct = new XmlParser().parseText(modifiedXml)
    println "check 1"
    println "check 2:"+clientproduct
    println "check 3:"+clientproduct.ClientProductModules.Client
    def cliententries = clientproduct.ClientProductModules.Client.size()
    println "cliententries"+cliententries
    println "cliententries"+ cliententries
    for (int i = 0; i< cliententries; i++)
    {
        def clientEntry = clientproduct.ClientProductModules.Client[i]
        def clientName = clientEntry.@ClientCode
        list << clientName
    }
    return list.join(',')
}


// 1) This function will run 'svn diff' command and parse it for added files
// 2) It will run 'svn log' on each file found in step 2) and parse/sort the output
// 3) It will generate output XML file in the same format as 'svn log' with oldest revision of the files from step 2)
def generateLog(fromURL, toURL, output, selection)
{
    def xmlDoc
    def skip = false
    def slarray = selection.split(",")
    println "Entered Generate Log function"
    xmlInput = runSvnCmd("/opt/CollabNet_Subversion/bin/svn diff --username=IFDSbuild --password=build --summarize --xml ${fromURL} ${toURL}")
    println "After Diff command"
    def baseLength = fromURL.length()
    def difflist = new XmlParser().parseText(xmlInput)
    def entryCount = difflist.paths.path.size()
    for (int i = 0; i < entryCount; i++)
    {
        def entry = difflist.paths.path[i]
        kind = entry.@kind
        if(kind == "file")
        {
            action = entry.@item
            //Search for files that have been added to the later branch
            if(action == "added")
            {
                def relativePath = entry.text().substring(baseLength)
                if(selection)
                {
                    skip = true
                    for(String test: slarray)
                    {
                        if(relativePath.contains(test))
                        {
                            skip = false
                            break
                        }
                    }
                }
                if(!skip)
                {
                    def filePath = toURL + relativePath
                    xmlLog = runSvnCmd("/opt/CollabNet_Subversion/bin/svn log --username=IFDSbuild --password=build -v --xml ${filePath}")
                    def loglist = new XmlParser().parseText(xmlLog)
                    println ("After svn log command and filepath is ${filePath}")
                    def logentryCount = loglist.logentry.size()
                    for (int j = 0; j < logentryCount; j++)
                    {
                        def logentry = loglist.logentry[j]
                        def revision = logentry.@revision
                        def date = logentry.date.text()
                        def pathcount = logentry.paths.path.size()
                        for(int k = 0; k < pathcount; k++)
                        {
                            pathentry = logentry.paths.path[k]
                            def fullpath = pathentry.text()
                            if(fullpath.contains(relativePath))
                            {
                                def pathkind = pathentry.@kind
                                def pathaction = pathentry.@action
                                //println "Revision: ${revision}, Action: ${pathaction}, File: ${fullpath}"
                            }
                            else
                            {
                                //println "${fullpath} Does not contain ${relativePath}"
                                def myparent = pathentry.parent()
                                if(myparent.remove(pathentry))
                                {
                                    pathcount--
                                    k--
                                }
                            }
                        }
                        if(loglist.logentry[j].paths.path.size() == 0)
                        {
                            def myparent = logentry.parent()
                            if(myparent.remove(logentry))
                            {
                                j--
                                logentryCount--
                            }
                        }
                    }
                    if(xmlDoc)
                    {
                        logentryCount = loglist.logentry.size()
                        for (int j = 0; j< logentryCount; j++)
                        {
                            xmlDoc.append(loglist.logentry[j])
                        }
                    }
                    else
                    {
                        xmlDoc = loglist
                    }
                }
            }
        }
    }
    //}

    //now save xml
    if(xmlDoc)
    {

        try
        {
            if (env['NODE_NAME'] == null)
            {
                error "envvar NODE_NAME is not set, probably not inside an node {} or running an older version of Jenkins!";
            }
            else if (env['NODE_NAME'].equals("master"))
            {
                println "Local workspace detected..."
                fp = new FilePath(new File(output))
            }
            else
            {
                println "Remote workspace detected..."
                fp= new FilePath(Jenkins.getInstance().getComputer(env['NODE_NAME']).getChannel(), output);
            }

            def sw = new StringWriter()
            new XmlNodePrinter(new PrintWriter(sw)).print(xmlDoc)
            def modifiedXml = sw.toString()
            fp.write(modifiedXml, null);
        }
        catch(Exception e)
        {
            println(e.toString());
            println(e.getMessage());
            throw e;
        }
    }
}

//targetSvnUrl - URL that contains subfolders that you want to search
//svnURLselected - URL of the selected branch
//keyword - substring of the branch name that eligible for search
def findPreviousRelease(targetSvnUrl, svnURLselected, keyword)
{
    xmlInput = runSvnCmd("/opt/CollabNet_Subversion/bin/svn list --username=IFDSbuild --password=build --xml ${targetSvnUrl}")
    def myList = []
    def folderlists = new XmlParser().parseText(xmlInput)
    def entryCount = folderlists.list.entry.size()
    for (int i = 0; i< entryCount; i++)
    {
        def entry = folderlists.list.entry[i]
        entryName = entry.name.text()
        def tempSvnUrl = "${targetSvnUrl}/${entryName}"
        tempXML = runSvnCmd("/opt/CollabNet_Subversion/bin/svn list --username=IFDSbuild --password=build --xml ${tempSvnUrl}")
        def branchlists = new XmlParser().parseText(tempXML)
        def branchCount = branchlists.list.entry.size()
        for (int j = 0; j< branchCount; j++)
        {
            def branch = branchlists.list.entry[j]
            branchName = branch.name.text()
            def partialName = "${entryName}/${branchName}"
            def fullName = "${targetSvnUrl}/${partialName}"
            myList << fullName
        }
    }
    myList.sort()
    def selectedIndex = myList.findIndexOf{name -> name == svnURLselected}
    for (int i = selectedIndex - 1; i > 0; i--)
    {
        def name = myList[i].substring(myList[i].lastIndexOf('/')+1)
        if(name.toLowerCase().contains(keyword))
        {
            return myList[i]
        }
    }
    return  "Unknown"
}

//Getting output from SVN command

//Getting output from SVN command

def runSvnCmd(command)
{
    /* def ant = new AntBuilder()
    ant.echo('Entered runsvncommand function')
    ant.exec(
    outputproperty:"cmdOut",
    errorproperty: "cmdErr",
    resultproperty:"cmdExit",
    failonerror: "true",
   executable: '/opt/CollabNet_Subversion/bin/svn'
  )
    arg(line:command)
  output = "${ant.project.properties.cmdOut}"
  return output */
    def output = command.execute().text.trim()
    return output
}

def emailWithoutAttachments(Map kwargs, email_recepients, stage) {
    def devops_group_email = kwargs.devops_group_email ?: 'TorontoCICDAutomation@ifdsgroup.com'
    def custom_message = kwargs.custom_message ?: "${stage} stage Failed. Plese check the below-mentioned build URL"
    def custom_subject = kwargs.custom_subject ?: "${stage} Failed - Job: ${JOB_NAME}, Build: ${BUILD_NUMBER}"
    def attachlog = kwargs.attachlog ?: true
    def compresslog = kwargs.compresslog ?: true
    emailext attachLog: attachlog,
            body: """Hi Team,

${custom_message}

Build URL - ${BUILD_URL}

This is an automatically generated email.
Thanks,
DevOps Team""",
            compressLog: compresslog,
            recipientProviders: [[$class: 'DevelopersRecipientProvider']],
            subject: custom_subject,
            from: 'DevOps Team <noreply@ifdsgroup.com>',
            to: "${email_recepients},${devops_group_email}"
}

def emailWithAttachments(Map kwargs, email_recepients, stage, attachments_pattern) {
    def devops_group_email = kwargs.devops_group_email ?: 'TorontoCICDAutomation@ifdsgroup.com'
    def custom_message = kwargs.custom_message ?: "${stage} stage Failed. Plese check the below-mentioned build URL"
    def custom_subject = kwargs.custom_subject ?: "${stage} Failed - Job: ${JOB_NAME}, Build: ${BUILD_NUMBER}"
    def attachlog = kwargs.attachlog ?: true
    def compresslog = kwargs.compresslog ?: true
    emailext attachLog: attachlog,
            attachmentsPattern: attachments_pattern,
            body: """Hi Team,

${custom_message}

Build URL - ${BUILD_URL}

This is an automatically generated email.
Thanks,
DevOps Team""",
            compressLog: compresslog,
            recipientProviders: [[$class: 'DevelopersRecipientProvider']],
            subject: custom_subject,
            from: 'DevOps Team <noreply@ifdsgroup.com>',
            to: "${email_recepients},${devops_group_email}"
}