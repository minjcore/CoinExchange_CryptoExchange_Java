# Tooling

| File | Purpose |
|------|---------|
| [`md2pdf.sh`](md2pdf.sh) | Export spec/design markdown → PDF |
| [`exports/`](exports/) | Generated PDFs |
| `md2pdf.config.cjs`, `md2pdf.css`, `package.json` | md-to-pdf / pandoc helpers |

Run from repo root:

```bash
./tooling/md2pdf.sh spec/foundation.md
```

Or via root stub: `./md2pdf.sh spec/foundation.md`
