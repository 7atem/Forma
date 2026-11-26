# Security Policy

## Security & Privacy Architecture

Forma is designed to keep personal training, nutrition, and body-measurement data on the user's device:

- **Encrypted storage (Android):** Local records are stored in SQLCipher-encrypted `forma.db` (Room v10) with keys wrapped by Android Keystore (`KeystoreManager`). Databases created since raw-key mode use the stored 32 random bytes directly; legacy passphrase-mode databases remain readable without silent re-keying or data deletion.
- **OS-protected storage (Apple):** iOS/watchOS source stores local state in OS-protected `forma.json` rather than third-party cloud storage.
- **Portable encrypted backups (`FORMABK1`):** Backups are password-encrypted, authenticated, bounded typed-record archives. Device keys are never exported and plaintext staging files are never written.
- **Minimal permissions & network boundary:** Android `INTERNET` permission is scoped strictly to optional free, public, keyless Open Food Facts queries. Health Connect and Apple Health integrations are strictly read-only after explicit user consent.
- **Local notifications:** Opt-in reminders never include logged user data in notification payloads; write actions (such as +250 ml water) go through a non-exported receiver on explicit tap.

## Supported Versions

Forma is currently in **development alpha** (`0.7.0-alpha07`). Security fixes are applied to the active development branch.

| Version | Supported |
| :--- | :--- |
| `0.7.x-alpha` (current) | Yes |
| `< 0.7.0-alpha` | No |

## Reporting a Vulnerability

If you discover a security issue (such as key handling, backup cryptography, permission bypass, injection in CSV/JSON/transfer import, or unintended data exposure):

1. **Please do not open a public GitHub issue** with exploit details.
2. Report it privately using **GitHub Security Advisories** (`Security` → `Advisories` → `Report a vulnerability` on this repository).
3. Include:
   - Affected platform/module (Android, iOS/watchOS, `core:backup`, `core:data`, `core:wearables`, or scripts)
   - Steps to reproduce or minimal proof of concept
   - Expected vs. actual security boundary behavior

We will acknowledge reports and work on a verified fix with regression tests before public disclosure.
