package de.test;

import java.lang.reflect.Field;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Map;
import javax.net.ssl.SSLContextSpi;
import javax.net.ssl.X509TrustManager;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import lombok.extern.slf4j.Slf4j;

/**
 * Dies ist ein Shutdown-Hook der einen potentiellen ClassLoader-Leak verhindert.<br>
 * Dieser entsteht in etwa folgender Maßen:
 * <ol>
 * <li>Anwendung öffnet HTTP-Verbindung (Bsp.: {@code HttpSolrClient}}</li>
 * <li>Dadurch wird der SSL-Context initialisiert</li>
 * <li>Dabei werden die Zertifikate des trustStore validiert</li>
 * <li>Wenn ein Fehler auftritt wird das Zertifikat in eine Liste gehängt und mit der Exception versehen</li>
 * <li>Da der SSL-Context im System-Classloader liegt und die Exception eine Referenz im Stacktrace/Backtrace auf Klassen der Anwendung haben entsteht ein ClassLoader-Leak</li>
 * </ol>
 */
// squid:S1191 verbietet die Verwendung von Klassen im "sun.*"-Package. Diese werden jedoch benötigt um diesen ClassLoader-Leak zu verhindern.
@SuppressWarnings("squid:S1191")
@WebListener
@Slf4j
public class SSLClassloaderLeakPreventor implements ServletContextListener {

    @Override
    public void contextInitialized(final ServletContextEvent servletContextEvent) {
        log.debug("Shutdown hook registered to cleanup references for in invalid certificates which leads to classloader leaks...");
    }

    // squid:S2221 und checkstyle:illegalcatch sagen, dass man "Exception" nicht catchen soll. Das wird hier aber gemacht, damit diese Fehler beim Herunterfahren nicht komplett verschluckt werden.
    @SuppressWarnings({"squid:S2221", "checkstyle:illegalcatch"})
    @Override
    public void contextDestroyed(final ServletContextEvent servletContextEvent) {
        try {
            log.debug("Checking for stacktraces in SSL context which could lead to a classloader leak ...");
            final SSLContextSpi sslContext = getFieldValue(sun.security.ssl.SSLContextImpl.DefaultSSLContext.class, "defaultImpl");
            final X509TrustManager trustManager = getFieldValue(sun.security.ssl.SSLContextImpl.class, "trustManager", sslContext);
            final Collection<X509Certificate> certificates = getFieldValue(trustManager.getClass(), "trustedCerts", trustManager);

            if (certificates.isEmpty()) {
                log.debug("No certificates found! nothing to do ...");
            } else {
                log.trace("{} certificates found! Searching for uparseable extensions with stacktraces ...", certificates.size());
                handleCertificates(certificates);
                log.debug("SSL context is now cleaned up!");
            }
        } catch (final Exception e) {
            log.error("Error while removing stacktraces from SSL context. This could lead to a classloader memory leak!", e);
        }
    }

    /**
     * Verarbeitet die Zertifikate tes TrustStores.
     *
     * @param certificates Die Zertifikate.
     */
    private static void handleCertificates(final Collection<X509Certificate> certificates) {
        for (X509Certificate certificate : certificates) {
            if (certificate instanceof sun.security.x509.X509CertImpl) {
                handleCertificate((sun.security.x509.X509CertImpl) certificate);
            } else {
                log.warn("Certificate {} should be a 'sun.security.x509.X509CertImpl' but is a '{}'. It will be skipped!", certificate.getClass().getCanonicalName());
            }
        }
    }

    /**
     * Verarbeitet ein einzelnes Zertifikat.
     *
     * @param certificate Das Zertifikat.
     */
    private static void handleCertificate(final sun.security.x509.X509CertImpl certificate) {
        final sun.security.x509.X509CertInfo info = getFieldValue(sun.security.x509.X509CertImpl.class, "info", certificate);
        final sun.security.x509.CertificateExtensions extensions = getFieldValue(sun.security.x509.X509CertInfo.class, "extensions", info);
        if (extensions == null) {
            log.trace("Certificate {} has no extensions.", certificate.getSubjectDN());
        } else {
            final Map<String, sun.security.x509.Extension> unparseableExtensions = getFieldValue(sun.security.x509.CertificateExtensions.class, "unparseableExtensions", extensions);

            if (unparseableExtensions == null || unparseableExtensions.isEmpty()) {
                log.trace("Certificate {} has no unparseable extensions.", certificate.getSubjectDN());
            } else {
                log.debug("Certificate {} has an unparseable extensions.", certificate.getSubjectDN());
                for (sun.security.x509.Extension extension : unparseableExtensions.values()) {
                    handleExtension(certificate, extension);
                }
            }
        }
    }

    /**
     * Verarbeitet eine einzelne {@link sun.security.x509.Extension}.
     *
     * @param certificate Das Zertificat aus dem die {@link sun.security.x509.Extension} kommt.
     * @param extension   Die {@link sun.security.x509.Extension} die verarbeitet werden soll.
     */
    private static void handleExtension(final sun.security.x509.X509CertImpl certificate, final sun.security.x509.Extension extension) {
        final Class<? extends sun.security.x509.Extension> extensionClass = extension.getClass();
        if ("sun.security.x509.UnparseableExtension".equals(extensionClass.getCanonicalName())) {
            final Throwable why = getFieldValue(extensionClass, "why", extension);
            if (why != null) {
                setFieldValue(extensionClass, "why", extension, null);
                log.error("Exception of certificate {} removed! It was:", certificate.getSubjectDN(), why);
            } else {
                log.warn("Exception of certificate {} is already set to null. It will be skipped!", certificate.getSubjectDN());
            }
        } else {
            log.warn(
                "The 'unparseableExtensions' of certificate {} should be a 'sun.security.x509.UnparseableExtension' but is '{}'. It will be skipped!",
                certificate.getSubjectDN(),
                extensionClass.getCanonicalName()
            );
        }
    }

    /**
     * Liefert den Wert eines statischen Feldes.
     *
     * @param clazz     Die Klasse in der das Feld definiert ist.
     * @param fieldName Der Name des Feldes.
     * @param <T>       Der Rückgabetyp.
     * @return Der Wert des Feldes.
     * @throws ReflectionException Wenn der Wert des Feldes nicht ausgelesen werden kann.
     * @see #getFieldValue(Class, String, Object)
     */
    private static <T> T getFieldValue(final Class<?> clazz, final String fieldName) {
        return getFieldValue(clazz, fieldName, null);
    }

    /**
     * Liefert den Wert eines Feldes via Reflection.
     *
     * @param clazz     Die Klasse in der das Feld definiert ist.
     * @param fieldName Der Name des Feldes.
     * @param instance  Die Instanz aus der der Wert des Feldes gelesen werden soll oder {@code null}, wenn das Feld {@code static} ist.
     * @param <T>       Der Rückgabetyp.
     * @return Der Wert des Feldes.
     * @throws ReflectionException Wenn der Wert des Feldes nicht ausgelesen werden kann.
     */
    @SuppressWarnings("unchecked")
    private static <T> T getFieldValue(final Class<?> clazz, final String fieldName, final Object instance) {
        try {
            final Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            return (T) field.get(instance);
        } catch (final ReflectiveOperationException e) {
            throw new ReflectionException("Cannot access field!", e);
        }
    }

    /**
     * Setzt ein Feld via Reflection.
     *
     * @param clazz     Die Klasse in der das Feld definiert ist.
     * @param fieldName Der Name des Feldes.
     * @param instance  Die Instanz in der das Feld gesetzt werden soll oder {@code null}, wenn das Feld {@code static} ist.
     * @param value     Der Wert der gesetzt werden soll.
     * @throws ReflectionException Wenn der Wert dem Feld nicht zugewiesen werden kann.
     */
    private static void setFieldValue(final Class<?> clazz, final String fieldName, final Object instance, final Object value) {
        try {
            final Field backtraceField = clazz.getDeclaredField(fieldName);
            backtraceField.setAccessible(true);
            backtraceField.set(instance, value);
        } catch (final ReflectiveOperationException e) {
            throw new ReflectionException("Cannot access field!", e);
        }
    }

    /**
     * Diese Exception wird bei Reflection-Fehlern geworfen.<br>
     * Unchecked, um die ohnehin unsauberen Reflection-Zugriffe nicht noch komplizierter zu gestalten.
     */
    private static class ReflectionException extends RuntimeException {
        /**
         * Konstruktor.
         *
         * @param message Die Fehlermeldung.
         * @param cause   Die Ursache.
         */
        ReflectionException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }

}
