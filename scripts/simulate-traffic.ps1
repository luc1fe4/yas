# f:\Devops\yas\scripts\simulate-traffic.ps1
param(
    [int]$DurationSeconds = 120,
    [string]$Namespace = "dev"
)

# 1. Kiểm tra kết nối tới K8s Cluster
$clusterInfo = kubectl cluster-info 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "Loi: Khong the ket noi toi Kubernetes cluster! Hay chac chan rang Docker Desktop va k3d cluster dang chay." -ForegroundColor Red
    Write-Host ($clusterInfo -join "`n") -ForegroundColor Red
    exit 1
}

# 2. Kiểm tra xem namespace có tồn tại và có pod nào đang chạy không
$pods = kubectl get pods -n $Namespace --no-headers 2>$null
if (-not $pods) {
    Write-Warning "Canh bao: Khong tim thay bat ky Pod nao dang chay trong namespace '$Namespace'!"
    Write-Warning "Hay chac chan rang cac ung dung da duoc deploy va dang o trang thai Running."
}

Write-Host ">>> Starting FULL HTTP port-80 traffic simulation using main application containers for ${DurationSeconds} seconds..." -ForegroundColor Cyan

function Exec-Wget($sourceApp, $destUrl) {
    # Find the pod name
    $pod = kubectl get pod -l "app.kubernetes.io/name=$sourceApp" -n $Namespace --no-headers 2>$null | 
        Select-Object -First 1 | ForEach-Object { ($_ -split '\s+')[0] }
    
    # Fallback to app label
    if (-not $pod) {
        $pod = kubectl get pod -l "app=$sourceApp" -n $Namespace --no-headers 2>$null | 
            Select-Object -First 1 | ForEach-Object { ($_ -split '\s+')[0] }
    }

    if ($pod) {
        # Determine the main container name (which matches the service name)
        $containerName = $sourceApp
        if ($sourceApp -eq "dev-swagger-ui") {
            $containerName = "swagger-ui"
        }
        
        Write-Host "[$(Get-Date -Format 'HH:mm:ss')] Sending: $sourceApp -> $destUrl" -ForegroundColor Gray
        # Fire and forget using Start-Process, running wget inside the main application container so iptables intercepts it!
        Start-Process -FilePath "kubectl" -ArgumentList "exec $pod -n $Namespace -c $containerName -- wget -qO- --timeout=2 $destUrl" -NoNewWindow
    } else {
        Write-Warning "Pod cho service '$sourceApp' khong tim thay! Bo qua request toi $destUrl"
    }
}

$startTime = Get-Date
while (((Get-Date) - $startTime).TotalSeconds -lt $DurationSeconds) {
    Write-Host "[$(Get-Date -Format 'HH:mm:ss')] --- Starting new traffic batch request ---" -ForegroundColor Green
    # 1. UIs calling BFFs/Services (Port 80 to force HTTP telemetry)
    Exec-Wget "storefront-ui" "http://storefront-bff/"
    Exec-Wget "backoffice-ui" "http://backoffice-bff/"
    Exec-Wget "swagger-ui" "http://product/"
    Exec-Wget "swagger-ui" "http://order/"

    # 2. BFFs calling Backend Services
    Exec-Wget "storefront-bff" "http://customer/"
    Exec-Wget "storefront-bff" "http://cart/"
    Exec-Wget "storefront-bff" "http://order/"
    Exec-Wget "storefront-bff" "http://product/"
    
    Exec-Wget "backoffice-bff" "http://product/"
    Exec-Wget "backoffice-bff" "http://inventory/"
    Exec-Wget "backoffice-bff" "http://tax/"
    Exec-Wget "backoffice-bff" "http://media/"

    # 3. Inter-service backend calls
    Exec-Wget "order" "http://product/"
    Exec-Wget "order" "http://cart/"
    Exec-Wget "order" "http://customer/"
    Exec-Wget "order" "http://tax/"
    Exec-Wget "order" "http://inventory/"

    Exec-Wget "cart" "http://product/"
    Exec-Wget "cart" "http://media/"

    Exec-Wget "inventory" "http://product/"

    Exec-Wget "search" "http://product/"

    Exec-Wget "customer" "http://product/"
    
    Exec-Wget "tax" "http://product/"

    Exec-Wget "media" "http://product/"

    # 4. Sampledata calling product to seed data
    Exec-Wget "sampledata" "http://product/"
    Exec-Wget "sampledata" "http://media/"

    Start-Sleep -Seconds 2
}

Write-Host ">>> Traffic simulation complete!" -ForegroundColor Green
