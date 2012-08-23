package eu.schnuckelig.gradle.plugins.jaxws

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSet

/**
 * A plugin for adding JAX-WS support to {@link JavaPlugin java projects}
 *
 * @author Thorsten Klein
 */
public class JaxWSPlugin implements Plugin<Project> {
	private static final String GENERATE_GROUP = 'generate'

	void apply(Project project) {
		project.plugins.apply(JavaPlugin)

		project.configurations.add('jaxws') {
			visible = false
			transitive = true
			description = "The JAX-WS libraries to be used for this project."
		}

		project.configurations.compile {
			extendsFrom project.configurations.jaxws
		}

		project.extensions.create("jaxws", JaxWSPluginExtension)

		project.dependencies {
			jaxws ('com.sun.xml.ws:jaxws-tools:2.2.7-promoted-b73') {
				exclude group: 'org.glassfish.ha', module: 'ha-api'
			}
		}
		
		project.convention.plugins.java.sourceSets.main { 
			SourceSet sourceSet = it
			File generatedSrcDir = generatedJavaDirFor(project, sourceSet);
			java { srcDir  generatedSrcDir }
			
			Task jaxws = createJaxWSTaskFor(sourceSet, project)
			project.tasks[sourceSet.compileJavaTaskName].dependsOn(jaxws)
		}
	}

	private setupJaxWSFor(SourceSet sourceSet, Project project) {
		insertJaxWSSourceDirectorySetInto(sourceSet, project)
		Task jaxws = createJaxWSTaskFor(sourceSet, project)
		project.tasks[sourceSet.compileJavaTaskName].dependsOn(jaxws)
	}

	private Task createJaxWSTaskFor(SourceSet sourceSet, Project project) {
		def jaxWSTask = project.tasks.add(taskName(sourceSet), JaxWSTask)

		jaxWSTask.group = GENERATE_GROUP
		jaxWSTask.description = "Generates code from the WSDL."
		jaxWSTask.outputDirectory = generatedJavaDirFor(project, sourceSet)
		//jaxWSTask.conventionMapping.defaultSource = { sourceSet.jaxws }
		jaxWSTask.conventionMapping.jaxwsClasspath = {
			def jaxwsClassPath = project.configurations.jaxws.copy()
			jaxwsClassPath.transitive = true
			jaxwsClassPath
		}

		jaxWSTask
	}

	private File generatedJavaDirFor(Project project, SourceSet sourceSet) {
		def defaultOutputDir = "${project.buildDir}/generated-src/jaxws/${sourceSet.name}"; 
		
		if (project.jaxws.sourceDir) {
			return project.file(project.jaxws.sourceDir)
		}
		
		project.file(defaultOutputDir)
	}

	private String taskName(SourceSet sourceSet) {
		return sourceSet.getTaskName('wsimport', '')
	}
}