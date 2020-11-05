/**
 * (C) Copyright IBM Corporation 2014, 2020.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.openliberty.tools.gradle.tasks

import java.util.Set

import org.gradle.api.artifacts.ResolveException
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

import io.openliberty.tools.common.plugins.util.InstallFeatureUtil
import io.openliberty.tools.common.plugins.util.DevUtil
import io.openliberty.tools.common.plugins.util.PluginExecutionException
import io.openliberty.tools.common.plugins.util.PluginScenarioException

class InstallFeatureTask extends AbstractFeatureTask {

    InstallFeatureTask() {
        configure({
            description 'Install a new feature to the Liberty server'
            group 'Liberty'
        })
    }

    @TaskAction
    void installFeature() {
        def propertiesList = InstallFeatureUtil.loadProperties(getInstallDir(project))
        def openLibertyVersion = InstallFeatureUtil.getOpenLibertyVersion(propertiesList)

        boolean skipBetaInstallFeatureWarning = Boolean.parseBoolean(System.getProperty(DevUtil.SKIP_BETA_INSTALL_WARNING))
        if (InstallFeatureUtil.isOpenLibertyBetaVersion(openLibertyVersion)) {
            if (!skipBetaInstallFeatureWarning) {
                logger.warn("Features that are not included with the beta runtime cannot be installed. Features that are included with the beta runtime can be enabled by adding them to your server.xml file.")
            }
            return // do not install features if the runtime is a beta version
        }
    
        def pluginListedEsas = getPluginListedFeatures(true)
        InstallFeatureUtil util = getInstallFeatureUtil(pluginListedEsas, propertiesList, openLibertyVersion)

        // if getInstallFeatureUtil failed to retrieve an InstallFeatureUtil instance for util, then features are installed via ant
        if(installFeaturesFromAnt) {
            installFeatureFromAnt();
        }
        else {
            Set<String> featuresToInstall = getInstalledFeatures()
            util.installFeatures(server.features.acceptLicense, new ArrayList<String>(featuresToInstall))
        }
    }

    private def buildAntParams() {
        def params = buildLibertyMap(project)
        params.put('acceptLicense', server.features.acceptLicense)
        if (server.features.name != null) {
            params.put('name', server.features.name.join(","))
        }
        if (server.features.to != null) {
            params.put('to', server.features.to)
        }
        if (server.features.from != null) {
            params.put('from', server.features.from)
        }
        params.remove('timeout')
        return params
    }

    void installFeatureFromAnt() {
        def params = buildAntParams()
        project.ant.taskdef(name: 'installFeature',
                            classname: 'io.openliberty.tools.ant.InstallFeatureTask',
                            classpath: project.buildscript.configurations.classpath.asPath)
        project.ant.installFeature(params)
    }
}
