package com.haulmont.components.search.web.context;

import com.haulmont.components.search.context.SearchContext;
import com.haulmont.cuba.core.sys.AppContext;
import com.haulmont.cuba.core.sys.SecurityContext;
import com.haulmont.cuba.security.global.UserSession;
import com.vaadin.server.ClientConnector;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.UI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class WebSearchContext implements SearchContext {

    VaadinSession vaadinSession = VaadinSession.getCurrent();
    SecurityContext securityContext = AppContext.getSecurityContextNN();
    Map<String, Object> params = new HashMap<>();
    static Logger logger = LoggerFactory.getLogger(WebSearchContext.class);

    public WebSearchContext() {}

    public WebSearchContext(Map<String, Object> params) {
        this.params = params;
    }

    @Override
    public UserSession session() {
        return vaadinSession.getAttribute(UserSession.class);
    }

    @Override
    public Map<String, Object> params() {
        return Collections.unmodifiableMap(params);
    }

    @Override
    public void applyUICallback(Runnable callback) {
        VaadinSession oldSession = VaadinSession.getCurrent();
        SecurityContext oldContext = AppContext.getSecurityContext();
        try {
            VaadinSession.setCurrent(vaadinSession);
            AppContext.setSecurityContext(securityContext);

            vaadinSession.getUIs().stream()
                    .filter(UI::isConnectorEnabled)
                    .forEach(ui-> {
                        if (ui.isAttached()) {
                            ui.access(callback);
                        } else {
                            ui.addAttachListener(new ClientConnector.AttachListener() {
                                @Override
                                public void attach(ClientConnector.AttachEvent event) {
                                    ui.removeAttachListener(this);
                                    ui.access(callback);
                                }
                            });
                        }
                    });

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            VaadinSession.setCurrent(oldSession);
            AppContext.setSecurityContext(oldContext);
        }
    }

    @Override
    public SearchContext withParams(Map<String, Object> basicParams) {
        Map<String, Object> params = new HashMap<>(basicParams);
        basicParams.putAll(this.params);
        return new WebSearchContext(params);
    }

    @Override
    public SearchContext extendParams(Map<String, Object> params) {
        this.params.putAll(params);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WebSearchContext that = (WebSearchContext) o;

        return securityContext.getSessionId() != null ?
                securityContext.getSessionId().equals(that.securityContext.getSessionId()) :
                that.securityContext.getSessionId() == null;
    }

    @Override
    public int hashCode() {
        return vaadinSession != null ? securityContext.getSessionId().hashCode() : 0;
    }
}