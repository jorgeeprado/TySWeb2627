#!/bin/bash
echo "=== DETENIENDO TODOS LOS SERVICIOS ==="
for port in 8080 8081 8082 4200; do
    pid=$(lsof -ti:$port)
    if [ -n "$pid" ]; then
        echo "Deteniendo proceso en puerto $port (PID: $pid)..."
        kill -9 $pid 2>/dev/null
    else
        echo "Puerto $port ya está libre."
    fi
done
echo "=== TODOS LOS SERVICIOS DETENIDOS ==="

