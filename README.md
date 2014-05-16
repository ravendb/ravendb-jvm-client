jvm-client
==========

RavenDB JVM Client

RavenDB Client is released as snapshot to sonatype central repository. 

Sample pom:

```
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.example</groupId>
  <artifactId>ravendb-example</artifactId>
  <version>0.0.1-SNAPSHOT</version>

  <dependencies>
    <dependency>
      <groupId>net.ravendb</groupId>
      <artifactId>ravendb-client</artifactId>
      <version>3.0.0-SNAPSHOT</version>
    </dependency>
  </dependencies>

  <repositories>
    <repository>
        <id>snapshots-repo</id>
        <url>https://oss.sonatype.org/content/repositories/snapshots</url>
        <releases>
           <enabled>false</enabled>
        </releases>
        <snapshots>
          <enabled>true</enabled>
        </snapshots>
     </repository>
  </repositories>
</project>
```
