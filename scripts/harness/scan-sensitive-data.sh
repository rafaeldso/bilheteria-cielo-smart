#!/usr/bin/env bash
# Sensor S-02: repository-wide scan for card numbers (Luhn-valid PANs), CPFs and
# secrets outside test code. Test PANs from card networks are allowed only in tests.
# Usage: scan-sensitive-data.sh [file ...]   (no args = whole project)
set -uo pipefail
source "$(dirname "$0")/lib.sh"

list_targets() { if (( $# > 0 )); then printf '%s\n' "$@"; else list_project_files; fi; }

list_targets "$@" | python3 -I -c '
import re, sys, os

root = os.environ["HARNESS_ROOT"]
TEXT_EXT = (".kt", ".kts", ".java", ".xml", ".json", ".yml", ".yaml", ".properties", ".toml", ".gradle", ".txt", ".csv")
SKIP = re.compile(r"(^|/)(build|\.gradle|\.git|\.idea)/|/generated/|^docs/")
TEST = re.compile(r"/src/(test|androidTest|testFixtures)/|/core/testing/")
TEST_PANS = {"4111111111111111", "4012888888881881", "5555555555554444", "5105105105105100",
             "378282246310005", "6011111111111117", "4000000000000002"}
PAN = re.compile(r"(?<![\d.])(?:\d[ -]?){13,19}(?![\d.])")
CPF = re.compile(r"(?<!\d)\d{3}\.\d{3}\.\d{3}-\d{2}(?!\d)")

def luhn(n):
    s, alt = 0, False
    for d in reversed(n):
        d = int(d)
        if alt:
            d *= 2
            if d > 9: d -= 9
        s += d; alt = not alt
    return s % 10 == 0

def fail(rule, where, problem, fix):
    print(f"SENSOR S-02 FALHOU: {rule}\n  Onde: {where}\n  Problema: {problem}\n  Como corrigir: {fix}\n  Referência: docs/harness/guides/security.md\n")

count = 0
for rel in (l.strip() for l in sys.stdin):
    if not rel: continue
    rel = os.path.relpath(os.path.join(root, rel), root) if not rel.startswith("/") else os.path.relpath(rel, root)
    if SKIP.search(rel) or not rel.endswith(TEXT_EXT): continue
    is_test = bool(TEST.search("/" + rel))
    try:
        lines = open(os.path.join(root, rel), encoding="utf-8", errors="ignore").read().splitlines()
    except OSError:
        continue
    for i, line in enumerate(lines, 1):
        if "harness:allow" in line: continue
        for m in PAN.finditer(line):
            digits = re.sub(r"\D", "", m.group())
            if 13 <= len(digits) <= 19 and luhn(digits) and len(set(digits)) > 1:
                if is_test and digits in TEST_PANS: continue
                count += 1
                fail("PAN em texto claro", f"{rel}:{i}",
                     "Número com formato de cartão válido (Luhn)" + (" que não é PAN de teste oficial." if is_test else " fora de código de teste."),
                     "Remova. Em produção use token/PAN mascarado; em teste use PANs oficiais de teste (ex.: 4111111111111111).")
        if not is_test and CPF.search(line):
            count += 1
            fail("CPF literal", f"{rel}:{i}", "CPF formatado em código de produção.", "Remova; CPFs só em fixtures de teste, gerados.")

if count:
    print(f"S-02: {count} ocorrência(s) de dado sensível.")
    sys.exit(1)
print("SENSOR S-02 OK: nenhum dado sensível encontrado")
'
