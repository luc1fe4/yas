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

                    def affected = services.findAll { svc ->
                        changedFiles.contains("${svc}/")
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
            sh './gitleaks detect --source=. --report-format=json --report-path=gitleaks-report.json --exit-code=1'
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
                expression { (env.CHANGED_SERVICES ?: 'none') != 'none' }
            }
            steps {
                script {
                    def services = env.CHANGED_SERVICES.split(',')
                    services.each { svc ->
                        echo "Running tests for: ${svc}"
                        dir("${svc}") {
                            sh './mvnw test jacoco:report'
                        }
                    }
                }
            }
            post {
                always {
                    script {
                        def services = env.CHANGED_SERVICES.split(',')
                        services.each { svc ->
                            junit(
                                testResults: "${svc}/target/surefire-reports/*.xml",
                                allowEmptyResults: false
                            )
                            jacoco(
                                execPattern:   "${svc}/target/jacoco.exec",
                                classPattern:  "${svc}/target/classes",
                                sourcePattern: "${svc}/src/main/java",
                                exclusionPattern: '**/*Test*.class'
                            )
                        }
                    }
                }
            }
        }
        stage('Coverage Quality Gate') {
            when {
                expression { (env.CHANGED_SERVICES ?: 'none') != 'none' }
            }
            steps {
                script {
                    def services = env.CHANGED_SERVICES.split(',')
                    services.each { svc ->
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

                        if (coverage < 70) {
                            error("[${svc}] Coverage ${coverage}% < 70%. Pipeline failed!")
                        }
                    }
                }
            }
        }

        stage('Build Phase') {
            when {
                expression { (env.CHANGED_SERVICES ?: 'none') != 'none' }
            }
            steps {
                script {
                    def services = env.CHANGED_SERVICES.split(',')
                    services.each { svc ->
                        echo "Building: ${svc}"
                        dir("${svc}") {
                            sh './mvnw clean package -DskipTests'
                        }
                    }
                }
            }
            post {
                success {
                    script {
                        def services = env.CHANGED_SERVICES.split(',')
                        services.each { svc ->
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
