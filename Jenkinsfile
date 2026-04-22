pipeline {
    agent any

    tools {
        jdk   'jdk21'
        maven 'Maven-3.9'
    }

    // ✅ Bỏ environment block đi, khởi tạo trong script
    stages {

        stage('Detect Changed Services') {
            steps {
                script {
                    // ✅ Khởi tạo giá trị mặc định ngay từ đầu
                    env.CHANGED_SERVICES = ''

                    sh 'git fetch origin main'

                    def changedFiles = sh(
                        script: "git diff --name-only origin/main...HEAD",
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
                        changedFiles.contains("${svc}/")
                    }

                    if (affected.isEmpty()) {
                        echo "No service changes detected."
                        env.CHANGED_SERVICES = 'none'
                    } else {
                        env.CHANGED_SERVICES = affected.join(',')
                        echo "Services to build/test: ${env.CHANGED_SERVICES}"
                    }
                }
            }
        }

        stage('Security Scan') {
            steps {
                script {
                    sh '''
                        curl -sSL https://github.com/gitleaks/gitleaks/releases/download/v8.18.4/gitleaks_8.18.4_linux_x64.tar.gz -o gitleaks.tar.gz
                        tar -xzf gitleaks.tar.gz
                        chmod +x gitleaks
                    '''
                    sh './gitleaks detect --source=. --report-format=json --report-path=gitleaks-report.json --exit-code=0 || true'
                }
            }
            post {
                always {
                    archiveArtifacts artifacts: 'gitleaks-report.json',
                                     allowEmptyArchive: true
                }
            }
        }

        stage('Test Phase') {
            when {
                // ✅ Dùng env. nhất quán
                expression { env.CHANGED_SERVICES != 'none' && env.CHANGED_SERVICES != '' }
            }
            steps {
                script {
                    // Install common-library vào local Maven repo trước
                    dir('common-library') {
                        sh './mvnw install -DskipTests'
                    }

                    def services = env.CHANGED_SERVICES.split(',')
                    services.each { svc ->
                        echo "Running tests for: ${svc}"
                        dir("${svc}") {
                            sh './mvnw verify'
                        }
                    }
                }
            }
            post {
                always {
                    script {
                        if (env.CHANGED_SERVICES && env.CHANGED_SERVICES != 'none') {
                            def services = env.CHANGED_SERVICES.split(',')
                            services.each { svc ->
                                junit testResults: "${svc}/target/surefire-reports/*.xml",
                                      allowEmptyResults: true
                                jacoco(
                                    execPattern:      "${svc}/target/jacoco.exec",
                                    classPattern:     "${svc}/target/classes",
                                    sourcePattern:    "${svc}/src/main/java",
                                    exclusionPattern: '**/*Test*.class'
                                )
                            }
                        }
                    }
                }
            }
        }

        stage('Coverage Quality Gate') {
            when {
                expression { env.CHANGED_SERVICES != 'none' && env.CHANGED_SERVICES != '' }
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
                expression { env.CHANGED_SERVICES != 'none' && env.CHANGED_SERVICES != '' }
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

    post {
        success { echo "CI Pipeline PASSED." }
        failure { echo "CI Pipeline FAILED." }
        always  { cleanWs() }
    }
}