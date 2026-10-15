# Flujo de ramas y pull requests

`main` y `dev` solo se cambian mediante **pull request (PR)**, fusionada con **merge commit**. Relacionado: [versionado semántico (007)](007-versionado_semantico.md).

## Ramas

| Rama | Para qué | Quién escribe en ella |
|---|---|---|
| `main` | Releases estables. Cada fusión que sube versión publica tag y GitHub Release | Solo PR desde `dev` |
| `dev` | Integración de las fases terminadas | Solo PR desde `fase-*` |
| `fase-*` | Trabajo de una fase o cambio; siempre se crea **desde `dev`** | Push directo |

## Cómo se trabaja

```
git checkout dev && git pull
git checkout -b fase-N-nombre              # rama nueva desde dev
# ... commits convencionales (feat:, fix:, docs:, ci:...) ...
git push -u origin fase-N-nombre           # CI compila y verifica el APK

gh pr create --base dev                    # abrir la PR hacia dev
gh pr checks --watch                       # esperar a que CI termine en verde
gh pr merge <n> --merge                    # fusionar con merge commit
```

Cuando se quiera una release, se repite con `gh pr create --base main --head dev`.

## Por qué merge commit

El versionado lee los mensajes de commit de la rama (`feat:`, `fix:`…). Con merge commit se conservan todos y la versión sube según el mayor; con squash solo contaría el título de la PR y `dev` y `main` acabarían con historias distintas. El commit "Merge pull request…" no es convencional y el cálculo lo ignora.

## Cómo se hace cumplir (y hasta dónde)

El repositorio es **privado con cuenta gratuita**, y en ese caso GitHub no permite activar protección de ramas ni rulesets (la API responde `403 Upgrade to GitHub Pro or make this repository public`). Por eso la regla se apoya en dos avisos y no en un bloqueo:

1. **Guardia en CI** (`scripts/check-pr-merge.sh`, job `guard` de `.github/workflows/build.yml`): en cada push a `main` o `dev` comprueba con la API que el commit tiene una PR asociada. Si no la tiene, el job falla y marca el push en rojo, y GitHub avisa por correo. **No revierte nada.** Además, el job `release` depende del guardia: un push directo a `main` no publica release.
2. **Hook `pre-push` local** (`.githooks/pre-push`): rechaza cualquier push a `main` o `dev`, incluido borrarlas. Se activa una vez por clon:

   ```
   git config core.hooksPath .githooks
   ```

   Se puede saltar con `git push --no-verify`, así que protege contra descuidos, no contra una decisión deliberada. La fusión de una PR con `gh pr merge` ocurre en el servidor y no pasa por el hook.

También se ejecuta CI en las PR hacia `dev` y hacia `main`, y el workflow prueba los scripts (`test-compute-version.sh`, `test-check-pr-merge.sh`, `test-pre-push-hook.sh`) antes de compilar.

### Si el repo pasa a público o a GitHub Pro

Entonces sí se puede bloquear de verdad. Un ruleset sobre `main` y `dev` con:

- exigir pull request antes de fusionar (0 aprobaciones, si se trabaja en solitario);
- exigir que el check `build` esté en verde;
- bloquear force-push y borrado de la rama;
- no permitir saltarse las reglas (incluir administradores).

El guardia y el hook seguirían siendo válidos como segunda línea.

## Si algo falla

- **El guardia falla en una PR legítima:** la asociación commit–PR puede tardar unos segundos tras el merge; el script reintenta 6 veces. Si aun así falla, se relanza el job desde Actions.
- **Hay que corregir el propio workflow:** también se hace con una PR a `dev`; no se hace push directo ni para arreglarlo.
- **Release que no se publica:** el `GITHUB_TOKEN` por defecto del repo es de solo lectura y el job `release` pide `contents: write` explícitamente. Si GitHub lo rechaza (`Resource not accessible by integration`), hay que cambiar el ajuste de permisos de workflows del repo o publicar la release a mano con el APK del artefacto.
