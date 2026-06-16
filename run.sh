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

# Obtener el directorio absoluto del script
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Usar la librería CopyLibs local del proyecto
COPY_LIBS_PATH="$SCRIPT_DIR/Librerias/org-netbeans-modules-java-j2seproject-copylibstask.jar"

# Definir argumentos base para Ant (incluyendo propiedades vacías para librerías globales de NetBeans para evitar fallos de copia)
ANT_ARGS="-Dlibs.CopyLibs.classpath=$COPY_LIBS_PATH -Dj2ee.server.home=$GLASSFISH_HOME/glassfish -Dlibs.jsf20.classpath= -Dlibs.javaee-web-api-7.0.classpath= -Dlibs.spring-webmvc4.0.classpath= -Dlibs.spring-framework400.classpath="

# Colores para la salida
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Auto-detectar Java 8 (JDK 1.8) que es requerido por Glassfish 4.1 y Ant
detect_java8() {
    # 1. Probar la variable JAVA_HOME actual si ya está configurada y es Java 8
    if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ] && "$JAVA_HOME/bin/java" -version 2>&1 | grep -q "1.8"; then
        return 0
    fi
    
    # 2. Rutas comunes en Debian/Ubuntu para JDK 1.8 (incluyendo jdk1.8.0_* del usuario)
    for path in "/usr/lib/jvm/jdk1.8.0_202" /usr/lib/jvm/jdk1.8.0_* "/usr/lib/jvm/java-8-openjdk-amd64" "/usr/lib/jvm/java-1.8.0-openjdk-amd64"; do
        if [ -d "$path" ] && [ -x "$path/bin/java" ] && "$path/bin/java" -version 2>&1 | grep -q "1.8"; then
            export JAVA_HOME="$path"
            return 0
        fi
    done

    # 3. Detectar a través del comando 'java' del sistema si apunta a una versión 1.8
    local sys_java=$(readlink -f $(which java 2>/dev/null) 2>/dev/null)
    if [ -n "$sys_java" ] && "$sys_java" -version 2>&1 | grep -q "1.8"; then
        # Si apunta a .../jre/bin/java, el JDK está en el directorio superior de la jre
        local possible_home=$(dirname $(dirname "$sys_java"))
        if [[ "$possible_home" == */jre ]]; then
            possible_home=$(dirname "$possible_home")
        fi
        if [ -x "$possible_home/bin/java" ]; then
            export JAVA_HOME="$possible_home"
            return 0
        fi
    fi

    return 1
}

if ! detect_java8; then
    echo -e "${RED}Error: No se encontró una instalación válida de Java 8 (JDK 1.8).${NC}"
    echo -e "Glassfish 4.1 requiere Java 8 para funcionar y compilar correctamente."
    echo -e "Por favor, instale Java 8 o configure la variable de entorno JAVA_HOME."
    echo -e "Puedes instalarlo con: ${YELLOW}sudo apt install openjdk-8-jdk${NC}"
    exit 1
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
        # Desplegar limpiando primero para evitar el bug de "Keys cannot be duplicate" de Glassfish 4.1
        $ASADMIN undeploy PHumanidades-war >/dev/null 2>&1
        $ASADMIN deploy "$WAR_FILE"
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
