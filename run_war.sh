#!/bin/bash

# Configuración de variables de entorno
export ASADMIN="/home/hugo/glassfish-4.1/bin/asadmin"
export ANT="ant"
export PROJECT_WAR_DIR="PHumanidades-war"

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
    echo -e "\033[0;31mError: No se encontró una instalación válida de Java 8 (JDK 1.8).\033[0m"
    echo -e "Glassfish 4.1 requiere Java 8 para funcionar y compilar correctamente."
    echo -e "Por favor, instale Java 8 o configure correctamente la variable de entorno JAVA_HOME."
    exit 1
fi

# Exportar el binario de Java 8 al PATH
export PATH="$JAVA_HOME/bin:$PATH"

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

# Buscar la librería CopyLibs de NetBeans y definir argumentos para Ant
export GLASSFISH_HOME="/home/hugo/glassfish-4.1"
NB_PROPERTIES=$(ls /home/hugo/.netbeans/*/build.properties 2>/dev/null | sort -r | head -n 1)
if [ -n "$NB_PROPERTIES" ]; then
    COPY_LIBS_PATH=$(grep "libs.CopyLibs.classpath" "$NB_PROPERTIES" | cut -d'=' -f2)
fi
if [ ! -f "$COPY_LIBS_PATH" ]; then
    COPY_LIBS_PATH="/usr/local/netbeans-8.0.2/java/ant/extra/org-netbeans-modules-java-j2seproject-copylibstask.jar"
fi
ANT_ARGS="-Dlibs.CopyLibs.classpath=$COPY_LIBS_PATH -Dj2ee.server.home=$GLASSFISH_HOME/glassfish"
if [ -n "$NB_PROPERTIES" ]; then
    ANT_ARGS="-propertyfile $NB_PROPERTIES $ANT_ARGS"
fi

# Compilar el modulo WAR
echo -e "${YELLOW}[2/4] Compilando modulo WAR...${NC}"
cd $PROJECT_WAR_DIR
$ANT $ANT_ARGS dist
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

# Seguimos el log directamente
tail -f $LOG_FILE
