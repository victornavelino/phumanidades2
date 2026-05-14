#!/bin/bash

# =================================================================
# PHumanidades - Script Unificado de Gestión
# =================================================================

# Configuración
export GLASSFISH_HOME="/home/hugo/glassfish-4.1"
export ASADMIN="$GLASSFISH_HOME/bin/asadmin"
export JAVA_HOME="/usr/lib/jvm/java-8-openjdk-amd64"
export ANT="ant"
export PROJECT_WAR_DIR="PHumanidades-war"
LOG_FILE="$GLASSFISH_HOME/glassfish/domains/domain1/logs/server.log"

# Buscar la librería CopyLibs de NetBeans (necesaria para empaquetar el WAR)
# Intentamos obtenerla de la configuración del usuario
NB_PROPERTIES=$(ls /home/hugo/.netbeans/*/build.properties 2>/dev/null | sort -r | head -n 1)
if [ -n "$NB_PROPERTIES" ]; then
    COPY_LIBS_PATH=$(grep "libs.CopyLibs.classpath" "$NB_PROPERTIES" | cut -d'=' -f2)
fi

# Si no se encuentra, usar la ruta por defecto en este sistema
if [ ! -f "$COPY_LIBS_PATH" ]; then
    COPY_LIBS_PATH="/usr/local/netbeans-8.0.2/java/ant/extra/org-netbeans-modules-java-j2seproject-copylibstask.jar"
fi

# Definir argumentos base para Ant
ANT_ARGS="-Dlibs.CopyLibs.classpath=$COPY_LIBS_PATH -Dj2ee.server.home=$GLASSFISH_HOME/glassfish"
if [ -n "$NB_PROPERTIES" ]; then
    ANT_ARGS="-propertyfile $NB_PROPERTIES $ANT_ARGS"
fi

# Colores para la salida
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Verificar si Java 8 está instalado en la ruta especificada o buscar alternativas
check_java8() {
    if [ -d "$1" ] && [ -x "$1/bin/java" ]; then
        if "$1/bin/java" -version 2>&1 | grep -q "1.8"; then
            return 0
        fi
    fi
    return 1
}

if ! check_java8 "$JAVA_HOME"; then
    # Intentar buscar otras rutas comunes de Java 8
    JAVA_FOUND=false
    for path in "/usr/lib/jvm/java-8-openjdk-amd64" "/usr/lib/jvm/java-1.8.0-openjdk-amd64" "/usr/lib/jvm"; do
        if check_java8 "$path"; then
            export JAVA_HOME="$path"
            JAVA_FOUND=true
            break
        fi
    done

    if [ "$JAVA_FOUND" = false ]; then
        echo -e "${RED}Error: No se encontró Java 8.${NC}"
        echo -e "Ruta intentada por defecto: $JAVA_HOME"
        echo -e "Por favor, instálalo con: ${YELLOW}sudo apt install openjdk-8-jdk${NC}"
        exit 1
    fi
fi

# Exportar el binario de Java 8 al PATH para que asadmin lo use
export PATH="$JAVA_HOME/bin:$PATH"

# Función de limpieza al salir
cleanup() {
    echo -e "\n${YELLOW}--- Deteniendo servidor Glassfish... ---${NC}"
    $ASADMIN stop-domain domain1
    echo -e "${CYAN}--- Proceso finalizado ---${NC}"
    exit
}

# Capturar Ctrl+C
trap cleanup SIGINT

# Función para mostrar los logs
show_logs() {
    echo -e "\n${CYAN}--- Visualizando logs (Presiona Ctrl+C para detener y apagar el servidor) ---${NC}"
    if [ ! -f "$LOG_FILE" ]; then
        echo -e "${YELLOW}Esperando a que se cree el archivo de log...${NC}"
        sleep 2
    fi
    tail -f "$LOG_FILE" | grep -E --line-buffered "PHumanidades|SEVERE|WARNING|stdout|stderr" | grep -vE "org.jboss.weld|org.glassfish.tyrus|javax.enterprise.resource"
}

# Función para iniciar el servidor
start_server() {
    echo -e "${CYAN}--- Iniciando Servidor Glassfish ---${NC}"
    $ASADMIN start-domain domain1
}

# Función para compilar y desplegar el WAR
deploy_war() {
    # Verificar si Ant está instalado
    if ! command -v $ANT &> /dev/null; then
        echo -e "${RED}Error: 'ant' no está instalado.${NC}"
        echo -e "Puedes instalarlo con: ${YELLOW}sudo apt update && sudo apt install ant${NC}"
        return 1
    fi

    echo -e "${YELLOW}[1/3] Verificando servidor Glassfish...${NC}"
    $ASADMIN start-domain domain1

    echo -e "${YELLOW}[2/3] Compilando modulo WAR...${NC}"
    if [ -d "$PROJECT_WAR_DIR" ]; then
        cd "$PROJECT_WAR_DIR"
        # Usamos los argumentos detectados (CopyLibs, propiedades de NetBeans, etc.)
        $ANT $ANT_ARGS dist
        if [ $? -ne 0 ]; then
            echo -e "${RED}Error en la compilación con Ant${NC}"
            cd ..
            return 1
        fi
        cd ..
    else
        echo -e "${RED}Error: No se encuentra el directorio $PROJECT_WAR_DIR${NC}"
        return 1
    fi

    echo -e "${YELLOW}[3/3] Desplegando archivo WAR...${NC}"
    WAR_FILE="$PROJECT_WAR_DIR/dist/PHumanidades-war.war"
    if [ -f "$WAR_FILE" ]; then
        $ASADMIN deploy --force=true "$WAR_FILE"
        if [ $? -ne 0 ]; then
            echo -e "${RED}Error en el despliegue${NC}"
            return 1
        fi
    else
        echo -e "${RED}Error: No se encuentra el archivo WAR en $WAR_FILE${NC}"
        return 1
    fi

    echo -e "${GREEN}¡Éxito! Aplicación desplegada.${NC}"
    echo -e "${CYAN}URL: http://localhost:8080/PHumanidades-war${NC}"
}

# Lógica principal
echo -e "${CYAN}==============================================${NC}"
echo -e "${CYAN}       PHumanidades - Control Panel           ${NC}"
echo -e "${CYAN}==============================================${NC}"

if [ -z "$1" ]; then
    echo -e "Seleccione una opción:"
    echo -e "1) ${GREEN}Iniciar Servidor${NC} (Solo logs)"
    echo -e "2) ${YELLOW}Compilar y Desplegar WAR${NC}"
    echo -e "3) Salir"
    read -p "Opción [1-3]: " opt
else
    case "$1" in
        server) opt=1 ;;
        war) opt=2 ;;
        *) opt=0 ;;
    esac
fi

case $opt in
    1)
        start_server
        show_logs
        ;;
    2)
        deploy_war
        if [ $? -eq 0 ]; then
            show_logs
        fi
        ;;
    3)
        echo "Saliendo..."
        exit 0
        ;;
    *)
        echo -e "${RED}Opción no válida.${NC}"
        echo "Uso: $0 [server|war]"
        exit 1
        ;;
esac
