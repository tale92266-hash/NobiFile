# NobiFile — Integration Protocol v1 (diagnostics only)

Packages: NobiFile `com.nobifile.manager`, Nobi Code `com.nobicode.editor`,
Nobi Terminal `com.nobiterminal`. All three remain independently installable.

## Verified (PackageManager only)

- Whether each package is installed (Diagnostics screen).
- Whether ACTION_VIEW / ACTION_SEND choosers resolve on the device.
- Protocol version string: v1.

## NOT verified / NOT claimed

- Opening a specific file directly inside Nobi Code.
- Opening a terminal session at a specific working directory in Nobi Terminal.
- Executing any command from NobiFile.
- Passing a display path and assuming the receiver can access it.

## Current behaviour (honest)

NobiFile opens files via the system Open With chooser with a FileProvider URI
grant, and shares via ACTION_SEND. Project roots registered in Developer
workspace are the shared-workspace locations; each app can only access folders
Android permits it to access. SAF-picked folders are private to the picking app
unless the other app picks the same folder itself.

## Future contract (to be verified on device before claiming)

A future v2 contract would define explicit intent actions, extras, URI formats,
permission handling, error codes and version compatibility, and would require
matching intent handling in Nobi Code and Nobi Terminal. No undocumented
internal API of either app is hardcoded in v1.
