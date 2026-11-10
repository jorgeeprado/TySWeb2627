#!/bin/bash
echo "=== ESTADO DE PUERTOS Y SERVICIOS ==="
for port in 3306 8080 8081 8082 4200; do
    if ss -tulpn | grep -q ":$port "; then
        pid=$(ss -tulpn | grep ":$port " | grep -o 'pid=[0-9]*' | head -n 1 | cut -d= -f2)
        echo "✅ Puerto $port ACTIVO ${pid:+(PID: $pid)}"
    else
        echo "❌ Puerto $port INACTIVO"
    fi
done

