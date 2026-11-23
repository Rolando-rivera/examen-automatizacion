# Automatización de pruebas para una API de tickets

La API asigna una prioridad según el impacto de un ticket. Usa Java 17, Maven, JUnit 5 y GitHub Actions. El caso de estudio permite observar desde la validación de la regla de negocio hasta la recuperación de la aplicación en un ambiente de prueba.

| Solicitud GET | Resultado |
| --- | --- |
| `/prioridad/critico` | HTTP 200, `alta` |
| `/prioridad/moderado` | HTTP 200, `media` |
| `/prioridad/bajo` | HTTP 200, `baja` |
| `/prioridad/otro` | HTTP 400, `impacto inválido` |
| `/salud` | HTTP 200, identificador de la versión activa |

## Flujo Git Trunk-Based

- `main` es la rama principal. El trabajo se realiza en ramas cortas, por ejemplo `feat/documentar-flujo`.
- Cada cambio se sube y se abre un Pull Request hacia `main`. Se revisan build, pruebas unitarias, integración y aceptación antes de fusionar.
- Tras integrar el cambio, el push a `main` ejecuta el pipeline completo, incluido el despliegue. La rama de trabajo se elimina al terminar.

## Ejecutar las pruebas

Requisitos para la ejecución local: JDK 17, Maven 3.9 o compatible, Bash y curl. En Windows, los scripts se pueden ejecutar desde Git Bash con Java y Maven disponibles en el PATH.

```bash
mvn -B clean test
mvn -B clean verify
bash scripts/despliegue.sh acceptance target/prioridad-tickets.jar local
bash scripts/despliegue.sh deploy target/prioridad-tickets.jar local
```

`test` ejecuta los 4 casos unitarios de `PrioridadTest` con Surefire. `verify` repite las pruebas unitarias, genera el JAR y valida los 3 casos de integración de `ApiTicketsIT` con Failsafe. Las pruebas de integración inician un servidor en un puerto dinámico y envían peticiones HTTP reales. Los reportes se pueden revisar en `target/surefire-reports/` y `target/failsafe-reports/`.

El script usa el puerto 18080. Si está ocupado, se puede anteponer `PORT=18081` a la llamada. Para iniciar la API manualmente se puede usar `java -jar target/prioridad-tickets.jar 18080 manual` y detenerla con Ctrl+C.

## Pipeline de integración y despliegue

El workflow [automatizacion.yml](.github/workflows/automatizacion.yml) se ejecuta al abrir o actualizar un Pull Request hacia `main`, al hacer push a `main` o manualmente desde **Actions → Automatización de pruebas y despliegue → Run workflow**.

| Etapa | Acción | Condición de avance |
| --- | --- | --- |
| Build y pruebas automatizadas | Ejecuta `mvn clean verify` y guarda reportes y JAR | Compilación y ambas suites aprobadas |
| Aceptación HTTP | Descarga y levanta el mismo JAR. Valida versión, prioridad crítica y respuesta al impacto inválido | Salud y respuestas esperadas |
| Despliegue de prueba | Inicia una versión base y luego promueve la candidata. Inyecta un JAR inválido tras el despliegue sano | Despliegue sano y restauración verificada |

[despliegue.sh](scripts/despliegue.sh) conserva el JAR de la versión anterior, detiene la instancia activa e inicia la versión entrante. Si falla la comprobación de salud, vuelve a iniciar el JAR anterior y comprueba su versión y sus respuestas HTTP. El script devuelve internamente 10 para una restauración verificada y 11 si la recuperación falla. La demostración completa solo termina con código 0 cuando el despliegue sano funciona y se confirma la recuperación del fallo inducido.

El ambiente de prueba es temporal y usa el runner de GitHub Actions con acceso por localhost. En la demostración, base y candidata son copias sanas del mismo JAR con identificadores de despliegue distintos. El fallo se inyecta en una tercera copia. El cambio usa reinicio del proceso y la instancia se cierra cuando termina el ejercicio.

## Evidencias

- [Compilación y suites Maven](evidencias/01-maven-local.log), [aceptación](evidencias/02-aceptacion-local.log) y [despliegue con rollback](evidencias/03-despliegue-local.log) ejecutados localmente.
- En GitHub, el resumen de la ejecución permite ver los tres jobs. Los artefactos `informes-maven`, `evidencia-aceptacion` y `evidencia-despliegue` permiten descargar los registros.
- Para completar las evidencias de la entrega, guarda capturas del resumen en verde, del paso Maven y del paso de despliegue que confirma la versión restaurada. Si guardas las capturas como `evidencias/01-ci.png`, `evidencias/02-maven.png` y `evidencias/03-rollback.png`, agrega sus enlaces aquí.

## Publicar y comprobar el flujo de ramas

1. Crea en GitHub un repositorio vacío llamado `examen-automatizacion`. Si es privado, da acceso al docente que revisará la entrega. Abre Git Bash en la carpeta que contiene `pom.xml` y ejecuta lo siguiente. Cambia `TU_USUARIO` por tu usuario de GitHub.

   ```bash
   git init -b main
   git add .
   git commit -m "Implementa API de tickets y pipeline de pruebas"
   git remote add origin https://github.com/TU_USUARIO/examen-automatizacion.git
   git push -u origin main
   ```

   Si Git te pide identificarte, configura `git config user.name "Tu nombre"` y `git config user.email "Tu correo"` en esta carpeta. Después repite el commit y los comandos siguientes.

2. Para evidenciar la estrategia Trunk-Based, crea una rama corta. Agrega tu nombre a este README como un cambio de documentación y súbelo.

   ```bash
   git switch -c feat/documentar-flujo
   # Edita y guarda README.md antes de continuar.
   git add README.md
   git commit -m "Agrega autor al README"
   git push -u origin feat/documentar-flujo
   ```

3. En GitHub, abre el Pull Request de `feat/documentar-flujo` a `main`. Confirma que pasan build y aceptación. El despliegue se omite en el Pull Request y se ejecuta en `main` después de fusionar. Guarda el enlace del Pull Request y elimina la rama desde GitHub.

4. Abre **Actions** y selecciona la ejecución del merge en `main`. Guarda su enlace, toma las capturas indicadas en Evidencias y adjúntalas al Word. Si guardas las imágenes también en el repositorio, actualiza sus enlaces en este README a través de otro Pull Request.

## Autor

Rolando Rivera