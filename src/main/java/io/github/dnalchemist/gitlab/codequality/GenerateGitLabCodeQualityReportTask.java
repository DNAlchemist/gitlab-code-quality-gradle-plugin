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
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputFile;
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

  /**
   * Source directories used to resolve {@code <SourceLine sourcepath="…"/>} entries from
   * SpotBugs XML to absolute paths, which are then made relative to the git root for
   * GitLab. The plugin's convention auto-populates this from
   * {@code sourceSets.main.java.srcDirs} on plain Java/Kotlin-JVM projects. For Android
   * or Kotlin Multiplatform builds — where there is no {@code main} source set on
   * {@code JavaPluginExtension} — set this explicitly, e.g.
   * {@code sourceRoots.setFrom(file("src/main/java"), file("src/main/kotlin"))}.
   */
  @Internal
  public abstract ConfigurableFileCollection getSourceRoots();

  /**
   * Project directory used as a starting point for git-root detection. Normally this is
   * left at the convention ({@code project.layout.projectDirectory}); override it only
   * for unusual layouts.
   */
  @Internal
  public abstract DirectoryProperty getProjectDirectory();

  @OutputFile
  public abstract RegularFileProperty getOutputFile();

  @TaskAction
  public void generate() {

    PluginLogger log = new PluginLogger(getLogger());

    File repositoryRoot = getRepositoryRootDir(getProjectDirectory().get().getAsFile(), log);

    List<String> compileSourceRoots = getSourceRoots().getFiles().stream()
        .filter(File::exists)
        .map(File::getAbsolutePath)
        .collect(Collectors.toList());

    if (compileSourceRoots.isEmpty()) {
      log.debug(
          "No source roots resolved; SpotBugs paths will be reported as-is (no repository prefix). "
              + "Set generateGitLabCodeQualityReport.sourceRoots explicitly for Android or "
              + "Kotlin Multiplatform projects.");
    }

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

    // The output file is always written, even when no findings exist (an empty JSON array
    // `[]`). This keeps GitLab CI's `reports:codequality` artifact pattern stable across
    // runs — it never fails an MR with "artifact not found" just because the build was
    // clean. Consumers that prefer "no file = no findings" semantics should remove the
    // file in a CI step when the array is empty.
    try (FileOutputStream stream = new FileOutputStream(output)) {
      new ReportSerializer().write(findings, stream);
      log.info("GitLab code quality report for {} issue created: {}",
          findings.size(), output);
    } catch (IOException e) {
      throw new GradleException("Failed to write GitLab code quality report", e);
    }

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
