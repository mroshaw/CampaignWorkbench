# Create UTF-8 encoding
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)

# ------------------------------------------------------------
# Paths
# ------------------------------------------------------------
$versionFile = Resolve-Path "$PSScriptRoot\..\version.properties"
$javaOut = Resolve-Path "$PSScriptRoot\..\src\com\campaignworkbench\ide\Version.java"
$manifestPath = Resolve-Path "$PSScriptRoot\..\resources\META-INF\MANIFEST.MF"

# ------------------------------------------------------------
# Read properties
# ------------------------------------------------------------
$propsRaw = Get-Content $versionFile -Raw
$props = @{}

foreach ($line in $propsRaw -split "`r?`n") {
    if ($line -match "(.+)=(.+)") {
        $props[$matches[1]] = $matches[2]
    }
}

# ------------------------------------------------------------
# Increment build number
# ------------------------------------------------------------
$props["build"] = [int]$props["build"] + 1

# Write back properties atomically
$propsContent = ($props.GetEnumerator() | ForEach-Object { "$($_.Key)=$($_.Value)" }) -join "`r`n"
[System.IO.File]::WriteAllText($versionFile, $propsContent, $utf8NoBom)

# ------------------------------------------------------------
# Construct version string
# ------------------------------------------------------------
$version = "$($props.major).$($props.minor).$($props.build)"
Write-Host "New Version: $version"

# ------------------------------------------------------------
# Generate Version.java
# ------------------------------------------------------------
$javaContent = @"
package com.campaignworkbench.ide;

public final class Version {
    public static final int MAJOR = $($props.major);
    public static final int MINOR = $($props.minor);
    public static final int BUILD = $($props.build);

    public static final String VERSION =
            MAJOR + "." + MINOR + "." + BUILD;
}
"@

[System.IO.File]::WriteAllText($javaOut, $javaContent, $utf8NoBom)

# ------------------------------------------------------------
# Update or Insert Implementation-Version in MANIFEST.MF
# ------------------------------------------------------------
if (Test-Path $manifestPath) {

    $content = Get-Content $manifestPath -Raw

    if ($content -match "(?m)^Implementation-Version:\s*.+$") {
        # Replace existing line
        $content = [regex]::Replace(
                $content,
                "(?m)^Implementation-Version:\s*.+$",
                "Implementation-Version: $version"
        )
    }
    else {
        # Remove trailing whitespace/newlines and append
        $content = $content.TrimEnd() + "`r`nImplementation-Version: $version"
    }

    # Ensure CRLF ending
    if (-not $content.EndsWith("`r`n")) {
        $content += "`r`n"
    }

    [System.IO.File]::WriteAllText($manifestPath, $content, $utf8NoBom)
    Write-Host "MANIFEST.MF updated."
}