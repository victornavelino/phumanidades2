$env:JAVA_HOME = "C:\Program Files\Java\jdk1.8.0_202"
$env:AS_JAVA = $env:JAVA_HOME
$env:PATH = "$env:JAVA_HOME\bin;" + $env:PATH
$ANT = "C:\Program Files\NetBeans 8.0.2\extide\ant\bin\ant.bat"
$PROJECT_WAR_DIR = "PHumanidades-war"

# Cargar variables de entorno desde .env
if (Test-Path .env) {
    Write-Host "Cargando variables desde .env..." -ForegroundColor Gray
    Get-Content .env | Where-Object { $_ -match '=' -and $_ -notmatch '^#' } | ForEach-Object {
        $name, $value = $_.Split('=', 2)
        [System.Environment]::SetEnvironmentVariable($name.Trim(), $value.Trim())
    }
} else {
    Write-Warning "Archivo .env no encontrado. Asegurate de crearlo basandote en .env.example"
    exit 1
}

$REMOTE_USER = [System.Environment]::GetEnvironmentVariable('REMOTE_USER')
$REMOTE_HOST = [System.Environment]::GetEnvironmentVariable('REMOTE_HOST')
$REMOTE_PATH = [System.Environment]::GetEnvironmentVariable('REMOTE_PATH')
$REMOTE_PORT = [System.Environment]::GetEnvironmentVariable('REMOTE_PORT')

if (-not $REMOTE_PORT) {
    $REMOTE_PORT = "22"
}

if (-not $REMOTE_USER -or -not $REMOTE_HOST -or -not $REMOTE_PATH) {
    Write-Error "Faltan variables de entorno. Verifica tu archivo .env"
    exit 1
}

Write-Host "--- Iniciando Proceso de Despliegue Remoto ---" -ForegroundColor Cyan

# Compilar el modulo WAR
Write-Host "[1/2] Compilando modulo WAR..." -ForegroundColor Yellow
Set-Location $PROJECT_WAR_DIR
& $ANT dist
if ($LASTEXITCODE -ne 0) { Write-Error "Error en la compilacion con Ant"; exit $LASTEXITCODE }
Set-Location ..

# Desplegar en servidor remoto directamente
Write-Host "[2/2] Enviando archivo WAR al servidor remoto directamente..." -ForegroundColor Yellow
$WAR_FILE = "$PROJECT_WAR_DIR\dist\PHumanidades-war.war"
$WAR_FILENAME = Split-Path $WAR_FILE -Leaf

Write-Host "Conectando a ${REMOTE_USER}@${REMOTE_HOST}:${REMOTE_PORT}..." -ForegroundColor Gray
scp -P $REMOTE_PORT $WAR_FILE "${REMOTE_USER}@${REMOTE_HOST}:${REMOTE_PATH}/$WAR_FILENAME"

if ($LASTEXITCODE -ne 0) { 
    Write-Error "Error al copiar el archivo directamente. Asegúrate de haber otorgado los permisos con chmod 777 en el servidor."
    exit $LASTEXITCODE 
}

Write-Host ""
Write-Host "¡Éxito! Archivo WAR copiado directamente a la carpeta de autodeploy de Glassfish." -ForegroundColor Green
Write-Host "Glassfish en el servidor remoto debería detectarlo y redesplegarlo automáticamente." -ForegroundColor Cyan



