package edu.stanford.bmir.protege.web.client.ui.portlet;

import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.IsWidget;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 10/02/16
 */
public interface PortletUi extends IsWidget {

    void setToolbarVisible(boolean visible);

    void addPortletAction(PortletAction action);

    AcceptsOneWidget getContentHolder();

}
