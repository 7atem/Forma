# Nemory preservation and resume record

Checkpoint created 3 November 2026 before independent Forma development.

| Item | Saved value |
|---|---|
| Original checkout | `D:\Vault Brain` |
| Original branch | `main` |
| Original HEAD | `c4e9f15c85bc49f4f9dc6dee76be4bcf6204718b` |
| Checkpoint tag | `checkpoint/before-forma-2026-11-03_150600` |
| Local backup directory | `D:\Archives\Nemory\2026-11-03_150600` |
| Source/history archive | `nemory-all-refs.bundle` |
| Integrity record | `manifest.json` (SHA-256, byte counts, scope) |
| Recovery instructions | `RESTORE.md` in backup directory |

Original working tree was clean before preservation. No Nemory source files, build configuration, package ID, release signing configuration or app data were changed for Forma. A checkpoint tag was added. The backup bundle includes all Git refs/history and passed `git bundle verify`. Selected ignored local configuration/signing files and the already-existing debug APK were copied into the local backup, separate from Forma. The APK was not rebuilt; correspondence to the checkpoint source is unverified.

To resume Nemory, open the original folder and continue on `main`. There is no need to restore or switch branches. To recover independently, use the backup's RESTORE instructions with a **new** clone destination. Never put the backup's private-local signing/configuration files in the Forma repository.

This is a local recovery copy on the same physical drive as the checkout, not an off-device disaster-recovery service. It does not contain a dump of installed-app private databases or Android Keystore keys. Generated build caches, model downloads, datasets and IDE state remain in the original checkout. Do not delete that directory on the assumption that every ignored byte is in the bundle.
