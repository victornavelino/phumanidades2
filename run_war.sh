#!/bin/bash

# Configuración de variables de entorno
export JAVA_HOME="/usr/lib/jvm"
export ASADMIN="/home/hugo/glassfish-4.1/bin/asadmin"
export ANT="ant"
export PROJECT_WAR_DIR="PHumanidades-war"

# Colores para la salida
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${CYAN}--- Iniciando Proceso de Despliegue (Linux) ---${NC}"

# Verificar si Ant está instalado
if ! command -v $ANT &> /dev/null; then
    echo -e "${RED}Error: 'ant' no está instalado.${NC}"
    echo -e "Puedes instalarlo con: ${YELLOW}sudo apt update && sudo apt install ant${NC}"
    exit 1
fi

# Asegurarse de que el dominio esté corriendo
echo -e "${YELLOW}[1/4] Verificando servidor Glassfish...${NC}"
$ASADMIN start-domain domain1

# Compilar el modulo WAR
echo -e "${YELLOW}[2/4] Compilando modulo WAR...${NC}"
cd $PROJECT_WAR_DIR
$ANT dist
if [ $? -ne 0 ]; then
    echo -e "${RED}Error en la compilación con Ant${NC}"
    exit 1
fi
cd ..

# Desplegar en Glassfish
echo -e "${YELLOW}[3/4] Desplegando archivo WAR...${NC}"
WAR_FILE="$PROJECT_WAR_DIR/dist/PHumanidades-war.war"
$ASADMIN deploy --force=true "$WAR_FILE"
if [ $? -ne 0 ]; then
    echo -e "${RED}Error en el despliegue${NC}"
    exit 1
fi

echo -e "${GREEN}[4/4] ¡Éxito!${NC}"
echo -e "${CYAN}URL de la aplicación: http://localhost:8080/PHumanidades-war${NC}"

# Mostrar Logs al finalizar
echo -e "\n--- Iniciando visualización de logs (Presiona Ctrl+C para detener y apagar el servidor) ---"
LOG_FILE="/home/hugo/glassfish-4.1/glassfish/domains/domain1/logs/server.log"

# Función para apagar el servidor al salir
cleanup() {
    echo -e "\n${YELLOW}--- Deteniendo servidor Glassfish... ---${NC}"
    $ASADMIN stop-domain domain1
    echo -e "${CYAN}--- Proceso finalizado ---${NC}"
    exit
}

trap cleanup SIGINT

# Seguimos el log filtrando ruido innecesario
tail -f $LOG_FILE | grep -E --line-buffered "PHumanidades|SEVERE|WARNING|stdout|stderr" | grep -vE "org.jboss.weld|org.glassfish.tyrus|javax.enterprise.resource"
