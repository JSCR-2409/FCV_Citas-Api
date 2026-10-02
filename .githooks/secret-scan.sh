#!/usr/bin/env bash
# Detecta credenciales y patrones de secreto antes de versionar.
#
# Uso:
#   secret-scan.sh            revisa las lineas anadidas en el area de preparacion (modo hook)
#   secret-scan.sh --tracked  revisa el contenido completo de los archivos versionados y nuevos
#
# En modo hook solo se examinan las lineas que introduce el commit: lo preexistente se audita
# aparte con --tracked, para que un hallazgo antiguo no bloquee trabajo no relacionado.
#
# Una linea legitima se excluye anotandola, en la misma linea, con  allow-secret  y un motivo.
# Salida: 0 si esta limpio, 1 si encuentra algo.

set -uo pipefail

MODE="${1:---staged}"

AWK_RULES='
BEGIN {
  n = 0
  # regex | distingue mayusculas | descripcion
  add("-----BEGIN [A-Z ]*PRIVATE KEY-----", 1, "clave privada")
  add("AKIA[0-9A-Z]{16}", 1, "access key de AWS")
  add("AIza[0-9A-Za-z_-]{35}", 1, "API key de Google")
  add("gh[pousr]_[A-Za-z0-9]{36}", 1, "token de GitHub")
  add("xox[baprs]-[0-9A-Za-z-]{10,}", 1, "token de Slack")
  add("eyJ[A-Za-z0-9_-]{10,}\\.eyJ[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}", 1, "JWT emitido")
  add("(password|passwd|contrasena|secret|token|api[_-]?key)[ \t]*[:=][ \t]*[\"'"'"'][^\"'"'"']{8,}[\"'"'"']", 0, "credencial embebida")
  # El contenido entre comillas excluye ; ( ) = para no saltar de una cadena a la siguiente
  # cuando el codigo esta minificado en una sola linea: eso daba falsos positivos.
  add("(password|passwd|contrasena|secret|credential)[a-z_]*[^\"'"'"' \t]{0,12}[ \t]*\\(?[ \t]*[\"'"'"'][^\"'"'"';()=]{8,}[\"'"'"']", 0, "credencial pasada como argumento")
  add("(secret|password|passwd|token|api[_-]?key)[a-z_.-]*[\"'"'"'][ \t]*,[ \t]*[\"'"'"'][^\"'"'"']{12,}[\"'"'"']", 0, "valor por defecto literal junto a una clave sensible")
  add("(" KEYS ")[ \t]*[:=][ \t]*[A-Za-z0-9/+_.-]{8,}", 1, "valor literal en variable sensible")
  add("[$]\\{(" KEYS "):[^}  ]{8,}\\}", 1, "valor por defecto embebido en variable sensible")
}

function add(re, cased, label) { n++; rx[n] = re; cs[n] = cased; lb[n] = label }

function check(path, lineno, text,    i, probe) {
  if (text ~ /allow-secret/) return
  for (i = 1; i <= n; i++) {
    probe = cs[i] ? text : tolower(text)
    if (probe ~ rx[i]) {
      body = text
      sub(/^[ \t]+/, "", body)
      printf "  [SECRETO] %s:%s — %s\n", path, lineno, lb[i]
      printf "            %s\n", substr(body, 1, 90)
      found++
      return
    }
  }
}
'

# Las reglas con nombres de variable usan KEYS; los literales van en minusculas porque
# check() compara contra tolower() cuando la regla no distingue mayusculas.
KEYS='JWT_ACCESS_SECRET|JWT_REFRESH_SECRET|MYSQL_ROOT_PASSWORD|MYSQL_PASSWORD|DB_PASSWORD|GEMINI_API_KEY|N8N_API_KEY|SMTP_PASSWORD'

findings=0

# Un .env nunca se versiona; .env.example si, porque por definicion solo lleva plantillas.
while IFS= read -r f; do
  [ -n "$f" ] || continue
  case "$(basename "$f")" in
    .env|.env.local|.env.production|.env.development)
      echo "  [SECRETO] $f — los archivos .env no se versionan (usar .env.example)"
      findings=$((findings + 1))
      ;;
  esac
done < <(git diff --cached --name-only --diff-filter=ACM 2>/dev/null)

if [ "$MODE" = "--tracked" ]; then
  mapfile -t files < <(git ls-files --cached --others --exclude-standard \
    | grep -vE '\.(png|jpe?g|gif|ico|pdf|jar|woff2?|svg)$|(^|/)\.env\.example$|(^|/)\.githooks/secret-scan\.sh$' || true)
  if [ "${#files[@]}" -gt 0 ]; then
    report="$(awk -v KEYS="$KEYS" "$AWK_RULES"'
      FNR == 1 { }
      { check(FILENAME, FNR, $0) }
      END { exit 0 }
    ' "${files[@]}")"
  else
    report=""
  fi
else
  # Solo las lineas anadidas. El numero de linea sale de la cabecera del hunk.
  report="$(git diff --cached -U0 --diff-filter=ACM 2>/dev/null | awk -v KEYS="$KEYS" "$AWK_RULES"'
    /^\+\+\+ b\// { path = substr($0, 7); next }
    /^@@ / {
      match($0, /\+[0-9]+/)
      lineno = substr($0, RSTART + 1, RLENGTH - 1) + 0
      next
    }
    /^\+/ {
      if (path != "" && path != "/dev/null" && path !~ /\.githooks\/secret-scan\.sh$/ && path !~ /\.env\.example$/)
        check(path, lineno, substr($0, 2))
      lineno++
      next
    }
    END { exit 0 }
  ')"
fi

if [ -n "$report" ]; then
  printf '%s\n' "$report"
  findings=$((findings + $(printf '%s\n' "$report" | grep -c '\[SECRETO\]')))
fi

if [ "$findings" -gt 0 ]; then
  echo ""
  echo "  Se encontraron $findings hallazgo(s). Mueva el valor a una variable de entorno o,"
  echo "  si es un dato de laboratorio legitimo, anote la linea con  allow-secret  y el motivo."
  exit 1
fi

exit 0
