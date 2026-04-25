def changedServices = 'none'

pipeline {
    agent any

    tools {
       nodejs 'nodejs'   // Đảm bảo Jenkins có tool này cấu hình sẵn
    }

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
                    changedServices = selectedServices
                    env.CHANGED_SERVICES = selectedServices
                    writeFile file: '.changed_services', text: selectedServices

                    if (selectedServices == 'none') {
                        echo "No service changes detected. Skipping build/test."
                    } else {
                        echo "Services to build/test: ${selectedServices}"
                    }
                }
            }
        }

        stage('Security Scan') {
            steps {
                echo "--- Đang tải và thực thi GitLeaks ---"
                script {
                    // Tải và cài đặt GitLeaks binary trực tiếp trên Jenkins workspace
                    sh '''
                        curl -sSL https://github.com/gitleaks/gitleaks/releases/download/v8.18.4/gitleaks_8.18.4_linux_x64.tar.gz -o gitleaks.tar.gz
                        tar -xzf gitleaks.tar.gz
                        chmod +x gitleaks
                    '''
                    
                    // Thực thi quét secret, bỏ qua lỗi exit code nếu cần thiết lập Quality Gate riêng
                    sh './gitleaks detect --source=. --report-format=json --report-path=gitleaks-report.json --exit-code=0'
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
            steps {
                script {
                    def rawServices = (changedServices ?: '').trim()
                    if ((!rawServices || rawServices == 'none') && fileExists('.changed_services')) {
                        rawServices = readFile('.changed_services').trim()
                    }

                    echo "CHANGED_SERVICES in Test Phase: ${rawServices}"
                    def services = (rawServices && rawServices != 'none') ? rawServices.split(',').collect { it.trim() }.findAll { it } : []
                    if (services.isEmpty()) {
                        echo 'No changed services. Skip Test Phase.'
                    } else {
                        def nodeServices = services.findAll { fileExists("${it}/package.json") }
                        if (!nodeServices.isEmpty()) {
                            def npmExists = (sh(script: 'command -v npm >/dev/null 2>&1', returnStatus: true) == 0)
                            if (!npmExists) {
                                echo "npm is not available on Jenkins agent. Skipping frontend test for: ${nodeServices.join(', ')}"
                                services = services.findAll { svc -> !nodeServices.contains(svc) }
                            }
                        }

                        if (services.isEmpty()) {
                            echo 'No runnable services left in Test Phase after environment checks.'
                        } else {
                            sh 'chmod +x mvnw || true'
                            services.each { svc ->
                                echo "Running tests for: ${svc}"
                                if (fileExists("${svc}/mvnw")) {
                                    dir("${svc}") {
                                        sh 'chmod +x mvnw || true'
                                        sh './mvnw -B -ntp test jacoco:report'
                                    }
                                } else if (fileExists("${svc}/gradlew")) {
                                    dir("${svc}") {
                                        sh 'chmod +x gradlew || true'
                                        sh './gradlew test jacocoTestReport'
                                    }
                                } else if (fileExists("${svc}/package.json")) {
                                    dir("${svc}") {
                                        sh 'npm ci --no-audit --no-fund'
                                        sh 'npm test -- --runInBand'
                                    }
                                } else {
                                    echo "Skipping tests for ${svc}: no Maven/Gradle wrapper found."
                                }
                            }
                        }
                    }
                }
            }
            post {
                always {
                    script {
                        def rawServices = (changedServices ?: '').trim()
                        if ((!rawServices || rawServices == 'none') && fileExists('.changed_services')) {
                            rawServices = readFile('.changed_services').trim()
                        }

                        def services = (rawServices && rawServices != 'none') ? rawServices.split(',').collect { it.trim() }.findAll { it } : []
                        services.each { svc ->
                            def junitPatterns = []
                            if (fileExists("${svc}/target/surefire-reports")) {
                                junitPatterns << "${svc}/target/surefire-reports/*.xml"
                            }
                            if (fileExists("${svc}/build/test-results/test")) {
                                junitPatterns << "${svc}/build/test-results/test/*.xml"
                            }

                            if (!junitPatterns.isEmpty()) {
                                junit(
                                    testResults: junitPatterns.join(','),
                                    allowEmptyResults: true
                                )
                            } else {
                                echo "Skipping JUnit publish for ${svc}: no XML test reports found."
                            }

                            if (fileExists("${svc}/target/jacoco.exec")) {
                                jacoco(
                                    execPattern:   "${svc}/target/jacoco.exec",
                                    classPattern:  "${svc}/target/classes",
                                    sourcePattern: "${svc}/src/main/java",
                                    exclusionPattern: '**/*Test*.class'
                                )
                            } else {
                                echo "Skipping Jacoco for ${svc}: no Java coverage exec file found."
                            }
                        }
                    }
                }
            }
        }
        
        stage('Coverage Quality Gate') {
            steps {
                script {
                    def rawServices = (changedServices ?: '').trim()
                    if ((!rawServices || rawServices == 'none') && fileExists('.changed_services')) {
                        rawServices = readFile('.changed_services').trim()
                    }

                    echo "CHANGED_SERVICES in Coverage Quality Gate: ${rawServices}"
                    def services = (rawServices && rawServices != 'none') ? rawServices.split(',').collect { it.trim() }.findAll { it } : []
                    if (services.isEmpty()) {
                        echo 'No changed services. Skip Coverage Quality Gate.'
                    } else {
                        services.each { svc ->
                            def reportPath = "${svc}/target/site/jacoco/jacoco.csv"

                            if (fileExists(reportPath)) {
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
                            } else {
                                echo "Skipping coverage gate for ${svc}: jacoco.csv not found."
                            }
                        }
                    }
                }
            }
        }

        stage('Build Phase') {
            steps {
                script {
                    def rawServices = (changedServices ?: '').trim()
                    if ((!rawServices || rawServices == 'none') && fileExists('.changed_services')) {
                        rawServices = readFile('.changed_services').trim()
                    }

                    echo "CHANGED_SERVICES in Build Phase: ${rawServices}"
                    def services = (rawServices && rawServices != 'none') ? rawServices.split(',').collect { it.trim() }.findAll { it } : []
                    if (services.isEmpty()) {
                        echo 'No changed services. Skip Build Phase.'
                    } else {
                        def nodeServices = services.findAll { fileExists("${it}/package.json") }
                        if (!nodeServices.isEmpty()) {
                            def npmExists = (sh(script: 'command -v npm >/dev/null 2>&1', returnStatus: true) == 0)
                            if (!npmExists) {
                                echo "npm is not available on Jenkins agent. Skipping frontend build for: ${nodeServices.join(', ')}"
                                services = services.findAll { svc -> !nodeServices.contains(svc) }
                            }
                        }

                        if (services.isEmpty()) {
                            echo 'No runnable services left in Build Phase after environment checks.'
                        } else {
                            sh 'chmod +x mvnw || true'
                            services.each { svc ->
                                echo "Building: ${svc}"

                                if (fileExists("${svc}/mvnw")) {
                                    dir("${svc}") {
                                        sh 'chmod +x mvnw || true'
                                        sh './mvnw -B -ntp clean package -DskipTests'
                                    }
                                } else if (fileExists("${svc}/gradlew")) {
                                    dir("${svc}") {
                                        sh 'chmod +x gradlew || true'
                                        sh './gradlew clean build -x test'
                                    }
                                } else if (fileExists("${svc}/package.json")) {
                                    dir("${svc}") {
                                        sh 'npm ci --no-audit --no-fund'
                                        sh 'npm run build'
                                    }
                                } else {
                                    error("Cannot build ${svc}: no Maven/Gradle wrapper found")
                                }
                            }
                        }
                    }
                }
            }
            post {
                success {
                    script {
                        def rawServices = (changedServices ?: '').trim()
                        if ((!rawServices || rawServices == 'none') && fileExists('.changed_services')) {
                            rawServices = readFile('.changed_services').trim()
                        }

                        def services = (rawServices && rawServices != 'none') ? rawServices.split(',').collect { it.trim() }.findAll { it } : []
                        services.each { svc ->
                            archiveArtifacts artifacts: "${svc}/target/*.jar,${svc}/target/*.war,${svc}/build/libs/*.jar,${svc}/build/libs/*.war",
                                             allowEmptyArchive: true,
                                             fingerprint: true
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