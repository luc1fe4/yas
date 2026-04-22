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

                    sh 'git fetch origin main:remotes/origin/main'

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
                    // exit-code=0 + mark UNSTABLE thay vì fail cứng
                    def leakCount = sh(
                        script: './gitleaks detect --source=. --report-format=json --report-path=gitleaks-report.json --exit-code=1 || echo "LEAKS_FOUND"',
                        returnStdout: true
                    ).trim()

                    if (leakCount.contains('LEAKS_FOUND')) {
                        currentBuild.result = 'UNSTABLE'
                        echo "⚠️ Gitleaks found leaks — build marked UNSTABLE, continuing pipeline"
                    }
                }
            }
            post {
                always {
                    archiveArtifacts artifacts: 'gitleaks-report.json', allowEmptyArchive: true
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
                    def REVISION = '1.0-SNAPSHOT'

                    // Bước 1: Install root pom với revision tường minh
                    sh "mvn install -N -U -DskipTests -Drevision=${REVISION}"
                    // Bước 2: Install common-library với revision tường minh
                    dir('common-library') {
                        sh "mvn install -U -DskipTests -Drevision=${REVISION}"
                    }

                    def services = env.CHANGED_SERVICES.split(',')
                    services.each { svc ->
                        echo "Running tests for: ${svc}"
                        dir("${svc}") {
                            sh "./mvnw test -U -Drevision=${REVISION}"
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
            steps {
                script {
                    def services = env.CHANGED_SERVICES.split(',')
                    def failed = []
                    services.each { svc ->
                        def reportPath = "${svc}/target/site/jacoco/jacoco.csv"
                        def coverage = sh(script: """...""", returnStdout: true).trim().toInteger()
                        echo "[${svc}] Line Coverage: ${coverage}%"
                        if (coverage < 70) {
                            failed.add("${svc}: ${coverage}%")
                        }
                    }
                    if (!failed.isEmpty()) {
                        // UNSTABLE thay vì error() để Build Phase vẫn chạy
                        currentBuild.result = 'UNSTABLE'
                        echo "⚠️ Coverage below threshold: ${failed.join(', ')}"
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
                    def REVISION = '1.0-SNAPSHOT'
                    def services = env.CHANGED_SERVICES.split(',')
                    services.each { svc ->
                        echo "Building: ${svc}"
                        dir("${svc}") {
                            sh "./mvnw clean package -DskipTests -Drevision=${REVISION}"
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