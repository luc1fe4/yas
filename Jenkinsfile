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
                    // Lấy danh sách service, loại bỏ khoảng trắng dư thừa
                    def services = env.CHANGED_SERVICES.split(',').collect { it.trim() }.findAll { it != "" }
                    def failed = []

                    services.each { svc ->
                        def reportPath = "${svc}/target/site/jacoco/jacoco.csv"
                        
                        if (fileExists(reportPath)) {
                            // Đọc file CSV bằng hàm có sẵn của Jenkins (không dùng lệnh sh)
                            def csvContent = readFile(reportPath)
                            def lines = csvContent.split('\n')
                            
                            long lineMissed = 0
                            long lineCovered = 0
                            
                            // Duyệt từng dòng để cộng dồn LINE_MISSED (cột 7) và LINE_COVERED (cột 8)
                            lines.eachWithIndex { line, idx ->
                                if (idx > 0 && line.trim()) { // Bỏ qua dòng header
                                    def cols = line.split(',')
                                    if (cols.size() > 8) {
                                        lineMissed += cols[7].toLong()
                                        lineCovered += cols[8].toLong()
                                    }
                                }
                            }
                            
                            // Công thức tính phần trăm:
                            // Coverage = (Covered / (Missed + Covered)) * 100
                            int coverage = (lineCovered + lineMissed > 0) ? 
                                        (int) (lineCovered * 100 / (lineCovered + lineMissed)) : 0
                            
                            echo "[${svc}] Total Line Coverage: ${coverage}%"
                            
                            if (coverage < 70) {
                                failed.add("${svc} (${coverage}%)")
                            }
                        } else {
                            echo "⚠️ Không tìm thấy báo cáo JaCoCo cho service: ${svc} tại ${reportPath}"
                        }
                    }

                    if (!failed.isEmpty()) {
                        // Đánh dấu UNSTABLE để Build Phase vẫn được thực hiện
                        currentBuild.result = 'UNSTABLE'
                        echo "❌ Cảnh báo: Coverage dưới ngưỡng 70% tại các service: ${failed.join(', ')}"
                    } else {
                        echo "✅ Tuyệt vời! Tất cả các service đều vượt ngưỡng Coverage."
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