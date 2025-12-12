---
description: Project directory structure and organization rules
---

# Directory Structure Standards for NexusArchive

## Root Directory Rules

The root directory should ONLY contain:
- **Configuration files**: `package.json`, `tsconfig.json`, `vite.config.ts`, `.env.example`, `.gitignore`
- **Entry point**: `index.html`
- **Main README**: `README.md`

## Forbidden in Root

NEVER place these in root:
- `*.zip` - Move to `backups/` or delete
- `*.tar.gz` - Build artifacts, should be gitignored
- `*.log` - Should be in `logs/` directory and gitignored
- `*.jar` - Should be in `nexusarchive-java/libs/` or deleted
- `walkthrough.md` or other docs - Move to `docs/`
- Test output directories - Should be in `test/` or deleted
- `*.pid` - Runtime files, should be in `.run/`
- Shell scripts (`.sh`) - Should be in `scripts/`

## Directory Organization

```
nexusarchive/
├── src/                  # Frontend source code
├── public/               # Static assets
├── nexusarchive-java/    # Backend source code
├── deploy/               # Deployment configs and scripts
├── scripts/              # Utility and service scripts
│   ├── dev-start.sh
│   ├── restart-services.sh
│   └── ...
├── docs/                 # All documentation (except README.md)
├── backups/              # Backup files
├── logs/                 # Runtime log files (gitignored)
│   ├── backend.log
│   └── frontend.log
└── .run/                 # Runtime PID files (gitignored)
    ├── backend.pid
    └── frontend.pid
```

## When Adding New Files

1. **Scripts** → `scripts/`
2. **Documentation** → `docs/`
3. **Deployment configs** → `deploy/`
4. **Backend code** → `nexusarchive-java/src/`
5. **Frontend code** → `src/`
6. **Log files** → `logs/`
7. **PID/runtime files** → `.run/`

## Starting Services

Use scripts from the `scripts/` directory:
```bash
# Recommended: Use dev-start script
./scripts/dev-start.sh

# Or use restart-services script
./scripts/restart-services.sh
```

## Cleanup Command

If root gets polluted, run:
```bash
rm -f *.zip *.tar.gz *.log *.jar *.pid *.sh
rm -rf test_aip*/ .m2-temp/
```
