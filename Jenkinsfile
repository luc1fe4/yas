pipeline {
    agent any

    environment {
        CHANGED_SERVICES = ''
    }

    stages {

        stage('Detect Changed Services') {
            steps {
                script {
                    def changedFiles = sh(
                        script: "git diff --name-only origin/main...HEAD",
                        returnStdout: true
                    ).trim()

                    echo "Changed files:\n${changedFiles}"

                    def services = [
                        'media-service',
                        'product-service',
                        'cart-service',
                        'order-service',
                        'customer-service',
                        'rating-service',
                        'inventory-service',
                        'backoffice-bff',
                        'storefront-bff'
                    ]

                    def affected = services.findAll { svc ->
                        changedFiles.contains("${svc}/")
                    }

                    if (affected.isEmpty()) {
                        echo "No service changes detected. Skipping build/test."
                        CHANGED_SERVICES = 'none'
                    } else {
                        CHANGED_SERVICES = affected.join(',')
                        echo "Services to build/test: ${CHANGED_SERVICES}"
                    }
                }
            }
        }

        stage('Check Secrets (Gitleaks)') {
            steps {
                sh '''
                    gitleaks detect \
                        --source="." \
                        --report-format=json \
                        --report-path=gitleaks-report.json \
                        --exit-code=1 \
                        -v
                '''
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
                expression { CHANGED_SERVICES != 'none' && CHANGED_SERVICES != '' }
            }
            steps {
                script {
                    def services = CHANGED_SERVICES.split(',')
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
                        def services = CHANGED_SERVICES.split(',')
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
──
        stage('Coverage Quality Gate') {
            when {
                expression { CHANGED_SERVICES != 'none' && CHANGED_SERVICES != '' }
            }
            steps {
                script {
                    def services = CHANGED_SERVICES.split(',')
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
                expression { CHANGED_SERVICES != 'none' && CHANGED_SERVICES != '' }
            }
            steps {
                script {
                    def services = CHANGED_SERVICES.split(',')
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
                        def services = CHANGED_SERVICES.split(',')
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
