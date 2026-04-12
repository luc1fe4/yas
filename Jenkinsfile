pipeline {
    agent any
    stages {
        stage('Security Scan') {
            agent {
                docker { 
                    image 'zricethezav/gitleaks:v8.18.4'
                    args '--entrypoint=' 
                }
            }
            steps {
                echo "--- Đang quét Secret bằng GitLeaks ---"
                sh 'gitleaks detect --source=. --config=gitleaks.toml --report-path=gitleaks-report.json --exit-code=1'
            }
            post {
                always {
                    archiveArtifacts artifacts: 'gitleaks-report.json', fingerprint: true, allowEmptyArchive: true
                }
            }
        }
    }
}