$env:JAVA_HOME = "C:\Program Files\Java\jdk1.8.0_202"
$env:AS_JAVA = $env:JAVA_HOME
$env:PATH = "$env:JAVA_HOME\bin;" + $env:PATH
$ASADMIN = "C:\Users\victo\glassfish-4.1\glassfish\bin\asadmin.bat"
$ANT = "C:\Program Files\NetBeans 8.0.2\extide\ant\bin\ant.bat"
$PROJECT_WAR_DIR = "PHumanidades-war"

Write-Host "--- Iniciando Proceso de Despliegue ---" -ForegroundColor Cyan

# Asegurarse de que el dominio esté corriendo
Write-Host "[1/4] Verificando servidor Glassfish..." -ForegroundColor Yellow
& $ASADMIN start-domain domain1

# Compilar el modulo WAR
Write-Host "[2/4] Compilando modulo WAR..." -ForegroundColor Yellow
Set-Location $PROJECT_WAR_DIR
& $ANT dist
if ($LASTEXITCODE -ne 0) { Write-Error "Error en la compilación con Ant"; exit $LASTEXITCODE }
Set-Location ..

# Desplegar en Glassfish
Write-Host "[3/4] Desplegando archivo WAR..." -ForegroundColor Yellow
$WAR_FILE = "$PROJECT_WAR_DIR/dist/PHumanidades-war.war"
& $ASADMIN deploy --force=true "$WAR_FILE"
if ($LASTEXITCODE -ne 0) { Write-Error "Error en el despliegue"; exit $LASTEXITCODE }

Write-Host "[4/4] ¡Éxito!" -ForegroundColor Green
Write-Host "URL de la aplicación: http://localhost:8080/PHumanidades-war" -ForegroundColor Cyan

# Mostrar Logs al finalizar y manejar el cierre
Write-Host "`n--- Iniciando visualización de logs (Presiona Ctrl+C para detener y apagar el servidor) ---" -ForegroundColor Gray
$LOG_FILE = "C:\Users\victo\glassfish-4.1\glassfish\domains\domain1\logs\server.log"

try {
    # Filtramos para mostrar: mensajes de la app, errores, advertencias y salidas de consola (stdout)
    # Ignoramos el "ruido" de inyección de dependencias (weld) y otros módulos internos
    Get-Content $LOG_FILE -Wait -Tail 20 | Where-Object { 
        ($_ -match "PHumanidades|SEVERE|WARNING|stdout|stderr") -or 
        ($_ -notmatch "org.jboss.weld|org.glassfish.tyrus|javax.enterprise.resource")
    }
}
finally {
    Write-Host "`n--- Deteniendo servidor Glassfish... ---" -ForegroundColor Yellow
    & $ASADMIN stop-domain domain1
    Write-Host "--- Proceso finalizado ---" -ForegroundColor Cyan
}
