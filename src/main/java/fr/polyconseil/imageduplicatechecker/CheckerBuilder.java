package fr.polyconseil.imageduplicatechecker;
import java.io.File;
import java.io.IOException;

import javax.servlet.ServletException;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Builder;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;

/**
 * Sample {@link Builder}.
 *
 * <p>
 * When the user configures the project and enables this builder,
 * {@link DescriptorImpl#newInstance(StaplerRequest)} is invoked
 * and a new {@link CheckerBuilder} is created. The created
 * instance is persisted to the project configuration XML by using
 * XStream, so this allows you to use instance fields (like {@link #excludedFolders})
 * to remember the configuration.
 *
 * <p>
 * When a build is performed, the {@link #perform} method will be invoked.
 *
 * @author Weiwei ZHANG
 */
public class CheckerBuilder extends Publisher implements SimpleBuildStep {

    private final String excludedFolders;
    private final String allowedFileExtensions;
    private final String sourcePathPattern;

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public CheckerBuilder(String excludedFolders, String allowedFileExtensions, String sourcePathPattern) {
        this.excludedFolders = excludedFolders;
        this.allowedFileExtensions = allowedFileExtensions;
        this.sourcePathPattern = sourcePathPattern;
    }


    public String getExcludedFolders() {
        return excludedFolders;
    }

    public String getAllowedFileExtensions() {
        return allowedFileExtensions;
    }
    
    public String getSourcePathPattern() {
        return sourcePathPattern;
    }

    @Override
    public void perform(Run<?,?> build, FilePath workspace, Launcher launcher, TaskListener listener) {
        FindDuplicates.execute(this.getExcludedFolders(), this.getAllowedFileExtensions(), this.getSourcePathPattern(), workspace, listener);
    }

    // Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public BuildStepDescriptor<Publisher> getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        // TODO(wzhang): remove global config!
        private String addPath;

        public DescriptorImpl() {
            load();
        }

        /**
         * Performs on-the-fly validation of the form field 'name'.
         *
         * @param excludedFolders
         *      This parameter receives the value that the user has typed.
         * @param allowedFileExtensions
         *      This parameter receives the value that the user has typed.
         * @param sourcePathPattern
         *      This parameter receives the value that the user has typed.
         * @return
         *      Indicates the outcome of the validation. This is sent to the browser.
         *      <p>
         *      Note that returning {@link FormValidation#error(String)} does not
         *      prevent the form from being saved. It just means that a message
         */
        public FormValidation doCheckName(@QueryParameter String excludedFolders, @QueryParameter String allowedFileExtensions, @QueryParameter String sourcePathPattern)
                throws IOException, ServletException {
            if (excludedFolders.isEmpty() || allowedFileExtensions.isEmpty())
                return FormValidation.error("Please supply a path to directory to find duplicate files in.");
            if (excludedFolders.length() < 2 || allowedFileExtensions.length() < 2)
                return FormValidation.warning("Isn't the folder path too short?");
            File excludedFoldersFile = new File(excludedFolders);
            File allowedFileExtensionsFile = new File(allowedFileExtensions);
            if (!excludedFoldersFile.isDirectory() || !allowedFileExtensionsFile.isDirectory()) {
                return FormValidation.error("Supplied directory (" + excludedFolders  + ") does not exist.");
            }
            if (sourcePathPattern.isEmpty())
                return FormValidation.error("Please define a folder/file to write images duplacate report.");
            return FormValidation.ok();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "Check duplicate images";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            // To persist global configuration information,
            // set that to properties and call save().
            addPath = nullify(formData.getString("addPath"));
            // ^Can also use req.bindJSON(this, formData);
            //  (easier when there are many fields; need set* methods for this, like setUseFrench)
            save();
            return super.configure(req,formData);
        }

        private String nullify(String v) {
            if(v!=null && v.length()==0)    v=null;
            return v;
        }

        /**
         * This method returns true if the global configuration says we should speak French.
         *
         * The method name is bit awkward because global.jelly calls this method to determine
         * the initial state of the checkbox by the naming convention.
         */
        public void setAddPath(String addPath) {
            this.addPath = addPath;
        }
        public String getAddPath() {
            return addPath;
        }
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }
}
