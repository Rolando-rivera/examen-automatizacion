#!/usr/bin/env bash
set -euo pipefail

modo=${1:-}
artefacto=${2:-}
ejecucion=${3:-local}
puerto=${PORT:-18080}

if [[ "$modo" != "acceptance" && "$modo" != "deploy" ]] || [[ ! -f "$artefacto" ]]; then
    echo "Uso: bash scripts/despliegue.sh {acceptance|deploy} ruta/al.jar [id-ejecucion]" >&2
    exit 2
fi

directorio=$(mktemp -d)
url="http://127.0.0.1:$puerto"
pid=""
salida="$directorio/servidor.log"

detener() {
    if [[ -n "$pid" ]]; then
        kill "$pid" 2>/dev/null || true
        wait "$pid" 2>/dev/null || true
        pid=""
    fi
}

limpiar() {
    detener
    rm -rf -- "$directorio"
}

trap limpiar EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

arrancar_y_validar() {
    local archivo=$1 version=$2 estado
    java -jar "$archivo" "$puerto" "$version" > "$salida" 2>&1 &
    pid=$!
    for (( intento=1; intento<=40; intento++ )); do
        if ! kill -0 "$pid" 2>/dev/null; then
            break
        fi
        if estado=$(curl --silent --max-time 1 --output "$directorio/respuesta" \
            --write-out '%{http_code}' "$url/salud") &&
           [[ "$estado" == "200" && "$(cat "$directorio/respuesta")" == "$version" ]]; then
            echo "[SALUD] HTTP 200 | versión activa = $version"
            return 0
        fi
        sleep 0.25
    done
    echo "[SALUD] La versión $version no respondió correctamente." >&2
    cat "$salida" >&2
    return 1
}

aceptacion() {
    local estado
    estado=$(curl --silent --show-error --max-time 3 --output "$directorio/respuesta" \
        --write-out '%{http_code}' "$url/prioridad/critico") || return 1
    [[ "$estado" == "200" && "$(cat "$directorio/respuesta")" == "alta" ]] || return 1
    echo "[ACEPTACIÓN] Impacto crítico: HTTP 200 | prioridad alta"

    estado=$(curl --silent --show-error --max-time 3 --output "$directorio/respuesta" \
        --write-out '%{http_code}' "$url/prioridad/otro") || return 1
    [[ "$estado" == "400" && "$(cat "$directorio/respuesta")" == "impacto inválido" ]] || return 1
    echo "[ACEPTACIÓN] Impacto inválido: HTTP 400 | mensaje esperado"
}

desplegar() {
    local nueva_ruta=$1 nueva_version=$2 ruta_anterior=$3 version_anterior=$4
    echo "[DEPLOY] Activando $nueva_version"
    detener
    if arrancar_y_validar "$nueva_ruta" "$nueva_version"; then
        echo "[DEPLOY] Versión $nueva_version verificada"
        return 0
    fi

    echo "[ROLLBACK] Falló $nueva_version. Restaurando $version_anterior"
    detener
    if arrancar_y_validar "$ruta_anterior" "$version_anterior" && aceptacion; then
        echo "[ROLLBACK] Versión $version_anterior restaurada y verificada"
        return 10
    fi
    echo "[ROLLBACK] No se pudo verificar la recuperación" >&2
    return 11
}

if [[ "$modo" == "acceptance" ]]; then
    echo "[ACEPTACIÓN] Validando el JAR antes del despliegue"
    arrancar_y_validar "$artefacto" "candidata-$ejecucion"
    aceptacion
    exit 0
fi

# Copias aisladas del JAR para el ejercicio. Las versiones sanas difieren
# en su identificador de despliegue. La tercera copia simula un JAR dañado.
cp -- "$artefacto" "$directorio/base.jar"
cp -- "$artefacto" "$directorio/candidata.jar"
echo "Contenido de prueba: JAR deliberadamente inválido." > "$directorio/fallida.jar"

echo "[AMBIENTE DE PRUEBA] $url"
echo "[DEPLOY] Levantando la versión de referencia"
arrancar_y_validar "$directorio/base.jar" "base-$ejecucion"
desplegar "$directorio/candidata.jar" "candidata-$ejecucion" "$directorio/base.jar" "base-$ejecucion"
aceptacion

echo "[DEMOSTRACIÓN] Intentando desplegar una copia de prueba dañada"
resultado=0
desplegar "$directorio/fallida.jar" "fallida-$ejecucion" \
    "$directorio/candidata.jar" "candidata-$ejecucion" || resultado=$?

if [[ "$resultado" != "10" ]]; then
    echo "[RESULTADO] Se esperaba un fallo con recuperación verificada (10). Resultado: $resultado" >&2
    exit 1
fi

echo "[RESULTADO] Despliegue y rollback verificados en el ambiente de prueba"
