#!/bin/sh
# Weigert te starten zonder geldige API_BASE_URL; de waarde komt letterlijk in config.json en de CSP.
set -eu
if ! printf '%s' "${API_BASE_URL:-}" | grep -Eq '^https?://[A-Za-z0-9.-]+(:[0-9]+)?(/[A-Za-z0-9._~/-]*)?$'; then
    echo "API_BASE_URL ontbreekt of is ongeldig (verwacht bijv. https://api.example.nl)" >&2
    exit 1
fi
