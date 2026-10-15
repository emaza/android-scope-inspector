# Versionado semántico automático

Cada push genera una versión distinta que sigue [semver](https://semver.org/lang/es/) y se calcula a partir de los mensajes de commit. Plan y decisiones: [006](006-plan_de_versionado_semantico.md).

## Dónde se ve la versión

- **En la app:** pie fijo en todas las pantallas, por ejemplo `v0.2.0-fase-versionado.58 · build 58`.
- **En Ajustes › Aplicaciones › Scope Inspector › Información de la app:** el nombre de versión y el paquete `io.github.emaza.scopeinspector`.
- **En GitHub Actions:** el resumen de cada ejecución muestra la versión, y el log del paso "Verify APK version" imprime el `package`, `versionCode` y `versionName` reales del APK (`aapt2 dump badging`); el paso falla si no coinciden con lo calculado.
- **En el APK descargable:** el artefacto se llama `scope-inspector-<versión>.apk`.

## Cómo se escribe un commit

Formato: `tipo(ámbito opcional): descripción`. El tipo va en inglés y la descripción en español.

| Commit | Efecto en la versión |
|---|---|
| `feat(ubicacion): detectar actividad física` | sube **minor** (0.2.0 → 0.3.0) |
| `fix(mapa): corregir el User-Agent` · `perf: …` | sube **patch** (0.2.0 → 0.2.1) |
| `feat!: …` · `fix(api)!: …` · pie `BREAKING CHANGE: …` | sube **major** (0.2.0 → 1.0.0) |
| `docs:` `chore:` `ci:` `test:` `refactor:` `style:` `build:` `revert:` | no sube la versión |
| Cualquier otro mensaje (sin tipo, `Feature:`, con mayúscula…) | no se tiene en cuenta |

Si entre dos versiones hay varios commits, **gana el mayor** (un `feat` junto a varios `fix` da un minor) y los números de la derecha se reinician.

## Qué versión sale en cada rama

| Dónde | Versión | Tag y release |
|---|---|---|
| `main`, con commits que suben versión | `X.Y.Z` (siguiente a la última) | sí: tag `vX.Y.Z` y GitHub Release con el APK |
| `main`, sin commits que suban versión | `X.Y.Z+<build>` (la última más el número de build) | no |
| `dev`, `fase-*` y pull requests | `X.Y.Z-<rama>.<build>`, por ejemplo `0.3.0-fase-2-ubicacion.58` | no |

- **Versión base:** el último tag `vX.Y.Z` alcanzable desde el commit. Mientras no haya ninguno, la base es `0.1.0`; el historial anterior a este sistema no usaba commits convencionales y no cuenta.
- **`versionCode`** (el "build"): el número de ejecución del workflow, que crece en cada push y es único entre ramas. Android lo usa para saber qué instalación es más nueva.
- **Pre-releases:** en ramas de trabajo sin commits que suban versión se usa el siguiente patch, para que siempre queden por encima de la versión base.

## Cómo se calcula

`scripts/compute-version.sh` lee los commits desde el último tag, decide el bump y escribe `versionName`, `versionCode`, `isRelease`, `tag`, `bump` y `base`. El workflow ([`build.yml`](../.github/workflows/build.yml)) los pasa a Gradle con `-PappVersionName` y `-PappVersionCode`; sin esas propiedades, Gradle usa `0.1.0-dev` y `1`.

Se puede probar en cualquier repo git, sin Android:

```
bash scripts/test-compute-version.sh                       # 27 comprobaciones sobre repos temporales
BUILD_NUMBER=58 bash scripts/compute-version.sh            # versión que saldría ahora mismo
BRANCH=main BUILD_NUMBER=58 bash scripts/compute-version.sh
```

El workflow ejecuta esos tests antes de compilar.

## Releases

Al hacer merge a `main` con commits que suben versión, el job `release` crea el tag y la GitHub Release con el APK y las notas generadas. El push de ese tag no lanza otra ejecución. Ojo con el merge:

- **Merge normal:** cuentan los commits de la rama, así que deben ser convencionales.
- **Squash merge:** cuenta solo el título del PR, que debe ser convencional.

El APK de la release va firmado con la clave de debug: sirve para instalar a mano, no para Google Play.

## Cambio de paquete

El `applicationId` pasó de `com.example.scopeinspector` a `io.github.emaza.scopeinspector`. Android lo trata como **otra app**: la versión antigua sigue instalada por separado y hay que desinstalarla a mano; los permisos empiezan de cero en la nueva. El código y su `namespace` (`com.example.scopeinspector`) no cambian.

## Límites

- Con versión `0.x`, un cambio marcado como breaking lleva a `1.0.0`. Úsalo solo cuando se quiera dar ese salto.
- Los commits anteriores a este sistema no cuentan. El primer `feat:` creará la primera versión `0.2.0`.
