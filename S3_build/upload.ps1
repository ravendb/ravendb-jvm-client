$base_dir  = resolve-path .
$version = "3.0.0-SNAPSHOT"
$uploader = "..\Uploader\S3Uploader.exe"

$scriptPath = $MyInvocation.MyCommand.Path
$scriptDir = Split-Path $scriptPath

get-module psake | remove-module

import-module (Get-ChildItem "$scriptDir\packages\psake.*\tools\psake.psm1" | Select-Object -First 1)

Write-Host "Starting upload"
if (Test-Path $uploader) {
	$log = "RavenDB Java Client v3.0"
	$file = "$base_dir\target\ravendb-build-$version-dist.zip"
	$currentUploadCategory = "RavenDB Java Client"
	
	Write-Host "Executing: $uploader ""$currentUploadCategory"" ""$version"" $file ""$log"""
	
	Exec { &$uploader "$currentUploadCategory" "$version" $file "$log" }
	
	if ($lastExitCode -ne 0) {
		Write-Host "Failed to upload to S3: $lastExitCode."
	} else {
		break
	}

	if ($lastExitCode -ne 0) {
		Write-Host "Failed to upload to S3: $lastExitCode. UploadTryCount: $uploadTryCount. Build will fail."
		throw "Error: Failed to publish build"
    }
	
}
else {
	Write-Host "could not find upload script $uploadScript, skipping upload"
}