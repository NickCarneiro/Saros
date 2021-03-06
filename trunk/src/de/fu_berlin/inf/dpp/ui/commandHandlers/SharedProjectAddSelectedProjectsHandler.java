package de.fu_berlin.inf.dpp.ui.commandHandlers;

import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.picocontainer.annotations.Inject;

import de.fu_berlin.inf.dpp.SarosPluginContext;
import de.fu_berlin.inf.dpp.project.ISarosSession;
import de.fu_berlin.inf.dpp.project.SarosSessionManager;
import de.fu_berlin.inf.dpp.ui.util.CollaborationUtils;
import de.fu_berlin.inf.dpp.ui.util.selection.retriever.SelectionRetrieverFactory;

/**
 * Handles the addition of selected {@link IProject}s to the running
 * {@link ISarosSession}.
 */
public class SharedProjectAddSelectedProjectsHandler extends AbstractHandler {

    @Inject
    protected SarosSessionManager sarosSessionManager;

    public Object execute(ExecutionEvent event) throws ExecutionException {
        if (sarosSessionManager == null)
            SarosPluginContext.initComponent(this);

        List<IProject> projects = SelectionRetrieverFactory
            .getSelectionRetriever(IProject.class).getSelection();
        CollaborationUtils.addProjectsToSarosSession(sarosSessionManager,
            projects);
        return null;
    }

}
