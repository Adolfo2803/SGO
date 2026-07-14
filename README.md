[![CI](https://github.com/Adolfo2803/SGO/actions/workflows/ci.yml/badge.svg)](https://github.com/Adolfo2803/SGO/actions/workflows/ci.yml)

# SGO — Sistema de Gestión de Oficios

Sistema para registro, control y turnado de oficios recibidos y enviados
en la Dirección del Hospital General.

## Requisitos

- Java 21
- Docker (para la base de datos y las pruebas)
- Maven (incluido via wrapper: `./mvnw`)

## Cómo levantar el proyecto

```bash
# 1. Base de datos
docker compose up -d db

# 2. Aplicación
./mvnw spring-boot:run

# 3. Pruebas
./mvnw test
```

La aplicación estará en `http://localhost:8080`.

## Integración continua

El workflow de GitHub Actions (`.github/workflows/ci.yml`) se ejecuta en cada **push a `main`**
y en cada **pull request contra `main`**. Realiza lo siguiente:

1. Compila el proyecto con Java 21 (Temurin).
2. Ejecuta **todas** las pruebas, incluidas las de integración con base de datos
   (Testcontainers levanta un PostgreSQL 16 automáticamente en el runner).
3. Empaqueta la aplicación (`./mvnw -B verify`).
4. Sube los reportes de Surefire como artefacto, incluso si el build falla,
   para poder diagnosticar sin reproducir localmente.

**Si el badge está en rojo** significa que el último push a `main` o el último PR
no compiló o tiene pruebas fallidas. Revisa la pestaña **Actions** del repositorio
para ver el detalle del error y descarga el artefacto `reportes-pruebas` si necesitas
los XML de Surefire.
