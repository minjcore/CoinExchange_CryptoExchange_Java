#!/usr/bin/env python3
"""
cf_sync.py — sync claude/confluence/*.md files to Confluence

Usage:
  python3 claude/tools/cf_sync.py                  # sync all pages
  python3 claude/tools/cf_sync.py 43581441          # sync one page by CF ID
  python3 claude/tools/cf_sync.py --dry-run         # preview HTML only

Prerequisites:
  pip install markdown requests
  export CF_USER=your.email@domain.com
  export CF_TOKEN=your_atlassian_api_token
  (token: https://id.atlassian.com/manage-profile/security/api-tokens)

CF_DOMAIN defaults to nivc.atlassian.net — override with CF_DOMAIN env var.
"""

import os
import re
import sys
import json
import html as html_lib
import requests
import markdown
from markdown.extensions.tables import TableExtension
from markdown.extensions.fenced_code import FencedCodeExtension

CF_DOMAIN = os.environ.get("CF_DOMAIN", "nivc.atlassian.net")
CF_USER   = os.environ.get("CF_USER", "")
CF_TOKEN  = os.environ.get("CF_TOKEN", "")

BASE_URL = f"https://{CF_DOMAIN}/wiki/api/v2"

# Mapping: local file path → (CF page ID, title override or None)
# Title override = None means use h1 from the markdown file.
PAGES = {
    "claude/confluence/data-plane-architecture.md":                 ("43581441", None),
    "claude/confluence/conclusion.md":                              ("51609737", None),
    "claude/confluence/start-here/architecture-faq.md":             ("51544171", None),
    "claude/confluence/start-here/terminology.md":                  ("44924932", None),
    "claude/confluence/architecture-principles/wallet-accounting-overview.md": ("45842592", None),
    "claude/confluence/architecture-principles/platform-boundaries.md":        ("44826626", None),
    "claude/confluence/architecture-principles/correlation-idempotency.md":    ("44859398", None),
    "claude/confluence/domain-trds/accounting-trd.md":              ("43221005", None),
    "claude/confluence/domain-trds/wallet-trd.md":                  ("43843585", None),
    "claude/confluence/contracts-integration/integration-surfaces.md":         ("43220994", None),
    "claude/confluence/contracts-integration/core-platform-design.md":         ("42041345", None),
    "claude/confluence/contracts-integration/gtelpay-public-api.md":           ("51643159", None),
    "claude/confluence/contracts-integration/accounting-api.md":               ("51544687", None),
    "claude/confluence/contracts-integration/wallet-api.md":                   ("51610183", None),
    "claude/confluence/contracts-integration/core-commands-api.md":            ("51643061", None),
    "claude/confluence/build-process/business-process-deposit.md":  ("44859411", None),
    "claude/confluence/build-process/implementation-decisions.md":  ("49152021", None),
    # --- Architecture Decision Records (parent: 51642522) ---
    "adr/ADR-001-immutable-ledger.md":                              ("47972359", None),
    "adr/ADR-002-core-foundation-shared-library.md":                ("51544266", None),
    "adr/ADR-003-dual-schema-single-postgres.md":                   ("48594986", None),
    "adr/ADR-004-wallet-balance-snapshot.md":                       ("51184014", None),
    "adr/ADR-005-idempotency-key-strategy.md":                      ("51642542", None),
    "adr/ADR-006-two-phase-deposit.md":                             ("48594970", None),
    "adr/ADR-007-freeze-settle-async-outflow.md":                   ("51544287", None),
    "adr/ADR-008-saga-compensation-no-2pc.md":                      ("51609821", None),
    "adr/ADR-009-fee-ownership-orchestration.md":                   ("45777090", None),
    "adr/ADR-010-transit-accounts-net-zero.md":                     ("51184034", None),
    "adr/ADR-011-auth-identity-jwt-subject.md":                     ("51184055", None),
    "adr/ADR-012-orchestration-integration-forbidden-rules.md":     ("50987432", None),
    "adr/ADR-013-outbox-at-least-once-messaging.md":                ("48595305", None),
    "adr/ADR-014-reconciliation-w5-report-only.md":                 ("50987452", None),
    "adr/ADR-015-eod-settlement-independent-batch.md":              ("51184075", None),
    "adr/ADR-016-qr-pos-default-no-per-txn-wallet.md":              ("51609841", None),
    "adr/ADR-017-partial-batch-payroll-disbursement.md":            ("51544307", None),
    "adr/ADR-018-openapi-asyncapi-wire-truth.md":                   ("51642563", None),
    "adr/ADR-019-vnd-single-currency-v1.md":                        ("50987472", None),
    "adr/ADR-020-wallet-lanes-coa-control-mapping.md":              ("51642583", None),
    "adr/ADR-021-aging-jobs-async-pending.md":                      ("51544328", None),
    "adr/ADR-022-mtls-bank-webhooks.md":                            ("51609861", None),
    "adr/ADR-023-accounting-period-close.md":                       ("51642603", None),
    "adr/ADR-024-deposit-wallet-credit-dual-path.md":               ("50987492", None),
    "adr/ADR-025-ibft-napas-clearing-1112.md":                      ("51642623", None),
    "adr/ADR-026-wallet-never-reverses-accounting.md":              ("50987512", None),
    "adr/ADR-027-sync-payment-transfer-three-commits.md":           ("51642643", None),
    "adr/ADR-028-money-scale-four-half-up.md":                      ("48267270", None),
    "adr/ADR-029-wallet-locked-rejects-mutation.md":                ("51642663", None),
    "adr/ADR-030-virtual-account-deposit-mapping.md":               ("51609881", None),
    "adr/ADR-031-sql-ledger-invariant-ci.md":                       ("50987532", None),
    "adr/ADR-032-wallet-balance-monitoring.md":                     ("51642683", None),
    "adr/ADR-033-bank-poll-t2-frozen-tmax.md":                      ("50987552", None),
    "adr/ADR-034-locked-wallet-deposit-credit-reject.md":           ("51642703", None),
    "adr/ADR-035-rabbitmq-workers-not-temporal-v1.md":              ("47972375", None),
    "adr/ADR-036-accrual-basis-ledger-v1.md":                       ("51642723", None),
    "adr/ADR-037-tigerbeetle-ledger-backing-store.md":              ("51544348", None),
    "adr/ADR-038-orchestrator-separate-service-gateway-seam.md":    ("51609901", None),
    "adr/ADR-039-no-synchronous-wallet-aggregate-row.md":           ("51544368", None),
    "adr/ADR-040-user-multi-pocket-wallets.md":                     ("51642743", None),
    "adr/ADR-041-rabbitmq-orch-to-accounting-worker.md":            ("50987572", None),
}


# ---------------------------------------------------------------------------
# Markdown → CF HTML conversion
# ---------------------------------------------------------------------------

def strip_frontmatter(text: str) -> str:
    """Remove CF metadata blockquotes and horizontal rules (---) from markdown."""
    lines = text.splitlines()
    out = []
    for line in lines:
        # Strip CF metadata blockquote lines
        if line.startswith("> ") and any(k in line for k in (
            "CF page ID", "Source of truth", "ADR authority",
            "Wire contracts", "Parent:", "See also:", "Reflects:"
        )):
            continue
        # Strip horizontal rules
        if line.strip() == "---":
            continue
        out.append(line)
    return "\n".join(out)


def extract_title(text: str) -> str:
    """Pull the first # H1 as the page title."""
    for line in text.splitlines():
        if line.startswith("# "):
            return line[2:].strip()
    return "Untitled"


def preprocess_panels(text: str) -> str:
    """
    Convert blockquote patterns to CF panel divs before markdown parsing.
    > **Note:** ...   → panel-note
    > **Info:** ...   → panel-info
    > **Warning:** .. → panel-warning
    > **Source of truth:** ... → panel-success (green)
    """
    color_map = {
        "note": "panel-note",
        "info": "panel-info",
        "warning": "panel-warning",
        "tip": "panel-success",
        "source of truth": "panel-success",
        "tóm tắt": "panel-info",
        "tổng quan": "panel-info",
        "hot path": "panel-note",
    }

    lines = text.splitlines()
    out = []
    i = 0
    while i < len(lines):
        line = lines[i]
        if line.startswith("> "):
            # Collect all consecutive blockquote lines
            block_lines = []
            while i < len(lines) and lines[i].startswith("> "):
                block_lines.append(lines[i][2:])
                i += 1
            block_text = " ".join(block_lines).strip()

            # Detect panel type
            panel_class = "panel-info"
            lower = block_text.lower()
            for key, val in color_map.items():
                if key in lower:
                    panel_class = val
                    break

            # Emit as raw HTML placeholder (won't be re-parsed by markdown)
            inner_html = md_inline(block_text)
            out.append(f'<div data-type="{panel_class}"><p>{inner_html}</p></div>')
        else:
            out.append(line)
            i += 1
    return "\n".join(out)


def md_inline(text: str) -> str:
    """Convert inline markdown (bold, code, links) to HTML."""
    # bold **...**
    text = re.sub(r'\*\*(.+?)\*\*', r'<strong>\1</strong>', text)
    # italic *...*
    text = re.sub(r'\*(.+?)\*', r'<em>\1</em>', text)
    # inline code `...`
    text = re.sub(r'`([^`]+)`', r'<code>\1</code>', text)
    # links [text](url)
    text = re.sub(r'\[([^\]]+)\]\(([^)]+)\)', r'<a href="\2">\1</a>', text)
    return text


def convert_md_to_cf_html(md_text: str) -> str:
    """
    Convert markdown to CF-compatible HTML.
    Steps:
      1. Pre-process special patterns (panels, etc.)
      2. Use python-markdown for tables, code, lists, headings
      3. Post-process: clean up CF-incompatible artifacts
    """
    # Step 1: strip frontmatter header block
    md_text = strip_frontmatter(md_text)

    # Step 2: pre-process blockquote panels
    md_text = preprocess_panels(md_text)

    # Step 3: convert with python-markdown
    raw_html = markdown.markdown(
        md_text,
        extensions=[TableExtension(), FencedCodeExtension(), "nl2br"],
        output_format="html",
    )

    # Step 4: post-process
    # Wrap bare <table> → ensure thead/tbody exist (markdown ext already does this)
    # Remove <p> wrappers inside <th>/<td> that markdown sometimes adds
    raw_html = re.sub(r'<th><p>(.*?)</p></th>', r'<th><p>\1</p></th>', raw_html, flags=re.DOTALL)
    raw_html = re.sub(r'<td><p>(.*?)</p></td>', r'<td><p>\1</p></td>', raw_html, flags=re.DOTALL)

    # Ensure table cells always have <p> wrapper (CF requirement)
    raw_html = re.sub(r'<th>(?!<p>)(.*?)(?!</p>)</th>', r'<th><p>\1</p></th>', raw_html, flags=re.DOTALL)
    raw_html = re.sub(r'<td>(?!<p>)(.*?)(?!</p>)</td>', r'<td><p>\1</p></td>', raw_html, flags=re.DOTALL)

    # Fix <br /> → <br>
    raw_html = raw_html.replace("<br />", "<br>")

    # Remove the first <h1> (it's the page title, not body content)
    raw_html = re.sub(r'<h1>[^<]+</h1>\s*', '', raw_html, count=1)

    return raw_html


# ---------------------------------------------------------------------------
# Confluence API helpers
# ---------------------------------------------------------------------------

def get_page_version(page_id: str) -> int:
    """Fetch current version number for a page."""
    resp = requests.get(
        f"{BASE_URL}/pages/{page_id}",
        auth=(CF_USER, CF_TOKEN),
        headers={"Accept": "application/json"},
    )
    resp.raise_for_status()
    return resp.json()["version"]["number"]


def update_page(page_id: str, title: str, html_body: str, version: int) -> dict:
    """PUT /pages/{id} with new content."""
    payload = {
        "id": page_id,
        "status": "current",
        "title": title,
        "version": {"number": version + 1, "message": "cf_sync.py auto-sync from local"},
        "body": {
            "representation": "storage",
            "value": html_body,
        },
    }
    resp = requests.put(
        f"{BASE_URL}/pages/{page_id}",
        auth=(CF_USER, CF_TOKEN),
        headers={"Content-Type": "application/json", "Accept": "application/json"},
        data=json.dumps(payload),
    )
    resp.raise_for_status()
    return resp.json()


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

def sync_page(local_path: str, page_id: str, dry_run: bool = False) -> None:
    repo_root = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
    abs_path = os.path.join(repo_root, local_path)

    if not os.path.exists(abs_path):
        print(f"  [SKIP] {local_path} — file not found")
        return

    with open(abs_path, encoding="utf-8") as f:
        md_text = f.read()

    title = extract_title(md_text)
    html_body = convert_md_to_cf_html(md_text)

    if dry_run:
        print(f"\n{'='*60}")
        print(f"  Page: {page_id}  |  Title: {title}")
        print(f"  File: {local_path}")
        print(f"  HTML preview (first 800 chars):")
        print(html_body[:800])
        return

    version = get_page_version(page_id)
    result = update_page(page_id, title, html_body, version)
    new_ver = result["version"]["number"]
    print(f"  [OK] {local_path} → CF {page_id}  (v{new_ver})")


def main() -> None:
    args = sys.argv[1:]
    dry_run = "--dry-run" in args
    args = [a for a in args if not a.startswith("--")]

    if not dry_run and (not CF_USER or not CF_TOKEN):
        print("ERROR: set CF_USER and CF_TOKEN environment variables")
        print("  export CF_USER=your.email@company.com")
        print("  export CF_TOKEN=<atlassian_api_token>")
        sys.exit(1)

    # Filter to specific page IDs if provided
    target_ids = set(args)

    print(f"cf_sync.py  |  domain={CF_DOMAIN}  |  dry_run={dry_run}")
    print(f"Pages to sync: {len(PAGES)}")
    print()

    for local_path, (page_id, _) in PAGES.items():
        if target_ids and page_id not in target_ids:
            continue
        print(f"  Syncing {page_id} ← {local_path}")
        try:
            sync_page(local_path, page_id, dry_run=dry_run)
        except requests.HTTPError as e:
            print(f"  [ERR] {page_id}: HTTP {e.response.status_code} — {e.response.text[:200]}")
        except Exception as e:
            print(f"  [ERR] {page_id}: {e}")

    print("\nDone.")


if __name__ == "__main__":
    main()
