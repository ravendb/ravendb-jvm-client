package net.ravendb.client.documents.indexes;

import org.apache.commons.lang3.StringUtils;

import java.util.Set;

public class AdditionalAssembly {

    private String assemblyName;
    private String assemblyPath;
    private String packageName;
    private String packageVersion;
    private String packageSourceUrl;
    private Set<String> usings;

    private AdditionalAssembly() {
    }

    public String getAssemblyName() {
        return assemblyName;
    }

    public void setAssemblyName(String assemblyName) {
        this.assemblyName = assemblyName;
    }

    public String getAssemblyPath() {
        return assemblyPath;
    }

    public void setAssemblyPath(String assemblyPath) {
        this.assemblyPath = assemblyPath;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getPackageVersion() {
        return packageVersion;
    }

    public void setPackageVersion(String packageVersion) {
        this.packageVersion = packageVersion;
    }

    public String getPackageSourceUrl() {
        return packageSourceUrl;
    }

    public void setPackageSourceUrl(String packageSourceUrl) {
        this.packageSourceUrl = packageSourceUrl;
    }

    public Set<String> getUsings() {
        return usings;
    }

    public void setUsings(Set<String> usings) {
        this.usings = usings;
    }

    public static AdditionalAssembly onlyUsings(Set<String> usings) {
        if (usings == null || usings.isEmpty()) {
            throw new IllegalArgumentException("Using cannot be null or empty");
        }

        AdditionalAssembly additionalAssembly = new AdditionalAssembly();
        additionalAssembly.setUsings(usings);
        return additionalAssembly;
    }

    public static AdditionalAssembly fromRuntime(String assemblyName) {
        return AdditionalAssembly.fromRuntime(assemblyName, null);
    }

    public static AdditionalAssembly fromRuntime(String assemblyName, Set<String> usings) {
        if (StringUtils.isBlank(assemblyName)) {
            throw new IllegalArgumentException("AssemblyName cannot be null or whitespace.");
        }

        AdditionalAssembly additionalAssembly = new AdditionalAssembly();
        additionalAssembly.setAssemblyName(assemblyName);
        additionalAssembly.setUsings(usings);
        return additionalAssembly;
    }

    public static AdditionalAssembly fromPath(String assemblyPath)  {
        return AdditionalAssembly.fromPath(assemblyPath, null);
    }

    public static AdditionalAssembly fromPath(String assemblyPath, Set<String> usings) {
        if (StringUtils.isBlank(assemblyPath)) {
            throw new IllegalArgumentException("AssemblyPath cannot be null or whitespace.");
        }

        AdditionalAssembly additionalAssembly = new AdditionalAssembly();
        additionalAssembly.setAssemblyPath(assemblyPath);
        additionalAssembly.setUsings(usings);
        return additionalAssembly;
    }

    public static AdditionalAssembly fromNuGet(String packageName, String packageVersion) {
        return fromNuGet(packageName, packageVersion, null, null);
    }

    public static AdditionalAssembly fromNuGet(String packageName, String packageVersion, String packageSourceUrl) {
        return fromNuGet(packageName, packageVersion, packageSourceUrl, null);
    }

    public static AdditionalAssembly fromNuGet(String packageName, String packageVersion, String packageSourceUrl, Set<String> usings) {
        if (StringUtils.isBlank(packageName)) {
            throw new IllegalArgumentException("PackageName cannot be null or whitespace.");
        }
        if (StringUtils.isBlank(packageVersion)) {
            throw new IllegalArgumentException("PackageVersion cannot be null or whitespace.");
        }

        AdditionalAssembly additionalAssembly = new AdditionalAssembly();
        additionalAssembly.setPackageName(packageName);
        additionalAssembly.setPackageVersion(packageVersion);
        additionalAssembly.setPackageSourceUrl(packageSourceUrl);
        additionalAssembly.setUsings(usings);
        return additionalAssembly;
    }

}
