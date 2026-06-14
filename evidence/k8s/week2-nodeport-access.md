# Evidence: Week 2 Kubernetes Deployment and NodePort Access

- **Evidence ID**: EVID-06 / EVID-08 / EVID-09
- **Status**: `UNVERIFIED_RUNTIME`

---

## 1. What Evidence to Capture
* Command output of the deployed pods and deployments in the `dev` namespace to verify they are using the correct image tag.
* Command output showing the service configured as NodePort.
* Output of the local hosts file mapping.
* Response headers or output from hitting the endpoint.

---

## 2. Exact UI Path or Command
Run the following terminal commands:
```bash
# 1. Inspect the image tag for the deployed cart service
kubectl get deploy -n dev cart -o yaml | grep image:

# 2. Check all deployments in the namespace to confirm non-selected services use default tags
kubectl get deploy -n dev -o jsonpath='{range .items[*]}{.metadata.name}{"\t"}{.spec.template.spec.containers[*].image}{"\n"}{end}'

# 3. Retrieve NodePorts assigned to YAS services
kubectl get svc -n dev

# 4. Read the hosts file to verify routing configuration
# Windows:
powershell Get-Content C:\Windows\System32\drivers\etc\hosts
# Linux/macOS:
cat /etc/hosts

# 5. Access the endpoint
curl -v http://yas-dev.local:<NODE_PORT>
```

---

## 3. Expected Result
* `cart` deployment should show image: `<namespace>/cart:13f4c2a2`.
* Other deployments should show `main` or `latest`.
* NodePort mapping exists (e.g., `80:31080/TCP`).
* Hosts file contains `<WORKER_NODE_IP> yas-dev.local`.
* Curl returns HTTP 200 OK or appropriate routing redirect.

---

## 4. Actual Result Placeholder
```text
[INSERT DEPLOYED IMAGE CHECK OUTPUT HERE]
[INSERT GET SVC OUTPUT HERE]
[INSERT HOSTS FILE CONTENT HERE]
[INSERT CURL VERBOSE LOGS HERE]
```

---

## 5. Screenshot Placeholder
*(Insert screenshot of browser loading the page via yas-dev.local:<NODE_PORT>)*
```text
[IMAGE PLACEHOLDER: evidence/k8s/svc-nodeport.txt]
```
```text
[IMAGE PLACEHOLDER: evidence/repository/hosts-mapping.txt]
```
