package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.indexes.AdditionalAssembly;
import net.ravendb.client.documents.indexes.IndexDefinition;
import net.ravendb.client.documents.operations.indexes.PutIndexesOperation;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RavenDB_15753Test extends RemoteTestBase {

    @Test
    public void additionalAssemblies_Runtime() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            IndexDefinition indexDefinition = new IndexDefinition();
            indexDefinition.setName("XmlIndex");
            indexDefinition.setMaps(Collections.singleton("from c in docs.Companies select new { Name = typeof(System.Xml.XmlNode).Name }"));

            Set<AdditionalAssembly> assemblies = new HashSet<>();
            indexDefinition.setAdditionalAssemblies(assemblies);

            assemblies.add(AdditionalAssembly.fromRuntime("System.Xml"));
            assemblies.add(AdditionalAssembly.fromRuntime("System.Xml.ReaderWriter"));
            assemblies.add(AdditionalAssembly.fromRuntime("System.Private.Xml"));

            store.maintenance().send(new PutIndexesOperation(indexDefinition));
        }
    }

    @Test
    public void additionalAssemblies_Runtime_InvalidName() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            assertThatThrownBy(() -> {
                IndexDefinition indexDefinition = new IndexDefinition();
                indexDefinition.setName("XmlIndex");
                indexDefinition.setMaps(Collections.singleton("from c in docs.Companies select new { Name = typeof(System.Xml.XmlNode).Name }"));
                indexDefinition.setAdditionalAssemblies(Collections.singleton(AdditionalAssembly.fromRuntime("Some.Assembly.That.Does.Not.Exist")));

                store.maintenance().send(new PutIndexesOperation(indexDefinition));
            })
                    .hasMessageContaining("Cannot load assembly 'Some.Assembly.That.Does.Not.Exist'")
                    .hasMessageContaining("Could not load file or assembly 'Some.Assembly.That.Does.Not.Exist");
        }
    }

    @Test
    public void additionalAssemblies_NuGet() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            IndexDefinition indexDefinition = new IndexDefinition();
            indexDefinition.setName("XmlIndex");
            indexDefinition.setMaps(Collections.singleton("from c in docs.Companies select new { Name = typeof(System.Xml.XmlNode).Name }"));

            Set<AdditionalAssembly> assemblies = new HashSet<>();
            indexDefinition.setAdditionalAssemblies(assemblies);

            assemblies.add(AdditionalAssembly.fromRuntime("System.Private.Xml"));
            assemblies.add(AdditionalAssembly.fromNuGet("System.Xml.ReaderWriter", "4.3.1"));

            store.maintenance().send(new PutIndexesOperation(indexDefinition));
        }
    }

    @Test
    public void additionalAssemblies_NuGet_InvalidName() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            assertThatThrownBy(() -> {
                IndexDefinition indexDefinition = new IndexDefinition();
                indexDefinition.setName("XmlIndex");
                indexDefinition.setMaps(Collections.singleton("from c in docs.Companies select new { Name = typeof(System.Xml.XmlNode).Name }"));
                indexDefinition.setAdditionalAssemblies(Collections.singleton(AdditionalAssembly.fromNuGet("Some.Assembly.That.Does.Not.Exist", "4.3.1")));

                store.maintenance().send(new PutIndexesOperation(indexDefinition));
            })
                    .hasMessageContaining("Cannot load NuGet package 'Some.Assembly.That.Does.Not.Exist'")
                    .hasMessageContaining("NuGet package 'Some.Assembly.That.Does.Not.Exist' version '4.3.1' from 'https://api.nuget.org/v3/index.json' does not exist");
        }
    }

    @Test
    public void additionalAssemblies_NuGet_InvalidSource() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            assertThatThrownBy(() -> {
                IndexDefinition indexDefinition = new IndexDefinition();
                indexDefinition.setName("XmlIndex");
                indexDefinition.setMaps(Collections.singleton("from c in docs.Companies select new { Name = typeof(System.Xml.XmlNode).Name }"));
                indexDefinition.setAdditionalAssemblies(
                        Collections.singleton(
                                AdditionalAssembly.fromNuGet("System.Xml.ReaderWriter", "4.3.1", "http://some.url.that.does.not.exist.com")));

                store.maintenance().send(new PutIndexesOperation(indexDefinition));
            })
                    .hasMessageContaining("Cannot load NuGet package 'System.Xml.ReaderWriter' version '4.3.1' from 'http://some.url.that.does.not.exist.com'")
                    .hasMessageContaining("Unable to load the service index for source");

        }
    }
}
