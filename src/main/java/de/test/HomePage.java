package de.test;

import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The homepage for wicket. Just an example...
 */
@SuppressWarnings("WeakerAccess")
@Slf4j
public class HomePage extends WebPage {
    private static final long serialVersionUID = 1L;

    /**
     * The {@link SolrClient} instance (injected by spring).
     * @see de.test.spring.SolrJConfig
     */
    @SpringBean
    private SolrClient solrClient;

    /**
     * Constructor.
     *
     * @param parameters The Page parameter.
     * @see WebPage
     */
    public HomePage(final PageParameters parameters) {
        super(parameters);

        int status = -1;
        try {
            status = solrClient.ping().getStatus();
        } catch (final SolrServerException | IOException e) {
            log.error("Solr ping error!", e);
        }

        add(new Label("test", solrClient.getClass().getName() + " (ping-status: " + status + ")"));

    }
}
