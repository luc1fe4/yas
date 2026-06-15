# Sample Hosts File Configuration for YAS Microservices

This document provides a template for mapping the local developer domains to the Kubernetes Worker node IP (e.g., Minikube IP or Kubernetes Cluster Worker Node IP).

## How to use
1. Retrieve the IP of your Kubernetes cluster/worker node:
   - For Minikube: `minikube ip`
   - For generic K8s: Use `kubectl get nodes -o wide` to identify the internal or external IP of your worker node.
2. Open your operating system's hosts file in administrative/superuser mode:
   - **Windows**: `C:\Windows\System32\drivers\etc\hosts`
   - **Linux/macOS**: `/etc/hosts`
3. Append the configuration below, replacing `<Worker-IP>` with your resolved IP.

---

```hosts
# =====================================================================
# YAS Local Development Domains Mapping
# =====================================================================
# Replace <Worker-IP> with your cluster IP (e.g., 192.168.49.2)

# --- Shared Infrastructure / Admin Tools ---
<Worker-IP> pgoperator.yas.local.com
<Worker-IP> pgadmin.yas.local.com
<Worker-IP> akhq.yas.local.com
<Worker-IP> kibana.yas.local.com
<Worker-IP> identity.yas.local.com
<Worker-IP> grafana.yas.local.com
<Worker-IP> kiali.yas.local.com
<Worker-IP> argocd.yas.local.com

# --- Dev Environment Namespace ---
<Worker-IP> backoffice.dev.yas.local.com
<Worker-IP> storefront.dev.yas.local.com
<Worker-IP> identity.dev.yas.local.com

# --- Staging Environment Namespace ---
<Worker-IP> backoffice.staging.yas.local.com
<Worker-IP> storefront.staging.yas.local.com
<Worker-IP> identity.staging.yas.local.com

# --- Developer Sandbox Namespace ---
<Worker-IP> backoffice.sandbox.yas.local.com
<Worker-IP> storefront.sandbox.yas.local.com
<Worker-IP> identity.sandbox.yas.local.com
```
