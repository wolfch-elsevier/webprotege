package edu.stanford.bmir.protege.web.client.actionbar.project;

import com.google.common.base.Optional;
import com.google.web.bindery.event.shared.EventBus;
import edu.stanford.bmir.protege.web.client.Application;
import edu.stanford.bmir.protege.web.client.dispatch.DispatchServiceManager;
import edu.stanford.bmir.protege.web.client.projectsettings.ProjectSettingsDialogController;
import edu.stanford.bmir.protege.web.client.projectsettings.ProjectSettingsPresenter;
import edu.stanford.bmir.protege.web.client.projectsettings.ProjectSettingsViewImpl;
import edu.stanford.bmir.protege.web.client.ui.library.dlg.WebProtegeDialog;
import edu.stanford.bmir.protege.web.shared.event.EventBusManager;
import edu.stanford.bmir.protege.web.shared.project.ProjectId;

/**
 * Author: Matthew Horridge<br>
 * Stanford University<br>
 * Bio-Medical Informatics Research Group<br>
 * Date: 27/08/2013
 */
public class ShowProjectDetailsHandlerImpl implements ShowProjectDetailsHandler {

    private final EventBus eventBus;

    private final DispatchServiceManager dispatchServiceManager;

    public ShowProjectDetailsHandlerImpl(EventBus eventBus, DispatchServiceManager dispatchServiceManager) {
        this.eventBus = eventBus;
        this.dispatchServiceManager = dispatchServiceManager;
    }

    @Override
    public void handleShowProjectDetails() {
        Optional<ProjectId> projectId = Application.get().getActiveProject();
        if(!projectId.isPresent()) {
            return;
        }
        ProjectSettingsPresenter presenter = new ProjectSettingsPresenter(
                new ProjectSettingsViewImpl(),
                eventBus,
                dispatchServiceManager);
        presenter.showDialog(projectId.get());
    }
}
