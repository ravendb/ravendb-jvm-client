properties {
	$base_dir  = resolve-path .
	$version = "3.0"
	$release_dir = "$base_dir\Release"
	$uploader = "..\..\Uploader\S3Uploader.exe"
	$global:configuration = "Debug"
}

task default -depends Vnext3, Upload

task Vnext3 {
	$global:uploadCategory = "RavenDB-Unstable"
	$global:uploadMode = "Vnext3"
	$global:configuration = "Release"
}

task Unstable {
	$global:uploadCategory = "RavenDB-Unstable"
	$global:uploadMode = "Unstable"
	$global:configuration = "Release"
}

task Stable {
	$global:uploadCategory = "RavenDB"
	$global:uploadMode = "Stable"
	$global:configuration = "Release"
}

task Upload {
	Write-Host "Starting upload"
	if (Test-Path $uploader) {
		$log = $env:push_msg 
		if(($log -eq $null) -or ($log.Length -eq 0)) {
		  $log = git log -n 1 --oneline		
		}
		
		$log = $log.Replace('"','''') # avoid problems because of " escaping the output
		
		$zipFile = "target\ravendb-build-3.0.0-SNAPSHOT-dist.zip"
				
		$files = @(@($zipFile, "$uploadCategory"))
		
		foreach ($obj in $files)
		{
			$file = $obj[0]
			$currentUploadCategory = $obj[1]
			write-host "Executing: $uploader ""$currentUploadCategory"" ""$env:buildlabel"" $file ""$log"""
			
			$uploadTryCount = 0
			while ($uploadTryCount -lt 5) {
				$uploadTryCount += 1
				# Uncomment !Exec { &$uploader "$currentUploadCategory" "$env:buildlabel" $file "$log" }
				
				if ($lastExitCode -ne 0) {
					write-host "Failed to upload to S3: $lastExitCode. UploadTryCount: $uploadTryCount"
				}
				else {
					break
				}
			}
			
			if ($lastExitCode -ne 0) {
				write-host "Failed to upload to S3: $lastExitCode. UploadTryCount: $uploadTryCount. Build will fail."
				throw "Error: Failed to publish build"
			}
		}
	}
	else {
		Write-Host "could not find upload script $uploadScript, skipping upload"
	}
}
