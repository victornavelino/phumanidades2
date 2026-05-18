#!/bin/bash

# Configuración de variables de entorno locales
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
GRAY='\033[0;90m'

# Cargar variables de entorno desde .env de forma robusta
if [ -f .env ]; then
    echo -e "${GRAY}Cargando variables desde .env...${NC}"
    while IFS= read -r line || [ -n "$line" ]; do
        # Ignorar comentarios y líneas vacías
        if [[ ! "$line" =~ ^# ]] && [[ ! -z "$line" ]]; then
            # Limpiar retornos de carro de Windows (\r) y exportar
            clean_line=$(echo "$line" | tr -d '\r')
            export "$clean_line"
        fi
    done < .env
else
    echo -e "${RED}Error: Archivo .env no encontrado. Asegúrate de crearlo basándote en .env.example${NC}"
    exit 1
fi

# Configurar puerto por defecto
if [ -z "$REMOTE_PORT" ]; then
    REMOTE_PORT="22"
fi

# Validar variables obligatorias
if [ -z "$REMOTE_USER" ] || [ -z "$REMOTE_HOST" ] || [ -z "$REMOTE_PATH" ]; then
    echo -e "${RED}Error: Faltan variables de entorno obligatorias (REMOTE_USER, REMOTE_HOST, REMOTE_PATH) en el archivo .env${NC}"
    exit 1
fi

echo -e "${CYAN}--- Iniciando Proceso de Despliegue Remoto (Debian) ---${NC}"

# Verificar si Ant está instalado
if ! command -v $ANT &> /dev/null; then
    echo -e "${RED}Error: '$ANT' no está instalado.${NC}"
    echo -e "Puedes instalarlo con: ${YELLOW}sudo apt update && sudo apt install ant${NC}"
    exit 1
fi

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

# Compilar el módulo WAR
echo -e "${YELLOW}[1/2] Compilando módulo WAR...${NC}"
cd $PROJECT_WAR_DIR
$ANT $ANT_ARGS dist
if [ $? -ne 0 ]; then
    echo -e "${RED}Error en la compilación con Ant${NC}"
    exit 1
fi
cd ..

# Desplegar en servidor remoto
echo -e "${YELLOW}[2/2] Enviando archivo WAR al servidor remoto directamente...${NC}"
WAR_FILE="$PROJECT_WAR_DIR/dist/PHumanidades-war.war"
WAR_FILENAME=$(basename "$WAR_FILE")

# Verificar que el archivo compilado exista antes de intentar enviarlo
if [ ! -f "$WAR_FILE" ]; then
    echo -e "${RED}Error: No se encontró el archivo compilado en '$WAR_FILE'${NC}"
    exit 1
fi

echo -e "${GRAY}Conectando a ${REMOTE_USER}@${REMOTE_HOST}:${REMOTE_PORT}...${NC}"
scp -P "$REMOTE_PORT" "$WAR_FILE" "${REMOTE_USER}@${REMOTE_HOST}:${REMOTE_PATH}/$WAR_FILENAME"

if [ $? -ne 0 ]; then
    echo -e "${RED}Error al copiar el archivo directamente. Asegúrate de haber otorgado los permisos con chmod 777 en el servidor remoto.${NC}"
    exit 1
fi

echo -e ""
echo -e "${GREEN}¡Éxito! Archivo WAR copiado directamente a la carpeta de autodeploy de Glassfish.${NC}"
echo -e "${CYAN}Glassfish en el servidor remoto debería detectarlo y redesplegarlo automáticamente.${NC}"
