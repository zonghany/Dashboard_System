[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$mavenPath = Resolve-Path "..\..\..\vue-and-springboot-demo\apache-maven-3.8.8\bin\mvn.cmd"
Write-Host "Maven: $mavenPath"
& $mavenPath spring-boot:run