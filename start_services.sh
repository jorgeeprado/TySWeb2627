#!/bin/bash
PROJECT_DIR="/home/jorge/Escritorio/PROYECTO TySW/TySWeb2627"

echo "=== DETENIENDO SERVICIOS ANTERIORES ==="
for port in 8080 8081 8082 4200; do
    pid=$(lsof -ti:$port 2>/dev/null)
    if [ -n "$pid" ]; then
        echo "Cerrando proceso en puerto $port (PID: $pid)..."
        kill -9 $pid 2>/dev/null
    fi
done

sleep 2

echo "=== VERIFICANDO MYSQL (3306) ==="
if ! ss -tulpn | grep -q ":3306 "; then
    echo "Intentando iniciar MySQL..."
    service mysql start 2>/dev/null || sudo service mysql start 2>/dev/null
fi

echo "=== ARRANCANDO SERVIDORES ==="

# 1. beusu (puerto 8080)
echo "Iniciando beusu (8080)..."
(cd "$PROJECT_DIR/beusu" && nohup ./mvnw spring-boot:run > "$PROJECT_DIR/beusu.log" 2>&1 & disown)

# 2. bealquileres (puerto 8081)
echo "Iniciando bealquileres (8081)..."
(cd "$PROJECT_DIR/bealquileres" && nohup ./mvnw spring-boot:run > "$PROJECT_DIR/bealquileres.log" 2>&1 & disown)

# 3. simulador (puerto 8082)
echo "Iniciando simulador (8082)..."
(cd "$PROJECT_DIR/simulador" && nohup ./mvnw spring-boot:run > "$PROJECT_DIR/simulador.log" 2>&1 & disown)

# 4. feayto2 (puerto 4200)
echo "Iniciando feayto2 (4200)..."
(cd "$PROJECT_DIR/feayto2" && nohup env CI=true HOME=/tmp NG_CLI_ANALYTICS=false ./node_modules/.bin/ng serve --host 0.0.0.0 --port 4200 > "$PROJECT_DIR/feayto2.log" 2>&1 & disown)

echo "=== TODOS LOS SERVICIOS LANZADOS EN SEGUNDO PLANO ==="
