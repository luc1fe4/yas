stage('Security Scan') {
    agent {
        docker { 
            image 'zricethezav/gitleaks:latest'
            args '--entrypoint='
        }
    }
    steps {
        echo "--- Đang quét Secret bằng GitLeaks ---"
        // Sử dụng file gitleaks.toml có sẵn trong repo của bạn
        sh 'gitleaks detect --source=. --config=gitleaks.toml --report-path=gitleaks-report.json --exit-code=0'
    }
    post {
        always {
            archiveArtifacts artifacts: 'gitleaks-report.json', fingerprint: true
        }
    }
}