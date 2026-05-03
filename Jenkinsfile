pipeline {
    agent any

    environment {
        CHANGED_SERVICES = 'none'
    }

    tools {
        maven 'Maven-3.9'
    }

    options {
        skipDefaultCheckout()
    }

    stages {

        stage('Checkout') {
            steps {
                cleanWs()
                checkout scm
            }
        }

        stage('Detect Changed Services') {
            steps {
                script {
                    sh 'git fetch --no-tags --prune origin +refs/heads/main:refs/remotes/origin/main || true'

                    def changedFiles = sh(
                        script: '''
                            if git rev-parse --verify origin/main >/dev/null 2>&1; then
                                git diff --name-only origin/main...HEAD
                            else
                                echo "origin/main not found, fallback to latest commit diff" >&2
                                git diff --name-only HEAD~1..HEAD || git ls-files
                            fi
                        ''',
                        returnStdout: true
                    ).trim()

                    echo "Changed files:\n${changedFiles}"

                    def services = [
                        'cart', 'customer', 'delivery', 'inventory', 'location',
                        'media', 'order', 'payment', 'payment-paypal', 'product',
                        'promotion', 'rating', 'recommendation', 'search', 'tax',
                        'backoffice-bff', 'storefront-bff', 'identity',
                        'backoffice', 'storefront'
                    ]

                    def affected = services.findAll { svc ->
                        changedFiles && (changedFiles =~ /(?m)^${java.util.regex.Pattern.quote(svc)}\//)
                    }

                    echo "Affected services detected: ${affected}"

                    env.CHANGED_SERVICES = affected.isEmpty() ? 'none' : affected.join(',')
                    if (env.CHANGED_SERVICES == 'none') {
                        echo 'No service changes detected. Skipping build/test.'
                    } else {
                        echo "Services to build/test: ${env.CHANGED_SERVICES}"
                    }
                }
            }
        }

        stage('Security Scan') {
            steps {
                echo 'Running GitLeaks secret scan'
                script {
                    sh '''
                        curl -sSL https://github.com/gitleaks/gitleaks/releases/download/v8.18.4/gitleaks_8.18.4_linux_x64.tar.gz -o gitleaks.tar.gz
                        tar -xzf gitleaks.tar.gz
                        chmod +x gitleaks
                    '''

                    sh './gitleaks detect --source=. --config=gitleaks.toml --log-opts="origin/main..HEAD" --report-format=json --report-path=gitleaks-report.json --exit-code=1'
                }
            }
            post {
                always {
                    archiveArtifacts artifacts: 'gitleaks-report.json', fingerprint: true, allowEmptyArchive: true
                }
            }
        }

        stage('Snyk Dependency Scan') {
            when {
                expression { env.CHANGED_SERVICES?.trim() && env.CHANGED_SERVICES != 'none' }
            }
            steps {
                withCredentials([string(credentialsId: 'snyk-token', variable: 'SNYK_TOKEN')]) {
                    script {
                        echo "--- Initializing Snyk CLI Environment ---"
                        sh '''
                            curl -sSL https://static.snyk.io/cli/latest/snyk-linux -o snyk
                            chmod +x snyk
                            chmod +x mvnw || true
                        '''
                        echo "--- Pre-installing Maven Parent and Common-library ---"
                        sh "./mvnw install -N -DskipTests -Drevision=1.0-SNAPSHOT"
                        sh "./mvnw install -pl common-library -am -DskipTests -Drevision=1.0-SNAPSHOT"

                        def services = (env.CHANGED_SERVICES ?: '').split(',').findAll { it?.trim() }
                        services.each { svc ->
                            echo "--- Executing Snyk Scan for: ${svc} ---"
                            def reportFile = "${env.WORKSPACE}/snyk-${svc}-report.json"
                            if (fileExists("${svc}/pom.xml")) {
                                sh "./snyk test --file=${svc}/pom.xml --severity-threshold=high --command=./mvnw --json-file-output=${reportFile} || true"
                            } else if (fileExists("${svc}/package.json")) {
                                dir("${svc}") {
                                    sh "npm install || true"
                                }
                                sh "./snyk test --file=${svc}/package-lock.json --severity-threshold=high --json-file-output=${reportFile} || true"
                            } else {
                                echo "Skipping ${svc}: No valid manifest file (pom.xml/package.json) detected."
                            }
                        }
                    }
                }
            }
            post {
                always {
                    archiveArtifacts artifacts: 'gitleaks-report.json', fingerprint: true, allowEmptyArchive: true
                    archiveArtifacts artifacts: 'snyk-*-report.json', fingerprint: true, allowEmptyArchive: true
                }
            }
        }

        stage('Test Phase') {
            when {
                expression { env.CHANGED_SERVICES?.trim() && env.CHANGED_SERVICES != 'none' }
            }
            steps {
                script {
                    def services = (env.CHANGED_SERVICES ?: '').split(',').findAll { it?.trim() }
                    services.each { svc ->
                        echo "Running tests for: ${svc}"
                        sh "mvn verify jacoco:report -DskipITs -f pom.xml -pl ${svc} -am -U -Drevision=1.0-SNAPSHOT"
                    }
                }
            }
            post {
                always {
                    script {
                        def services = (env.CHANGED_SERVICES ?: '')
                            .split(',')
                            .collect { it.trim() }
                            .findAll { it && it != 'none' }
                        services.each { svc ->
                            junit(
                                testResults: "${svc}/target/surefire-reports/*.xml",
                                allowEmptyResults: true
                            )
                        }
                    }
                }
            }
        }

        stage('Coverage Quality Gate') {
            when {
                expression { env.CHANGED_SERVICES?.trim() && env.CHANGED_SERVICES != 'none' }
            }
            steps {
                script {
                    def services = (env.CHANGED_SERVICES ?: '').split(',').findAll { it?.trim() }
                    services.each { svc ->
                        def reportPath = "${svc}/target/site/jacoco/jacoco.csv"

                        def fileExists = sh(script: "test -f ${reportPath} && echo 'yes' || echo 'no'", returnStdout: true).trim()

                        if (fileExists == 'no') {
                            echo "[${svc}] WARNING: jacoco.csv not found at ${reportPath}"
                            echo "[${svc}] Listing target/site to debug:"
                            sh "find ${svc}/target -name '*.csv' -o -name 'jacoco*' 2>/dev/null || echo 'No jacoco files found'"
                            error("[${svc}] JaCoCo report missing - check if jacoco-maven-plugin is configured in ${svc}/pom.xml")
                        }

                        def coverage = sh(script: """
                            awk -F',' 'NR>1 {
                                missed  += \$8;
                                covered += \$9
                            } END {
                                if (missed+covered > 0)
                                    printf "%.0f", covered/(missed+covered)*100;
                                else
                                    print 0
                            }' ${reportPath}
                        """, returnStdout: true).trim().toInteger()

                        echo "[${svc}] Line Coverage: ${coverage}%"

                        if (coverage <= 70) {
                            error("[${svc}] Coverage ${coverage}% <= 70%. Pipeline failed!")
                        }
                    }
                }
            }
        }

        stage('SonarQube Scan') {
            steps {
                withCredentials([string(credentialsId: 'sonar-token', variable: 'SONAR_TOKEN')]) {
                    script {
                        def services = (env.CHANGED_SERVICES ?: '')
                            .split(',')
                            .collect { it.trim() }
                            .findAll { it && it != 'none' }
                        def plModules = services ? services.join(',') : ''
                        withEnv(['SONAR_SCANNER_OPTS=-Dsonar.scanner.internal.useHttp2=false']) {
                            if (plModules) {
                                sh """
                                    mvn -DskipTests -DskipITs compile org.sonarsource.scanner.maven:sonar-maven-plugin:5.5.0.6356:sonar \\
                                        -f pom.xml \\
                                        -pl ${plModules} -am \\
                                        -Drevision=1.0-SNAPSHOT \\
                                        -Dsonar.token=\$SONAR_TOKEN \\
                                        -Dsonar.organization=luc1fe4 \\
                                        -Dsonar.projectKey=luc1fe4_yas \\
                                        -Dsonar.coverage.jacoco.xmlReportPaths=**/target/site/jacoco/jacoco.xml \\
                                        -Dsonar.scanner.connectTimeout=600 \\
                                        -Dsonar.scanner.socketTimeout=600 \\
                                        -Dsonar.scanner.responseTimeout=600 \\
                                        -Dsonar.scanner.internal.useHttp2=false
                                """
                            } else {
                                sh """
                                    mvn -DskipTests -DskipITs compile org.sonarsource.scanner.maven:sonar-maven-plugin:5.5.0.6356:sonar \\
                                        -f pom.xml \\
                                        -Drevision=1.0-SNAPSHOT \\
                                        -Dsonar.token=\$SONAR_TOKEN \\
                                        -Dsonar.organization=luc1fe4 \\
                                        -Dsonar.projectKey=luc1fe4_yas \\
                                        -Dsonar.coverage.jacoco.xmlReportPaths=**/target/site/jacoco/jacoco.xml \\
                                        -Dsonar.scanner.connectTimeout=600 \\
                                        -Dsonar.scanner.socketTimeout=600 \\
                                        -Dsonar.scanner.responseTimeout=600 \\
                                        -Dsonar.scanner.internal.useHttp2=false
                                """
                            }
                        }
                    }
                }
            }
            post {
                always {
                    archiveArtifacts artifacts: 'sonarqube-test-report.json',
                                    fingerprint: true,
                                    allowEmptyArchive: true
                }
            }
        }

        stage('Build Phase') {
            when {
                expression { env.CHANGED_SERVICES?.trim() && env.CHANGED_SERVICES != 'none' }
            }
            steps {
                script {
                    def services = (env.CHANGED_SERVICES ?: '').split(',').findAll { it?.trim() }
                    services.each { svc ->
                        echo "Building: ${svc}"
                        sh "mvn clean package -DskipTests -f pom.xml -pl ${svc} -am -U -Drevision=1.0-SNAPSHOT"
                    }
                }
            }
            post {
                success {
                    script {
                        def services = (env.CHANGED_SERVICES ?: '').split(',').findAll { it?.trim() }
                        services.each { svc ->
                            archiveArtifacts artifacts: "${svc}/target/*.jar",
                                             allowEmptyArchive: true
                        }
                    }
                }
            }
        }
    }

    post {
        success {
            echo 'CI Pipeline PASSED - All stages completed successfully.'
        }
        failure {
            echo 'CI Pipeline FAILED - Check logs above for details.'
        }
        always {
            script {
                try {
                    cleanWs()
                } catch (Throwable e) {
                    echo "cleanWs skipped: ${e.getClass().getSimpleName()}"
                }
            }
        }
    }
}
