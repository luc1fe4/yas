def changedServices = 'none'
def frontendServices = ['backoffice', 'storefront']

pipeline {
    agent any

    parameters {
        string(name: 'DIFF_BASE_BRANCH', defaultValue: 'main', description: 'Nhanh goc de so sanh changed files (vd: main, develop)')
    }

    environment {
        DIFF_BASE_BRANCH = "${params.DIFF_BASE_BRANCH ?: 'main'}"
    }

    stages {

        stage('Detect Changed Services') {
            steps {
                script {
                    def diffBaseBranch = (env.DIFF_BASE_BRANCH ?: 'main').trim()
                    if (!diffBaseBranch) {
                        diffBaseBranch = 'main'
                    }
                    if (!(diffBaseBranch ==~ /^[A-Za-z0-9._\/-]+$/)) {
                        error("DIFF_BASE_BRANCH khong hop le: ${diffBaseBranch}")
                    }

                    def diffBaseRef = "origin/${diffBaseBranch}"
                    echo "Using base branch for diff: ${diffBaseRef}"

                    sh "git fetch --no-tags --prune origin +refs/heads/${diffBaseBranch}:refs/remotes/origin/${diffBaseBranch} || true"

                    def changedFiles = sh(
                        script: """
                            if git rev-parse --verify ${diffBaseRef} >/dev/null 2>&1; then
                                git diff --name-only ${diffBaseRef}...HEAD
                            else
                                echo "${diffBaseRef} not found, fallback to latest commit diff" >&2
                                git diff --name-only HEAD~1..HEAD || git ls-files
                            fi
                        """,
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

                    changedServices = affected.isEmpty() ? 'none' : affected.join(',')
                    if (changedServices == 'none') {
                        echo "No service changes detected. Skipping build/test."
                    } else {
                        echo "Services to build/test: ${changedServices}"
                    }
                }
            }
        }

        stage('Frontend Build Start Test') {
            when {
                expression {
                    def services = (changedServices ?: '').split(',').findAll { it?.trim() }
                    services.any { frontendServices.contains(it) }
                }
            }
            tools {
                nodejs 'nodejs'
            }
            steps {
                script {
                    def services = (changedServices ?: '').split(',').findAll { it?.trim() }
                    def frontendChanged = services.findAll { frontendServices.contains(it) }

                    frontendChanged.eachWithIndex { svc, idx ->
                        def port = 3100 + idx
                        echo "Running frontend build/start/test for: ${svc} on port ${port}"

                        sh """
                            set -e;
                            apt-get update -y || true;
                            apt-get install -y libatomic1 || true;
                            node --version;
                            npm --version;
                            cd ${svc};
                            npm ci;
                            npm run build;
                            npm run start -- -p ${port} > ../${svc}-start.log 2>&1 &
                            APP_PID=\$!;
                            trap 'kill \$APP_PID >/dev/null 2>&1 || true; wait \$APP_PID >/dev/null 2>&1 || true' EXIT;
                            sleep 15;
                            npm run test -- --ci;
                        """
                    }
                }
            }
        }

        stage('Security Scan') {
            steps {
                echo "--- Dang tai va thuc thi GitLeaks ---"
                script {
                    def diffBaseBranch = (env.DIFF_BASE_BRANCH ?: 'main').trim()
                    if (!diffBaseBranch) {
                        diffBaseBranch = 'main'
                    }

                    sh '''
                        curl -sSL https://github.com/gitleaks/gitleaks/releases/download/v8.18.4/gitleaks_8.18.4_linux_x64.tar.gz -o gitleaks.tar.gz
                        tar -xzf gitleaks.tar.gz
                        chmod +x gitleaks
                    '''

                    sh "./gitleaks detect --source=. --config=gitleaks.toml --log-opts=\"origin/${diffBaseBranch}..HEAD\" --report-format=json --report-path=gitleaks-report.json --exit-code=1"
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
                expression { changedServices?.trim() && changedServices != 'none' }
            }
            steps {
                withCredentials([string(credentialsId: 'snyk-token', variable: 'SNYK_TOKEN')]) {
                    script {
                        echo "--- Initializing Snyk CLI Environment ---"
                        sh '''
                            curl -sSL https://static.snyk.io/cli/latest/snyk-linux -o snyk
                            chmod +x snyk
                            chmod +x mvnw
                        '''
                        echo "--- Pre-installing Maven Parent and Common-library ---"
                        sh "./mvnw install -N -DskipTests -Drevision=1.0-SNAPSHOT"
                        sh "./mvnw install -pl common-library -am -DskipTests -Drevision=1.0-SNAPSHOT"

                        def services = (changedServices ?: '').split(',').findAll { it?.trim() }
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
                expression {
                    def services = (changedServices ?: '').split(',').findAll { it?.trim() }
                    services.any { !frontendServices.contains(it) }
                }
            }
            steps {
                script {
                    def services = (changedServices ?: '').split(',').findAll { it?.trim() }
                    def backendServices = services.findAll { !frontendServices.contains(it) }
                    sh 'chmod +x mvnw || true'
                    backendServices.each { svc ->
                        if (fileExists("${svc}/pom.xml")) {
                            echo "Running Maven tests for: ${svc}"
                            sh "./mvnw verify jacoco:report -DskipITs -pl ${svc} -am -U -Drevision=1.0-SNAPSHOT"
                        } else if (fileExists("${svc}/package.json")) {
                            echo "Running Node.js tests for: ${svc}"
                            dir("${svc}") {
                                sh "npm install && npm test || true"
                            }
                        }
                    }
                }
            }
            post {
                always {
                    script {
                        def services = (changedServices ?: '').split(',').findAll { it?.trim() }
                        def backendServices = services.findAll { !frontendServices.contains(it) }
                        backendServices.each { svc ->
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
                expression {
                    def services = (changedServices ?: '').split(',').findAll { it?.trim() }
                    services.any { !frontendServices.contains(it) }
                }
            }
            steps {
                script {
                    def services = (changedServices ?: '').split(',').findAll { it?.trim() }
                    def backendServices = services.findAll { !frontendServices.contains(it) }
                    backendServices.each { svc ->
                        if (!fileExists("${svc}/pom.xml")) {
                            echo "[${svc}] Skipping coverage check for non-Maven service."
                            return
                        }
                        def reportPath = "${svc}/target/site/jacoco/jacoco.csv"
                        def reportPresent = sh(script: "test -f ${reportPath} && echo yes || echo no", returnStdout: true).trim()
                        if (reportPresent != 'yes') {
                            echo "[${svc}] WARNING: jacoco.csv not found at ${reportPath}"
                            sh "find ${svc}/target -name '*.csv' -o -name 'jacoco*' 2>/dev/null || echo 'No jacoco files found'"
                            error("[${svc}] JaCoCo report missing — check jacoco-maven-plugin in ${svc}/pom.xml")
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

        stage('Build Phase') {
            when {
                expression {
                    def services = (changedServices ?: '').split(',').findAll { it?.trim() }
                    services.any { !frontendServices.contains(it) }
                }
            }
            steps {
                script {
                    def services = (changedServices ?: '').split(',').findAll { it?.trim() }
                    def backendServices = services.findAll { !frontendServices.contains(it) }
                    sh 'chmod +x mvnw || true'
                    backendServices.each { svc ->
                        if (fileExists("${svc}/pom.xml")) {
                            echo "Building: ${svc}"
                            sh "./mvnw clean package -DskipTests -pl ${svc} -am -U -Drevision=1.0-SNAPSHOT"
                        }
                    }
                }
            }
            post {
                success {
                    script {
                        def services = (changedServices ?: '').split(',').findAll { it?.trim() }
                        def backendServices = services.findAll { !frontendServices.contains(it) }
                        backendServices.each { svc ->
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
            echo "CI Pipeline PASSED – All stages completed successfully."
        }
        failure {
            echo "CI Pipeline FAILED – Check logs above for details."
        }
        always {
            cleanWs()
        }
    }
}
