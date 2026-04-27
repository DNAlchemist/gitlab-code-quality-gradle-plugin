package io.github.dnalchemist.gitlab.codequality;

import io.github.dnalchemist.gitlab.codequality.checkstyle.CheckstyleFindingProvider;
import io.github.dnalchemist.gitlab.codequality.spotbugs.SpotbugsFindingProvider;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;

public abstract class GenerateGitLabCodeQualityReportTask extends DefaultTask {

  @Input
  public abstract Property<Boolean> getSpotbugsEnabled();

  @Input
  public abstract Property<Boolean> getCheckstyleEnabled();

  @Internal
  public abstract RegularFileProperty getSpotbugsInputFile();

  @Internal
  public abstract RegularFileProperty getCheckstyleInputFile();

  @OutputFile
  public abstract RegularFileProperty getOutputFile();

  @TaskAction
  public void generate() {

    PluginLogger log = new PluginLogger(getLogger());

    File repositoryRoot = getRepositoryRootDir(getProject().getProjectDir(), log);

    List<String> compileSourceRoots = collectMainJavaSourceRoots();

    List<Finding> findings = new ArrayList<>();

    findings.addAll(executeProvider(
        new SpotbugsFindingProvider(compileSourceRoots, repositoryRoot, log),
        getSpotbugsEnabled().get(),
        getSpotbugsInputFile().getAsFile().get(),
        log
    ));

    findings.addAll(executeProvider(
        new CheckstyleFindingProvider(repositoryRoot),
        getCheckstyleEnabled().get(),
        getCheckstyleInputFile().getAsFile().get(),
        log
    ));

    File output = getOutputFile().getAsFile().get();
    output.getParentFile().mkdirs();

    try (FileOutputStream stream = new FileOutputStream(output)) {
      new ReportSerializer().write(findings, stream);
      log.info("GitLab code quality report for {} issue created: {}",
          findings.size(), output);
    } catch (IOException e) {
      throw new GradleException("Failed to write GitLab code quality report", e);
    }

  }

  private List<String> collectMainJavaSourceRoots() {
    JavaPluginExtension javaExtension =
        getProject().getExtensions().getByType(JavaPluginExtension.class);
    return javaExtension.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME)
        .getJava()
        .getSrcDirs()
        .stream()
        .filter(File::exists)
        .map(File::getAbsolutePath)
        .collect(Collectors.toList());
  }

  private static List<Finding> executeProvider(FindingProvider provider,
      boolean active, File file, PluginLogger log) {

    if (active) {

      if (file.canRead()) {

        try (InputStream stream = new FileInputStream(file)) {

          List<Finding> findings = provider.getFindings(stream);

          log.info("{} report with {} issues found: {}", provider.getName(),
              findings.size(), file);

          return findings;

        } catch (IOException e) {
          throw new GradleException("IO error while reading " + provider.getName() + " report", e);
        }

      } else {
        log.info("{} report not found: {}", provider.getName(), file);
      }

    } else {
      log.info("{} support disabled.", provider.getName());
    }

    return Collections.emptyList();


  }

  private static File getRepositoryRootDir(File initial, PluginLogger log) {

    File current = initial;

    do {

      if (new File(current, ".git").exists()) {
        log.debug("Detected git root directory: {}", current);
        return current;
      }

      current = current.getParentFile();

    } while (current != null);

    log.warn("Failed to locate git root directory. Paths will most likely be incorrect.");
    return initial;

  }

}
