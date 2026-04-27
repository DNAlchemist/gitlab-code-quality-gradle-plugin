package io.github.dnalchemist.gitlab.codequality;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaBasePlugin;

public class GitLabCodeQualityPlugin implements Plugin<Project> {

  public static final String TASK_NAME = "generateGitLabCodeQualityReport";

  @Override
  public void apply(Project project) {
    project.getPlugins().withId("java", p -> configureForJava(project));
  }

  private void configureForJava(Project project) {
    project.getTasks().register(TASK_NAME, GenerateGitLabCodeQualityReportTask.class, task -> {
      task.setGroup("verification");
      task.setDescription(
          "Aggregates SpotBugs and Checkstyle XML into GitLab code quality JSON.");
      task.getSpotbugsEnabled().convention(true);
      task.getCheckstyleEnabled().convention(true);
      task.getSpotbugsInputFile().convention(
          project.getLayout().getBuildDirectory().file("spotbugsXml.xml"));
      task.getCheckstyleInputFile().convention(
          project.getLayout().getBuildDirectory().file("checkstyle-result.xml"));
      task.getOutputFile().convention(
          project.getLayout().getBuildDirectory().file("gl-code-quality-report.json"));
    });

    project.getTasks().named(JavaBasePlugin.CHECK_TASK_NAME).configure(t ->
        t.dependsOn(project.getTasks().named(TASK_NAME)));
  }

}
