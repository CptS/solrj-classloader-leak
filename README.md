# SolrJ Classloader Leak Demonstration

The Classloader leak is reproducible under the following conditions (at least this is the environment which I have tested): 

- Tomcat Version: Apache Tomcat/8.0.14 (Debian)
- JVM Version: 1.8.0_91-b14
- JVM Vendor: Oracle Corporation
- OS Name: Linux
- OS Version: 3.16.0-4-amd64
- Architecture: amd64

**Note:** This project already contains a _"ClassLoader-Leak-Preventor"_ (`de.test.SSLClassloaderLeakPreventor`). So if you just run this project you will **not** see the classloader leak!

You have to disable the Shutdown-Hook in `de.test.SSLClassloaderLeakPreventor` if you want to reproduce the leak.
