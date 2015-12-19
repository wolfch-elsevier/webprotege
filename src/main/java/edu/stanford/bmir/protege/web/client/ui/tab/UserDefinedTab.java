package edu.stanford.bmir.protege.web.client.ui.tab;

import com.google.web.bindery.event.shared.EventBus;
import edu.stanford.bmir.protege.web.client.dispatch.DispatchServiceManager;
import edu.stanford.bmir.protege.web.client.project.Project;
import edu.stanford.bmir.protege.web.client.rpc.data.layout.TabConfiguration;
import edu.stanford.bmir.protege.web.client.ui.portlet.EntityPortlet;
import edu.stanford.bmir.protege.web.shared.selection.SelectionModel;

/**
 * A user defined tab. The user can compose her own tab and fill it
 * with any of the available portlets.
 * The configuration of this tab is stored in theproject configuration,
 * so that it can be later retrieved when the user logs back in.
 * 
 * @author Tania Tudorache <tudorache@stanford.edu>
 *
 */
public class UserDefinedTab extends AbstractTab {

	public UserDefinedTab(SelectionModel selectionModel, EventBus eventBus, DispatchServiceManager dispatchServiceManager, Project project) {
		super(selectionModel, eventBus, dispatchServiceManager, project);
	}

	@Override
	public void setup() {	
		super.setup();
		TabConfiguration tabConfiguration = getTabConfiguration();
		if (tabConfiguration == null) { return; }
		EntityPortlet controllingPortlet = getControllingPortlet();
		if (controllingPortlet == null) { //TODO: just a fix until we can set from the UI the controlling portlet
			try {
				String firstPortlet = tabConfiguration.getColumns().iterator().next().getPortlets().iterator().next().getName();
				setControllingPortlet(getPortletByClassName(firstPortlet));
			} catch (Exception e) { } //FIXME
		}
	}
}
