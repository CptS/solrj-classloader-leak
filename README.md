# SolrJ Classloader Leak Demonstration

Demo project for this Question on Stackoverflow: http://stackoverflow.com/q/39741147/4864870

The Classloader leak is reproducible under the following conditions (at least this is the environment which I have tested): 

- Tomcat Version: Apache Tomcat/8.0.14 (Debian)
- JVM Version: 1.8.0_91-b14
- JVM Vendor: Oracle Corporation
- OS Name: Linux
- OS Version: 3.16.0-4-amd64
- Architecture: amd64

**Note:** This project already contains a _"ClassLoader-Leak-Preventor"_ (`de.test.SSLClassloaderLeakPreventor`). So if you just run this project you will **not** see the classloader leak!

You have to disable the Shutdown-Hook in `de.test.SSLClassloaderLeakPreventor` if you want to reproduce the leak.

## Environment

1. **Solr**  
   The URL defined in [`SolrJConfig.SOLR_URL`](src/main/java/de/test/spring/SolrJConfig.java) points to the Solr server.
   - download: http://www.apache.org/dyn/closer.lua/lucene/solr/6.2.1
   - run: `solr start -p 8984`
2. **Certificate with unparseable extension**  
   I don't know exactly where the certificate with the _unparseable extension_ came from. I haven't installed any additional certificate in the keystore.
   The [`cacerts`](cacerts) Keystore used in my tests is attached.

## Potential for improvement

1. The `SSLClassloaderLeakPreventor` simply removes the Exception. Maybe it would be saver to check if there are references to Classes from the `WebappClassLoader`
2. Integration in `classloader-leak-prevention`, see https://github.com/mjiderhamn/classloader-leak-prevention/issues/58
