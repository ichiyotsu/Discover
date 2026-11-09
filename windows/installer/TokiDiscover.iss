; TokiDiscover 0.1.0 unpackaged installer. Publish output is staged next to this script.

#define MyAppName "TokiDiscover"
#define MyAppVersion "0.1.0"
#define MyAppExeName "TokiDiscover.exe"
#define PublishDir "..\TokiDiscover\bin\publish"

[Setup]
AppId={{7E4C1A2B-6D58-4F0E-9A11-2B8C4D6E0F31}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppPublisher=TokiDiscover
DefaultDirName={localappdata}\TokiDiscover
DefaultGroupName={#MyAppName}
DisableProgramGroupPage=yes
OutputDir=..\..\dist
OutputBaseFilename=TokiDiscover-0.1.0-windows-setup
Compression=lzma2
SolidCompression=yes
WizardStyle=modern
PrivilegesRequired=lowest
ArchitecturesAllowed=x64
ArchitecturesInstallIn64BitMode=x64
UninstallDisplayIcon={app}\{#MyAppExeName}
SetupIconFile=..\TokiDiscover\Assets\App.ico

[Files]
Source: "{#PublishDir}\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{autoprograms}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"
Name: "{userdesktop}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; Tasks: desktopicon

[Tasks]
Name: "desktopicon"; Description: "创建桌面快捷方式"; GroupDescription: "附加任务:"

[Run]
Filename: "{app}\{#MyAppExeName}"; Description: "启动 TokiDiscover"; Flags: nowait postinstall skipifsilent
