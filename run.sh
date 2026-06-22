#!/bin/bash
set -x

ask_yes_no() {
    local prompt="$1"
    local default="${2:-N}"
    local answer
    
    if [ "$default" = "Y" ]; then
        read -r -p "$prompt [Y/n]: " answer
        answer=${answer:-Y}
    else
        read -r -p "$prompt [y/N]: " answer
        answer=${answer:-N}
    fi
    
    case "$answer" in
        y|Y|yes|Yes|YES) return 0 ;;
        n|N|no|No|NO) return 1 ;;
        *) return 1 ;;
    esac
}

ask_confirmation() {
    if ! ask_yes_no "Продолжить установку и запуск?"; then
        exit 0
    fi
}

check_java_version() {
    if command -v java &> /dev/null; then
        JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d '"' -f2 | sed 's/^1\.//' | cut -d'.' -f1)
        if [ -z "$JAVA_VERSION" ]; then
            JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d '"' -f2 | cut -d'.' -f1)
        fi
        echo "$JAVA_VERSION"
    else
        echo "0"
    fi
}

install_dependencies() {
    if [ -f /etc/os-release ]; then
        . /etc/os-release
        OS=$ID
    else
        return 1
    fi
    
    if ! command -v docker &> /dev/null; then
        case $OS in
            ubuntu|debian)
                sudo apt-get update
                sudo apt-get install -y apt-transport-https ca-certificates curl software-properties-common
                curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add -
                sudo add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable"
                sudo apt-get update
                sudo apt-get install -y docker-ce docker-ce-cli containerd.io
                sudo usermod -aG docker $USER
                ;;
            centos|rhel|fedora)
                sudo yum install -y yum-utils
                sudo yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
                sudo yum install -y docker-ce docker-ce-cli containerd.io
                sudo usermod -aG docker $USER
                ;;
            *)
                return 1
                ;;
        esac
    fi
    
    if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
        case $OS in
            ubuntu|debian|centos|rhel|fedora)
                sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
                sudo chmod +x /usr/local/bin/docker-compose
                sudo ln -s /usr/local/bin/docker-compose /usr/bin/docker-compose 2>/dev/null
                ;;
            *)
                return 1
                ;;
        esac
    fi
    
    JAVA_VERSION=$(check_java_version)
    
    if [ "$JAVA_VERSION" -lt 17 ]; then
        case $OS in
            ubuntu|debian)
                sudo apt-get update
                sudo apt-get install -y openjdk-17-jdk
                sudo update-alternatives --set java /usr/lib/jvm/java-17-openjdk-amd64/bin/java 2>/dev/null
                sudo update-alternatives --set javac /usr/lib/jvm/java-17-openjdk-amd64/bin/javac 2>/dev/null
                ;;
            centos|rhel|fedora)
                sudo yum install -y java-17-openjdk
                ;;
            *)
                return 1
                ;;
        esac
        
        NEW_VERSION=$(check_java_version)
        if [ "$NEW_VERSION" -lt 17 ]; then
            JAVA17_PATH=$(update-alternatives --list java 2>/dev/null | grep -i "java-17" | head -1)
            if [ -n "$JAVA17_PATH" ]; then
                sudo update-alternatives --set java "$JAVA17_PATH"
            fi
        fi
    elif [ "$JAVA_VERSION" -gt 17 ]; then
        if ask_yes_no "Используется Java $JAVA_VERSION. Установить Java 17?"; then
            case $OS in
                ubuntu|debian)
                    sudo apt-get update
                    sudo apt-get install -y openjdk-17-jdk
                    ;;
                centos|rhel|fedora)
                    sudo yum install -y java-17-openjdk
                    ;;
            esac
        fi
    fi
    
    if ! command -v mvn &> /dev/null; then
        case $OS in
            ubuntu|debian)
                sudo apt-get update
                sudo apt-get install -y maven
                ;;
            centos|rhel|fedora)
                sudo yum install -y maven
                ;;
        esac
    fi
    
    if ! command -v curl &> /dev/null; then
        case $OS in
            ubuntu|debian)
                sudo apt-get update
                sudo apt-get install -y curl
                ;;
            centos|rhel|fedora)
                sudo yum install -y curl
                ;;
        esac
    fi
}

check_dependencies() {
    local missing=0
    
    if ! command -v docker &> /dev/null; then
        missing=1
    fi
    
    if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
        missing=1
    fi
    
    if ! command -v java &> /dev/null; then
        missing=1
    else
        JAVA_VERSION=$(check_java_version)
        if [ "$JAVA_VERSION" -lt 17 ]; then
            missing=1
        fi
    fi
    
    if ! command -v curl &> /dev/null; then
        missing=1
    fi
    
    if [ $missing -eq 1 ]; then
        return 1
    fi
    
    return 0
}

run_maven() {
    local dir=$1
    
    cd "$dir"
    
    if [ -f "./mvnw" ]; then
        ./mvnw clean package -DskipTests
    elif [ -f "../mvnw" ]; then
        ../mvnw clean package -DskipTests
    elif command -v mvn &> /dev/null; then
        mvn clean package -DskipTests
    else
        cd - > /dev/null
        return 1
    fi
    
    local result=$?
    cd - > /dev/null
    return $result
}

ask_confirmation

if ! check_dependencies; then
    install_dependencies
    if ! ask_yes_no "Продолжить сборку проекта?"; then
        exit 0
    fi
fi

if ! command -v docker &> /dev/null; then
    exit 1
fi

if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
    exit 1
fi

JAVA_VERSION=$(check_java_version)
if [ "$JAVA_VERSION" -lt 17 ]; then
    exit 1
fi

build_services() {
    if ! run_maven "api-gateway"; then
        exit 1
    fi
    
    if ! run_maven "orders-service"; then
        exit 1
    fi
    
    if ! run_maven "payments-service"; then
        exit 1
    fi
}

create_dockerfiles() {
    cat > api-gateway/Dockerfile << 'EOF'
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
EOF

    cat > orders-service/Dockerfile << 'EOF'
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
EOF

    cat > payments-service/Dockerfile << 'EOF'
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
EOF
}

start_with_compose() {
    docker-compose down 2>/dev/null
    docker-compose up -d
    
    if [ $? -ne 0 ]; then
        exit 1
    fi
}

check_status() {
    sleep 5
    docker-compose ps
    docker-compose logs --tail=5
    
    curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health | grep -q "200"
    curl -s -o /dev/null -w "%{http_code}" http://localhost:8081/actuator/health | grep -q "200"
    curl -s -o /dev/null -w "%{http_code}" http://localhost:8082/actuator/health | grep -q "200"
}

main() {
    case "${1}" in
        install)
            install_dependencies
            ;;
        check)
            check_dependencies
            ;;
        build)
            build_services
            create_dockerfiles
            ;;
        start)
            start_with_compose
            check_status
            ;;
        rebuild)
            build_services
            create_dockerfiles
            start_with_compose
            check_status
            ;;
        stop)
            docker-compose down
            ;;
        logs)
            docker-compose logs -f
            ;;
        status)
            check_status
            ;;
        test)
            docker-compose exec api-gateway curl -s http://localhost:8080/actuator/health
            ;;
        help|--help|-h)
            ;;
        *)
            if [ -z "${1}" ]; then
                build_services
                create_dockerfiles
                start_with_compose
                check_status
            else
                exit 1
            fi
            ;;
    esac
}

main "$@"