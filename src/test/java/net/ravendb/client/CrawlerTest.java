package net.ravendb.client;

import org.junit.jupiter.api.Test;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.util.ConfigurationBuilder;

import java.io.Console;
import java.lang.reflect.Method;
import java.util.Set;

public class CrawlerTest {

    @Test
    public void crawlTestFiles() {
        Reflections reflections = new Reflections(new ConfigurationBuilder()
        .forPackages("net.ravendb")
        .addScanners(new MethodAnnotationsScanner()));

        Set<Method> tests = reflections.getMethodsAnnotatedWith(Test.class);
        for (Method test : tests) {
            System.out.println(test.getDeclaringClass().getName()
                    .substring(19).replaceAll("\\.", "/") + "::" + test.getName());
        }


    }
}
