#!/bin/bash

# Configuración
export ASADMIN="/home/hugo/glassfish-4.1/bin/asadmin"
LOG_FILE="/home/hugo/glassfish-4.1/glassfish/domains/domain1/logs/server.log"

CYAN='\033[0;36m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${CYAN}--- Iniciando Servidor Glassfish ---${NC}"

$ASADMIN start-domain domain1

echo -e "\n--- Visualizando logs (Presiona Ctrl+C para detener el servidor) ---"

cleanup() {
    echo -e "\n${YELLOW}--- Deteniendo servidor Glassfish... ---${NC}"
    $ASADMIN stop-domain domain1
    echo -e "${CYAN}--- Servidor detenido ---${NC}"
    exit
}

trap cleanup SIGINT

tail -f $LOG_FILE | grep -E --line-buffered "PHumanidades|SEVERE|WARNING|stdout|stderr" | grep -vE "org.jboss.weld|org.glassfish.tyrus|javax.enterprise.resource"
