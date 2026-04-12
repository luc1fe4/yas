pipeline {
    agent any

    stages {
        stage('Security Scan') {
            agent {
                docker { 
                    // Pin phiên bản cụ thể theo góp ý của Copilot
                    image 'zricethezav/gitleaks:v8.18.4'
                    args '--entrypoint=' 
                }
            }
            steps {
                echo "--- Đang quét Secret bằng GitLeaks ---"
                // Để exit-code=1 để Jenkins báo Đỏ nếu phát hiện lộ Secret
                sh 'gitleaks detect --source=. --config=gitleaks.toml --report-path=gitleaks-report.json --exit-code=1'
            }
            post {
                always {
                    // Thêm allowEmptyArchive để tránh lỗi nếu không có file report
                    archiveArtifacts artifacts: 'gitleaks-report.json', fingerprint: true, allowEmptyArchive: true
                }
            }
        }
    }
}