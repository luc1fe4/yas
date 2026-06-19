# YAS Local Kubernetes Deployment and Configuration Guide

This guide describes how to configure, deploy, and verify the YAS microservices on a local Kubernetes cluster (Docker Desktop, Minikube, or WSL2) and access them using NodePorts and custom domain names.

---

## 1. Prerequisites and Setup

Before running the deployment, ensure the following are installed:
- **Docker Desktop** (or WSL2 Docker)
- **kubectl** (installed automatically with Docker Desktop or available at `C:\Program Files\Docker\Docker\resources\bin\kubectl.exe`)
- **Helm v3** (if deploying locally via Helm commands)

### A. Resource Requirements
Running the complete YAS stack (including databases, message brokers, observability tools, and 16+ Spring Boot apps) requires:
- **Memory**: Minimum 16 GB RAM allocated to your WSL/Minikube VM.
- **CPU**: Minimum 4 Cores.
- **Disk**: 40 GB free space.

*Note: Since default local WSL VMs might be allocated less RAM (e.g. 4GB), you must increase the memory limit in your `%USERPROFILE%\.wslconfig` file before booting up your cluster:*
```text
[wsl2]
memory=16GB
processors=4
```

---

## 2. Deploying Keycloak and Key Services

To start the infrastructure services (PostgreSQL, Kafka, Elasticsearch, Redis, Keycloak, etc.):
1. Navigate to the `k8s/deploy/` directory.
2. Initialize Keycloak (Identity and Access Management server):
   ```bash
   ./setup-keycloak.sh
   ```
3. Set up Redis:
   ```bash
   ./setup-redis.sh
   ```
4. Set up the remaining cluster services (Postgres, Elasticsearch, Kafka, Debezium):
   ```bash
   ./setup-cluster.sh
   ```

Verify all infrastructure servers are in the `Running` state:
```bash
kubectl get pods -n postgres
kubectl get pods -n elasticsearch
kubectl get pods -n kafka
kubectl get pods -n keycloak
```

---

## 3. Deploying YAS Applications

Once the base cluster services are healthy, run the application deployment script:
```bash
./deploy-yas-applications.sh dev
```
This script builds dependencies and deploys storefront, backoffice, and 16 backend microservices to the `dev` namespace.

---

## 4. NodePort Access Reference Table

We have configured the main entrypoints with custom static **NodePorts** between `30000` and `32767` for reliable local routing without requiring external load balancers or full ingress controllers.

| Service Name | Local Domain Mapping | Port Type | Target Port | NodePort | Access URL |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Storefront UI** | `storefront-ui.dev.yas.local.com` | HTTP | 3000 | **`30080`** | `http://storefront-ui.dev.yas.local.com:30080` |
| **Backoffice UI** | `backoffice-ui.dev.yas.local.com` | HTTP | 3000 | **`30081`** | `http://backoffice-ui.dev.yas.local.com:30081` |
| **Swagger UI** | `swagger-ui.dev.yas.local.com` | HTTP | 8080 | **`30082`** | `http://swagger-ui.dev.yas.local.com:30082/swagger-ui/` |
| **Keycloak (Identity)** | `identity.dev.yas.local.com` | HTTP | 8080 | **`30084`** | `http://identity.dev.yas.local.com:30084/auth/` |
| **Storefront BFF** | `storefront-bff.dev.yas.local.com` | HTTP | 80 | **`30085`** | `http://storefront-bff.dev.yas.local.com:30085` |
| **Backoffice BFF** | `backoffice-bff.dev.yas.local.com` | HTTP | 80 | **`30086`** | `http://backoffice-bff.dev.yas.local.com:30086` |

---

## 5. DNS / Hosts Resolution Mapping

Add the following mappings to your local operating system's `hosts` file to resolve these domain names:
- **Windows File Path**: `C:\Windows\System32\drivers\etc\hosts` (Open Notepad as Administrator to edit)
- **Linux/macOS File Path**: `/etc/hosts` (Edit using `sudo nano /etc/hosts`)

Replace `<WORKER_NODE_IP>` with your cluster IP (e.g. `127.0.0.1` for Docker Desktop, or the output of `minikube ip`):
```text
# --- YAS Local Kubernetes NodePort Domain Mappings ---
<WORKER_NODE_IP> storefront-ui.dev.yas.local.com
<WORKER_NODE_IP> backoffice-ui.dev.yas.local.com
<WORKER_NODE_IP> swagger-ui.dev.yas.local.com
<WORKER_NODE_IP> identity.dev.yas.local.com
<WORKER_NODE_IP> storefront-bff.dev.yas.local.com
<WORKER_NODE_IP> backoffice-bff.dev.yas.local.com
```

---

## 6. Verification and Troubleshooting Checklist

### A. Retrieve Service NodePorts
Check the actual services status and port mapping:
```bash
kubectl get svc -n dev
```
*Expected: Look for `storefront-ui`, `backoffice-ui`, and `swagger-ui` in the list, showing type `NodePort` with ports mapped to `30080`, `30081`, and `30082` respectively.*

### B. Verify Connectivity
Test DNS resolution and HTTP handshake via `curl`:
```bash
curl -v http://storefront-ui.dev.yas.local.com:30080
```
*Expected response: HTTP 200 OK or appropriate routing redirect to Keycloak.*

### C. Troubleshooting Keycloak Redirects
If you are redirected to the incorrect URL during sign-in:
1. Access the Keycloak admin panel at `http://identity.dev.yas.local.com:30084/auth/`
2. Log in using credentials from `keycloak-credentials` secret:
   ```bash
   kubectl get secret keycloak-credentials -n keycloak -o jsonpath="{.data.password}" | base64 --decode
   ```
3. Navigate to **Clients** and make sure the Valid Redirect URIs match your NodePort URLs.
