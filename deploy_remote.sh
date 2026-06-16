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

# Obtener el directorio absoluto del script
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Usar la librería CopyLibs local del proyecto
COPY_LIBS_PATH="$SCRIPT_DIR/Librerias/org-netbeans-modules-java-j2seproject-copylibstask.jar"

export GLASSFISH_HOME="/home/hugo/glassfish-4.1"

# Definir argumentos para Ant (incluyendo propiedades vacías para librerías globales de NetBeans para evitar fallos de copia)
ANT_ARGS="-Dlibs.CopyLibs.classpath=$COPY_LIBS_PATH -Dj2ee.server.home=$GLASSFISH_HOME/glassfish -Dlibs.jsf20.classpath= -Dlibs.javaee-web-api-7.0.classpath= -Dlibs.spring-webmvc4.0.classpath= -Dlibs.spring-framework400.classpath="

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
echo -e "${YELLOW}[2/2] Enviando archivo WAR al servidor remoto...${NC}"
WAR_FILE="$PROJECT_WAR_DIR/dist/PHumanidades-war.war"
WAR_FILENAME=$(basename "$WAR_FILE")

# Verificar que el archivo compilado exista antes de intentar enviarlo
if [ ! -f "$WAR_FILE" ]; then
    echo -e "${RED}Error: No se encontró el archivo compilado en '$WAR_FILE'${NC}"
    exit 1
fi

echo -e "${GRAY}Estableciendo conexión maestra con ${REMOTE_USER}@${REMOTE_HOST}:${REMOTE_PORT}...${NC}"
SSH_SOCKET="/tmp/ssh_mux_${REMOTE_HOST}_${REMOTE_PORT}_${REMOTE_USER}"

# Función de limpieza para cerrar la conexión compartida al terminar
cleanup() {
    if [ -S "$SSH_SOCKET" ]; then
        echo -e "${GRAY}Cerrando conexión SSH maestra...${NC}"
        ssh -O exit -S "$SSH_SOCKET" -p "$REMOTE_PORT" "${REMOTE_USER}@${REMOTE_HOST}" 2>/dev/null
    fi
}
trap cleanup EXIT

# Iniciar la conexión maestra (esta pedirá la contraseña del usuario huma una sola vez)
ssh -M -S "$SSH_SOCKET" -fnN -p "$REMOTE_PORT" "${REMOTE_USER}@${REMOTE_HOST}"
if [ $? -ne 0 ]; then
    echo -e "${RED}Error al establecer la conexión SSH maestra.${NC}"
    exit 1
fi

echo -e "${GREEN}Conexión SSH establecida con éxito.${NC}"

# 1. Copiar a /tmp del servidor remoto usando la conexión compartida (sin pedir contraseña)
echo -e "${GRAY}Copiando archivo WAR a /tmp en el servidor remoto...${NC}"
scp -o ControlPath="$SSH_SOCKET" -P "$REMOTE_PORT" "$WAR_FILE" "${REMOTE_USER}@${REMOTE_HOST}:/tmp/$WAR_FILENAME"

if [ $? -ne 0 ]; then
    echo -e "${RED}Error al copiar el archivo a /tmp en el servidor remoto.${NC}"
    exit 1
fi

# 2. Mover a la ruta de autodeploy elevando a root con su -c usando la conexión compartida
# (Esta pedirá la contraseña de root directamente y de forma clara)
echo -e "${YELLOW}Elevando privilegios con su -c para mover el archivo a la carpeta autodeploy...${NC}"
echo -e "${CYAN}Por favor, ingresa la contraseña de ROOT cuando se te solicite a continuación:${NC}"
ssh -t -o ControlPath="$SSH_SOCKET" -p "$REMOTE_PORT" "${REMOTE_USER}@${REMOTE_HOST}" "su -c 'mv /tmp/$WAR_FILENAME $REMOTE_PATH/$WAR_FILENAME && chmod 777 $REMOTE_PATH/$WAR_FILENAME'"

if [ $? -ne 0 ]; then
    echo -e "${RED}Error al mover el archivo a la carpeta autodeploy con su -c.${NC}"
    exit 1
fi

echo -e ""
echo -e "${GREEN}¡Éxito! Archivo WAR copiado directamente a la carpeta de autodeploy de Glassfish.${NC}"
echo -e "${CYAN}Glassfish en el servidor remoto debería detectarlo y redesplegarlo automáticamente.${NC}"
