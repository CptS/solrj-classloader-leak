package de.test;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.spring.injection.annot.SpringComponentInjector;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * The {@linkplain WebApplication Wicket Application} class.
 */
public class WicketApplication extends WebApplication {

    @Override
    public Class<? extends WebPage> getHomePage() {
        return HomePage.class;
    }

    @Override
    public void init() {
        super.init();

        // initialize spring:
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.scan("de.test.spring");
        ctx.refresh();

        // attach spring to wicket:
        getComponentInstantiationListeners().add(new SpringComponentInjector(this, ctx));
    }
}
