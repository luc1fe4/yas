def frontendServices = ['backoffice', 'storefront']

pipeline {
    agent any


    environment {
        CHANGED_SERVICES = 'none'
    }

    stages {
        stage('Detect Changed Services') {
            steps {
                script {
                    def targetBranch = env.CHANGE_TARGET ?: 'main'
                    sh "git fetch --no-tags origin +refs/heads/${targetBranch}:refs/remotes/origin/${targetBranch}"

                    def changedFiles = sh(
                        script: "git diff --name-only refs/remotes/origin/${targetBranch}...HEAD",
                        returnStdout: true
                    ).trim()

                    echo "Changed files:\n${changedFiles}"

                    def services = [
                        // Business Services (Backend - Java/Spring)
                        'cart', 'customer', 'delivery', 'inventory', 'location', 
                        'media', 'order', 'payment', 'payment-paypal', 'product', 
                        'promotion', 'rating', 'recommendation', 'search', 'tax',
                        
                        // BFF & Gateways
                        'backoffice-bff', 'storefront-bff', 'identity',
                        
                        // Frontend (Next.js)
                        'backoffice', 'storefront'
                    ]

                    def detectedByScript = ''
                    def affected = [] as Set

                    // Detect trực tiếp từ danh sách file thay đổi theo prefix thư mục service
                    def changedList = changedFiles ? changedFiles.split('\n') : []
                    changedList.each { filePath ->
                        def matched = services.find { svc -> filePath == svc || filePath.startsWith("${svc}/") }
                        if (matched) {
                            affected << matched
                        }
                    }

                    // Kết hợp script detect changed services của team (Nguyen Quoc Loc)
                    if (fileExists('scripts/detect-changed-services.sh')) {
                        sh 'chmod +x scripts/detect-changed-services.sh || true'
                        detectedByScript = sh(
                            script: 'bash scripts/detect-changed-services.sh',
                            returnStdout: true
                        ).trim()
                        echo "Detect script output: ${detectedByScript}"

                        if (detectedByScript && detectedByScript != 'none') {
                            detectedByScript.split(',').collect { it.trim() }.findAll { it }.each { svc ->
                                if (services.contains(svc)) {
                                    affected << svc
                                }
                            }
                        }
                    }

                    def selectedServices = affected ? affected.join(',') : 'none'
                    env.CHANGED_SERVICES = selectedServices

                    if (selectedServices == 'none') {
                        echo "No service changes detected. Skipping build/test."
                    } else {
                        echo "Services to build/test: ${selectedServices}"
                    }
                }
            }
        }

        stage('Frontend Build Start Test') {
            when {
                expression {
                    def services = (env.CHANGED_SERVICES ?: '').split(',').findAll { it?.trim() }
                    services.any { frontendServices.contains(it) }
                }
            }

            // Jenkins Plugin sẽ lo việc nạp NodeJS vào môi trường
            tools {
                nodejs 'nodejs'
            }

            steps {
                script {
                    def services = (env.CHANGED_SERVICES ?: '').split(',').findAll { it?.trim() }
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
            post {
                always {
                    archiveArtifacts artifacts: '*-start.log', allowEmptyArchive: true
                }
            }
        }

       stage('Security Scan') {
            steps {
                echo "--- Đang tải và thực thi GitLeaks ---"
                script {
                    def diffBaseBranch = 'main'

                    // Tải và cài đặt GitLeaks binary trực tiếp trên Jenkins workspace
                    sh '''
                        curl -sSL https://github.com/gitleaks/gitleaks/releases/download/v8.18.4/gitleaks_8.18.4_linux_x64.tar.gz -o gitleaks.tar.gz
                        tar -xzf gitleaks.tar.gz
                        chmod +x gitleaks
                    '''
                    
                    // Chi quet commit tren nhanh hien tai so voi main de tranh fail vi leak cu trong lich su du an
                    sh "./gitleaks detect --source=. --config=gitleaks.toml --log-opts=\"origin/${diffBaseBranch}..HEAD\" --report-format=json --report-path=gitleaks-report.json --exit-code=1"
                }
            }
            post {
                always {
                    // Lưu trữ file báo cáo quét bảo mật sau mỗi lần chạy
                    archiveArtifacts artifacts: 'gitleaks-report.json', fingerprint: true, allowEmptyArchive: true
                }
            }
        }

        stage('Test Phase') {
            when {
                expression {
                    def services = (env.CHANGED_SERVICES ?: '').split(',').findAll { it?.trim() }
                    services.any { !frontendServices.contains(it) }
                }
            }
            steps {
                script {
                    def services = (env.CHANGED_SERVICES ?: '').split(',').findAll { it?.trim() }
                    def backendServices = services.findAll { !frontendServices.contains(it) }
                    sh 'chmod +x mvnw || true'
                    backendServices.each { svc ->
                        echo "Running tests for: ${svc}"
                        sh "./mvnw verify jacoco:report -DskipITs -pl ${svc} -am -U -Drevision=1.0-SNAPSHOT"
                    }
                }
            }
            post {
                always {
                    script {
                        def services = (env.CHANGED_SERVICES ?: '').split(',').findAll { it?.trim() }
                        def backendServices = services.findAll { !frontendServices.contains(it) }
                        backendServices.each { svc ->
                            // Publish JUnit test results
                            junit(
                                testResults: "${svc}/target/surefire-reports/*.xml",
                                allowEmptyResults: true
                            )
                            // Note: jacoco() DSL step removed - JaCoCo plugin not installed.
                            // Coverage is enforced via the 'Coverage Quality Gate' stage below.
                        }
                    }
                }
            }
        }
        stage('Coverage Quality Gate') {
            when {
                expression {
                    def services = (env.CHANGED_SERVICES ?: '').split(',').findAll { it?.trim() }
                    services.any { !frontendServices.contains(it) }
                }
            }
            steps {
                script {
                    def services = (env.CHANGED_SERVICES ?: '').split(',').findAll { it?.trim() }
                    def backendServices = services.findAll { !frontendServices.contains(it) }
                    backendServices.each { svc ->
                        def reportPath = "${svc}/target/site/jacoco/jacoco.csv"

                        def coverage = sh(script: """
                            awk -F',' 'NR>1 {
                                missed  += \$4;
                                covered += \$5
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
                    def services = (env.CHANGED_SERVICES ?: '').split(',').findAll { it?.trim() }
                    services.any { !frontendServices.contains(it) }
                }
            }
            steps {
                script {
                    def services = (env.CHANGED_SERVICES ?: '').split(',').findAll { it?.trim() }
                    def backendServices = services.findAll { !frontendServices.contains(it) }
                    sh 'chmod +x mvnw || true'
                    backendServices.each { svc ->
                        echo "Building: ${svc}"
                        sh "./mvnw clean package -DskipTests -pl ${svc} -am -U -Drevision=1.0-SNAPSHOT"
                    }
                }
            }
            post {
                success {
                    script {
                        def services = (env.CHANGED_SERVICES ?: '').split(',').findAll { it?.trim() }
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

    // ── POST toàn bộ pipeline ────────────────────────────────────────────
    post {
        success {
            echo "CI Pipeline PASSED – All stages completed successfully."
        }
        failure {
            echo "CI Pipeline FAILED – Check logs above for details."
        }
        always {
            // Dọn workspace sau mỗi lần chạy
            cleanWs()
        }
    }
}