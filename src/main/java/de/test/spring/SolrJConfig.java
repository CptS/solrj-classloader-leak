package de.test.spring;

import lombok.extern.slf4j.Slf4j;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The SolrJ configuration.
 */
@Configuration
@Slf4j
public class SolrJConfig {

    /**
     * The Solr URL.
     */
    private static final String SOLR_URL = "http://172.21.16.29:8984/solr/sf5_dev";

    /**
     * Factory method for the {@link SolrClient}.
     *
     * @return The {@link SolrClient} instance.
     */
    @Bean(destroyMethod = "close")
    public SolrClient solrClient() {
        log.trace("initialize HttpSolrClient ...");
        return new HttpSolrClient(SOLR_URL);
    }

}
