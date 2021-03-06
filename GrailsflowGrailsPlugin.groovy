
import grails.util.BuildSettingsHolder
import com.jcatalog.grailsflow.messagebundle.i18n.SpringI18nMessageBundleProvider
import com.jcatalog.grailsflow.messagebundle.DefaultMessageBundleProvider

import com.jcatalog.grailsflow.model.process.FlowStatus

import org.springframework.orm.hibernate3.HibernateTemplate
import com.jcatalog.grailsflow.grails.ListFactoryBean

import com.jcatalog.grailsflow.search.DefaultSearchParameter
import com.jcatalog.grailsflow.search.DateSearchParameter
import com.jcatalog.grailsflow.search.DefaultDisplayParameter
import org.codehaus.groovy.grails.plugins.GrailsPluginUtils
import grails.util.Holders

import com.jcatalog.grailsflow.status.NodeStatusEnum
import com.jcatalog.grailsflow.status.ProcessStatusEnum
import com.jcatalog.grailsflow.scheduling.triggers.ConfigurableSimpleTrigger

class GrailsflowGrailsPlugin {
    def version = '1.4'
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "2.0 > *"
    def dependsOn = [quartz: "0.4 > *"]

    def doWithSpring = {
      def grailsFlowCoreConfig = application.config.grailsFlowCoreConfig

      ConfigurableSimpleTrigger.metaClass.'static'.getGrailsApplication = { -> application }

      def buildSettings = BuildSettingsHolder.settings
      String fileSystemName = Holders.pluginManager.getGrailsPlugin("grailsflow").getFileSystemName()
      String i18nDir = "WEB-INF/plugins/${fileSystemName}/grails-app/i18n/"

      def bundles = []
      def extraBundles = application.config.grailsflow.i18n.locations
      if (extraBundles) bundles.addAll(extraBundles)

      if (!application.warDeployed){
        def pluginDir = GrailsPluginUtils.getPluginDirForName("grailsflow")
        if (pluginDir != null) {
          i18nDir = "${pluginDir.URL.toExternalForm()}/grails-app/i18n/"
        }
      }
      bundles << i18nDir

      grailsflowMessageBundleProvider(DefaultMessageBundleProvider) {
        i18nMessageBundleProvider = {SpringI18nMessageBundleProvider provider ->
            bundlesLocations = bundles*.toString()
            cacheSeconds = application.warDeployed ? '-1' : '0'
        }
      }

      // format patterns
      datePatterns(java.util.HashMap, ['en':'MM/dd/yyyy', 'de':'dd.MM.yyyy'])
      dateTimePatterns(java.util.HashMap, ['en':'MM/dd/yy HH:mm', 'de':'dd.MM.yy HH:mm'])
      numberPatterns(java.util.HashMap, ['en':'0.00', 'de':'0.00'])
      decimalSeparators(java.util.HashMap, ['en':'.', 'de':','])
      defaultLocale(java.lang.String, "en")

      // default workarea configuration
      workareaPathProvider(com.jcatalog.grailsflow.workarea.GrailsflowPathProvider) {
        resourcesPath = "workarea"
        resourcesUrl = "/workarea"
      }

      // default scripts configuration
      scriptsProvider(com.jcatalog.grailsflow.workarea.GrailsflowWorkareaScriptsProvider) {
        workareaPathProvider = ref('workareaPathProvider')
      }
      appExternalID(java.lang.String, "grailsflow")
      processesPath(java.lang.String, "processes")
      actionsPath(java.lang.String, "actions")
      documentsPath(java.lang.String, "documents")
      callbacksPath(java.lang.String, "callbacks")

      // default UI configuration
      maxResultSize(java.lang.Integer, "20")

      // default configuration for threads quantity (threads that can be running concurrently)
      maxThreadsQuantity(java.lang.Integer, "10")

      // default configuration for quantity of restricted processes that used in SQL
      // 'in' clause
      maxRestrictedProcesses(java.lang.Integer, "2000")

      // default security configuration
      securityHelper(com.jcatalog.grailsflow.security.GrailsflowSecurityHelper)
      usersProvider(com.jcatalog.grailsflow.security.GrailsflowUsersProvider)
      rolesProvider(com.jcatalog.grailsflow.security.GrailsflowRolesProvider)
      groupsProvider(com.jcatalog.grailsflow.security.GrailsflowGroupsProvider)

      // default asynchronous configuration
      clientExecutor(com.jcatalog.grailsflow.client.GrailsflowHTTPClientExecutor)

      // Grailsflow engine configuration
      actionFactory(com.jcatalog.grailsflow.actions.GrailsflowActionFactory) {
           actionsPath = ref('actionsPath')
           scriptsProvider = ref('scriptsProvider')
      }

      worklistProvider(com.jcatalog.grailsflow.worklist.WorklistProvider) {
        appExternalID = ref('appExternalID')
      }
      processProvider(com.jcatalog.grailsflow.process.ProcessProvider) {
        appExternalID = ref('appExternalID')
      }

      processScriptProvider(com.jcatalog.grailsflow.process.script.GrailsflowProcessScriptProvider) {
        scriptsProvider = ref('scriptsProvider')
        processesPath = ref('processesPath')
      }

      cacheManager(com.jcatalog.grailsflow.cache.SimpleCacheManager)

      processFactory(com.jcatalog.grailsflow.engine.GrailsflowCachingProcessFactory) {
        processScriptProvider = ref('processScriptProvider')
        cacheManager = ref('cacheManager')
      }

      nodeExecutor(com.jcatalog.grailsflow.engine.execution.NodeExecutor) {
        actionFactory = ref('actionFactory')
      }

      // validation
      processDefValidator(com.jcatalog.grailsflow.validation.DefaultProcessDefValidator)
      processClassValidator(com.jcatalog.grailsflow.validation.DefaultProcessClassValidator)

      bean(org.springframework.web.multipart.commons.CommonsMultipartResolver) {
          maxUploadSize(1000000)
      }

      // Allows worklist filtering
      // NOTE: if set to TRUE - it can cause problems with performance
      // in case of large amount of items.
      isWorklistFilterAvailable(java.lang.Boolean, Boolean.TRUE)

      // processListColumns UI
      processTypeSearchProperty(DefaultSearchParameter) {
        name = "type"
        searchTemplate = "/processList/search/type"
      }
      processStatusSearchProperty(DefaultSearchParameter) {
        name = "status"
        searchTemplate = "/processList/search/status"
      }
      startedFromSearchProperty(DateSearchParameter) {
        name = "startedFrom"
        searchTemplate = "/processList/search/date"
      }
      userSearchProperty(DefaultSearchParameter) {
        name = "startUser"
      }
      finishedFromSearchProperty(DateSearchParameter) {
        name = "finishedFrom"
        searchTemplate = "/processList/search/date"
      }
      processListSearchParameters(ListFactoryBean) {
        items = [ ref('processTypeSearchProperty'),
                ref('processStatusSearchProperty'),
                ref('startedFromSearchProperty'), ref('userSearchProperty'),
                ref('finishedFromSearchProperty')
                ]
      }


      processTypeDisplayProperty(DefaultDisplayParameter) {
        name = "type"
        displayTemplate = "/processList/display/type"
      }
      processStatusDisplayProperty(DefaultDisplayParameter) {
        name = "status.statusID"
        displayProperty = "status"
        displayTemplate = "/processList/display/status"
      }
      createdOnDisplayProperty(DefaultDisplayParameter) {
        name = "createdOn"
        displayProperty = "createdOn"
        displayTemplate = "/processList/display/dateTime"
      }
      createdByDisplayProperty(DefaultDisplayParameter) {
        name = "createdBy"
        displayProperty = "createdBy"
      }
      activeNodesDisplayProperty(DefaultDisplayParameter) {
        name = "activeNodes"
        sortable = false
        displayTemplate = "/processList/display/activeNodes"
      }
      finishedOnDisplayProperty(DefaultDisplayParameter) {
        name = "finishedOn"
        displayProperty = "finishedOn"
        displayTemplate = "/processList/display/dateTime"
      }
      processListDisplayParameters(ListFactoryBean) {
        items = [ ref('processTypeDisplayProperty'),
                ref('processStatusDisplayProperty'),
                ref('createdOnDisplayProperty'), ref('createdByDisplayProperty'),
                ref("activeNodesDisplayProperty"),
                ref('finishedOnDisplayProperty')
                ]
      }

      nodeWorklistColumn(DefaultDisplayParameter) {
        name = "nodeID"
        displayTemplate = "/worklist/display/node"
      }
      externalURLWorklistColumn(DefaultDisplayParameter) {
        name = "externalUrl"
        sortable = false
        displayTemplate = "/worklist/display/externalUrl"
      }
      processTypeWorklistColumn(DefaultDisplayParameter) {
        name = "processType"
        displayProperty = "process"
        sortable = false
        displayTemplate = "/worklist/display/processType"
      }
      callerWorklistColumn(DefaultDisplayParameter) {
        name = "caller"
        displayProperty = "caller"
      }
      startedOnWorklistColumn(DefaultDisplayParameter) {
        name = "startedOn"
        displayProperty = "startedOn"
        displayTemplate = "/worklist/display/dateTime"
      }
      dueOnWorklistColumn(DefaultDisplayParameter) {
        name = "dueOn"
        displayProperty = "dueOn"
        displayTemplate = "/worklist/display/dateTime"
      }
      worklistDisplayParameters(ListFactoryBean) {
        items = [ ref('nodeWorklistColumn'),
                ref('externalURLWorklistColumn'),
                ref('processTypeWorklistColumn'),
                ref('callerWorklistColumn'),
                ref('startedOnWorklistColumn'),
                ref('dueOnWorklistColumn')
                ]
      }

      // Extensions
      eventEmailProcessor(com.jcatalog.grailsflow.extension.email.GrailsflowEventEmailProcessor) {
        processManagerService = ref('processManagerService')
      }

    }

    def doWithApplicationContext = { applicationContext ->

        def sessionFactory = applicationContext.sessionFactory
        def hTemplate  = new HibernateTemplate(sessionFactory)

        FlowStatus.withTransaction {
            // Insert process statuses into DB if they're missing
            ProcessStatusEnum.values()?.each() {
                def statusID = it.value()
                if (!FlowStatus.findByStatusID(statusID)) {
                    def status = new FlowStatus(statusID: statusID, description: statusID, isFinal: it.isFinal())
                    hTemplate.save(status)
                }
            }

            // Insert node statuses into DB if they're missing
            NodeStatusEnum.values()?.each() {
                def statusID = it.value()
                if (!FlowStatus.findByStatusID(statusID)) {
                    def status = new FlowStatus(statusID: statusID, description: statusID, isFinal: it.isFinal())
                    hTemplate.save(status)
                }
            }
        }

    }

    def doWithWebDescriptor = { xml ->
        // TODO Implement additions to web.xml (optional)
    }

    def doWithDynamicMethods = { ctx ->
        // TODO Implement registering dynamic methods to classes (optional)
    }

    def onChange = { event ->
        // TODO Implement code that is executed when this class plugin class is changed
        // the event contains: event.application and event.applicationContext objects
    }

    def onApplicationChange = { event ->
        // TODO Implement code that is executed when any class in a GrailsApplication changes
        // the event contain: event.source, event.application and event.applicationContext objects
    }
}

