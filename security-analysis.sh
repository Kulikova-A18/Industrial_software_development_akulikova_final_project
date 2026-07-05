#!/bin/bash
set -x

install_go() {
    if ! command -v go &> /dev/null; then
        curl -LO https://go.dev/dl/go1.22.5.linux-amd64.tar.gz
        sudo tar -C /usr/local -xzf go1.22.5.linux-amd64.tar.gz
        export PATH=$PATH:/usr/local/go/bin
        rm go1.22.5.linux-amd64.tar.gz
    fi
}

check_cmd() {
    if ! command -v "$1" &> /dev/null; then
        case "$1" in
            semgrep) sudo snap install semgrep ;;
            gitleaks) install_go && go install github.com/zricethezav/gitleaks/v8@latest && export PATH=$PATH:$(go env GOPATH)/bin ;;
            jq) sudo apt-get install -y jq ;;
        esac
    fi
}

check_cmd semgrep
check_cmd gitleaks
check_cmd jq

mkdir -p security-reports

# Gitleaks
gitleaks detect --source . --report-format json --report-path security-reports/gitleaks.json

# Semgrep (rules from https://github.com/AuroraProudmoore/java-audit-skill/tree/main)
semgrep scan --json --output security-reports/semgrep.json --include '*.java' \
  --config ./rules/semgrep/java-api-security.yaml \
  --config ./rules/semgrep/java-config.yaml \
  --config ./rules/semgrep/java-crypto.yaml \
  --config ./rules/semgrep/java-emerging.yaml \
  --config ./rules/semgrep/java-file.yaml \
  --config ./rules/semgrep/java-microservice.yaml \
  --config ./rules/semgrep/java-misc.yaml \
  --config ./rules/semgrep/java-rce.yaml \
  --config ./rules/semgrep/java-sqli.yaml \
  --config ./rules/semgrep/java-ssrf.yaml \
  --no-git-ignore
