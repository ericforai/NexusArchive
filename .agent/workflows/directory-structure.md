---
description: Project directory structure and organization rules
---

# Directory Structure Standards for NexusArchive

## Root Directory Rules

The root directory should ONLY contain:
- **Configuration files**: `package.json`, `tsconfig.json`, `vite.config.ts`, `.env.example`, `.gitignore`
- **Entry point**: `index.html`
- **Main README**: `README.md`
- **Service scripts**: `restart-services.sh`

## Forbidden in Root

NEVER place these in root:
- `*.zip` - Move to `backups/` or delete
- `*.tar.gz` - Build artifacts, should be gitignored
- `*.log` - Should be gitignored
- `*.jar` - Should be in `nexusarchive-java/libs/` or deleted
- `walkthrough.md` or other docs - Move to `docs/`
- Test output directories - Should be in `test/` or deleted

## Directory Organization

```
nexusarchive/
├── src/                  # Frontend source code
├── public/               # Static assets
├── nexusarchive-java/    # Backend source code
├── deploy/               # Deployment configs and scripts
├── scripts/              # Utility scripts
├── docs/                 # All documentation (except README.md)
└── backups/              # Backup files
```

## When Adding New Files

1. **Scripts** → `scripts/`
2. **Documentation** → `docs/`
3. **Deployment configs** → `deploy/`
4. **Backend code** → `nexusarchive-java/src/`
5. **Frontend code** → `src/`

## Cleanup Command

If root gets polluted, run:
```bash
rm -f *.zip *.tar.gz *.log *.jar
rm -rf test_aip*/ .m2-temp/
```
