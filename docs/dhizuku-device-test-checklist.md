# Dhizuku stack — device test checklist

Automated verification (CI/local): `assembleDebug`, `testDebugUnitTest` — passed on `prC`, `prD`, `prE` branches.

Manual tests required before upstream merge of #1345–#1347:

## Standalone Dhizuku (`com.rosan.dhizuku`)

- [ ] Grant API permission in Dhizuku app
- [ ] Select Dhizuku installer in Settings
- [ ] Single app install from app detail
- [ ] Single app update
- [ ] Silent uninstall with confirmation dialog
- [ ] Update All from Updates tab
- [ ] Auto-update after repository sync

## Frozen server (PR #13 wake)

- [ ] Background Dhizuku until process frozen (or use vendor freeze)
- [ ] Trigger install — verify `wakeDhizukuServer` recovers without opening Dhizuku UI
- [ ] Update-all batch with frozen server mid-run

## Built-in server (OwnDroid / device-owner, PR #12 + UI follow-up)

- [ ] No standalone Dhizuku app installed
- [ ] Device-owner Dhizuku server running
- [ ] Settings: can select Dhizuku installer (no false "not installed")
- [ ] Install / update / uninstall complete

## PR #14 fallback (deferred — do not merge until decided)

- [ ] Intentionally break Dhizuku RPC — verify Session fallback + one-time toast
- [ ] Update-all with fallback — confirm batch does not hang on Session prompts
