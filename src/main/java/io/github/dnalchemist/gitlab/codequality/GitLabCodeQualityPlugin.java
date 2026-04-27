package io.github.dnalchemist.gitlab.codequality;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Callable;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;

public class GitLabCodeQualityPlugin implements Plugin<Project> {

  public static final String TASK_NAME = "generateGitLabCodeQualityReport";
  public static final String EXTENSION_NAME = "gitlabCodeQuality";

  @Override
  public void apply(Project project) {
    GitLabCodeQualityExtension extension = project.getExtensions()
        .create(EXTENSION_NAME, GitLabCodeQualityExtension.class);
    extension.getWireIntoCheck().convention(true);

    TaskProvider<GenerateGitLabCodeQualityReportTask> reportTask = project.getTasks()
        .register(TASK_NAME, GenerateGitLabCodeQualityReportTask.class, task -> {
          task.setGroup("verification");
          task.setDescription(
              "Aggregates SpotBugs and Checkstyle XML into GitLab code quality JSON.");
          task.getSpotbugsEnabled().convention(true);
          task.getCheckstyleEnabled().convention(true);
          task.getSpotbugsInputFile().convention(
              project.getLayout().getBuildDirectory().file("reports/spotbugs/main.xml"));
          task.getCheckstyleInputFile().convention(
              project.getLayout().getBuildDirectory().file("reports/checkstyle/main.xml"));
          task.getOutputFile().convention(
              project.getLayout().getBuildDirectory().file("gl-code-quality-report.json"));

          task.getProjectDirectory().convention(project.getLayout().getProjectDirectory());

          // Lazy: source sets registered later in the script must still be picked up.
          task.getSourceRoots().from((Callable<Collection<File>>) () ->
              defaultSourceRoots(project));
        });

    project.getPlugins().withType(JavaBasePlugin.class, plugin ->
        project.getTasks().named(JavaBasePlugin.CHECK_TASK_NAME).configure(checkTask ->
            checkTask.dependsOn((Callable<Object>) () ->
                extension.getWireIntoCheck().getOrElse(true)
                    ? reportTask
                    : Collections.emptyList())));
  }

  private static Collection<File> defaultSourceRoots(Project project) {
    JavaPluginExtension javaExtension =
        project.getExtensions().findByType(JavaPluginExtension.class);
    if (javaExtension == null) {
      return Collections.emptyList();
    }
    SourceSet main = javaExtension.getSourceSets().findByName(SourceSet.MAIN_SOURCE_SET_NAME);
    if (main == null) {
      return Collections.emptyList();
    }
    return main.getJava().getSrcDirs();
  }

}
