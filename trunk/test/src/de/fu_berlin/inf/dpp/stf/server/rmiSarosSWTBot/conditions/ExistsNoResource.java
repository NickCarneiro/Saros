package de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.conditions;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.swtbot.swt.finder.waits.DefaultCondition;

public class ExistsNoResource extends DefaultCondition {

    String resourcePath;

    ExistsNoResource(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    public String getFailureMessage() {

        return null;
    }

    public boolean test() throws Exception {
        IPath path = new Path(resourcePath);
        IResource resource = ResourcesPlugin.getWorkspace().getRoot()
            .findMember(path);
        if (resource == null)
            return true;
        return false;
    }
}
