/*
 * The MIT License
 *
 * Copyright (C) 2010-2011 by Anthony Robinson
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package jenkins.plugins.publish_over_ftp.descriptor;

import hudson.Util;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.util.CopyOnWriteList;
import hudson.util.FormValidation;
import hudson.util.VersionNumber;
import jenkins.plugins.publish_over.BPBuildInfo;
import jenkins.plugins.publish_over.BPInstanceConfig;
import jenkins.plugins.publish_over.BPPlugin;
import jenkins.plugins.publish_over.BPPluginDescriptor;
import jenkins.plugins.publish_over_ftp.BapFtpHostConfiguration;
import jenkins.plugins.publish_over_ftp.BapFtpPublisherPlugin;
import jenkins.plugins.publish_over_ftp.Messages;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import java.util.List;

@SuppressWarnings("PMD.TooManyMethods")
public class BapFtpPublisherPluginDescriptor extends BuildStepDescriptor<Publisher> {

    /** null - prevent complaints from xstream */
    private BPPluginDescriptor.BPDescriptorMessages msg;
    /** null - prevent complaints from xstream */
    private Class hostConfigClass;
    private final CopyOnWriteList<BapFtpHostConfiguration> hostConfigurations = new CopyOnWriteList<BapFtpHostConfiguration>();

    public BapFtpPublisherPluginDescriptor() {
        super(BapFtpPublisherPlugin.class);
        load();
    }

    public String getDisplayName() {
        return Messages.descriptor_displayName();
    }

    public boolean isApplicable(final Class<? extends AbstractProject> aClass) {
        return !BPPlugin.PROMOTION_JOB_TYPE.equals(aClass.getCanonicalName());
    }

    public List<BapFtpHostConfiguration> getHostConfigurations() {
        return hostConfigurations.getView();
    }

    public BapFtpHostConfiguration getConfiguration(final String name) {
        for (BapFtpHostConfiguration configuration : hostConfigurations) {
            if (configuration.getName().equals(name)) {
                return configuration;
            }
        }
        return null;
    }

    public boolean configure(final StaplerRequest request, final JSONObject formData) {
        hostConfigurations.replaceBy(request.bindJSONToList(BapFtpHostConfiguration.class, formData.get("instance")));
        save();
        return true;
    }

    public boolean canSetMasterNodeName() {
        return Hudson.getVersion().isOlderThan(new VersionNumber(BPInstanceConfig.MASTER_GETS_NODE_NAME_IN_VERSION));
    }

    public BapFtpPublisherDescriptor getPublisherDescriptor() {
        return Hudson.getInstance().getDescriptorByType(BapFtpPublisherDescriptor.class);
    }

    public BapFtpHostConfigurationDescriptor getHostConfigurationDescriptor() {
        return Hudson.getInstance().getDescriptorByType(BapFtpHostConfigurationDescriptor.class);
    }

    public FormValidation doTestConnection(final StaplerRequest request, final StaplerResponse response) {
        final BapFtpHostConfiguration hostConfig = request.bindParameters(BapFtpHostConfiguration.class, "");
        final BPBuildInfo buildInfo = createDummyBuildInfo(request);
        try {
            hostConfig.createClient(buildInfo).disconnect();
            return FormValidation.ok(Messages.descriptor_testConnection_ok());
        } catch (Exception e) {
            return FormValidation.errorWithMarkup("<p>"
                    + Messages.descriptor_testConnection_error() + "</p><p><pre>"
                    + Util.escape(e.getClass().getCanonicalName() + ": " + e.getLocalizedMessage())
                    + "</pre></p>");
        }
    }

    private BPBuildInfo createDummyBuildInfo(final StaplerRequest request) {
        return new BPBuildInfo(
            TaskListener.NULL,
            "",
            Hudson.getInstance().getRootPath(),
            null,
            null
        );
    }
    
    public Object readResolve() {
        // nuke the legacy config
        msg = null;
        hostConfigClass = null;
        return this;
    }

}