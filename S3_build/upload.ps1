$base_dir  = resolve-path .
$version = "3.0.0-SNAPSHOT"
$uploader = "..\Uploader\S3Uploader.exe"

Write-Host "Starting upload"
if (Test-Path $uploader) {
	Write-Host "in if"
	$log = "RavenDB Java Client v3.0"
	Write-Host "iafter log"
	$file = "target\ravendb-build-$version-dist.zip"
	Write-Host "after file"
	$currentUploadCategory = "RavenDB-Unstable"
	
	
	Write-Host "Executing: $uploader ""$currentUploadCategory"" ""$version"" $file ""$log"""
	
	$uploadTryCount = 0
	while ($uploadTryCount -lt 5) {
		$uploadTryCount += 1
		# Uncomment !Exec { &$uploader "$currentUploadCategory" "$version" $file "$log" }
		
		if ($lastExitCode -ne 0) {
			Write-Host "Failed to upload to S3: $lastExitCode. UploadTryCount: $uploadTryCount"
		} else {
			break
		}
	}
	
	if ($lastExitCode -ne 0) {
		Write-Host "Failed to upload to S3: $lastExitCode. UploadTryCount: $uploadTryCount. Build will fail."
		throw "Error: Failed to publish build"
    }
	
}
else {
	Write-Host "could not find upload script $uploadScript, skipping upload"
}

Write-Host "end"