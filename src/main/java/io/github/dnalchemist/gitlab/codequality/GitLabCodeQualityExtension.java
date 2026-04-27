package io.github.dnalchemist.gitlab.codequality;

import org.gradle.api.provider.Property;

/**
 * Extension that configures how the plugin integrates with the surrounding build.
 *
 * <p>Available as {@code gitlabCodeQuality { ... }} in build scripts.
 */
public abstract class GitLabCodeQualityExtension {

  /**
   * When {@code true} (default), the plugin wires {@code generateGitLabCodeQualityReport}
   * into the {@code check} lifecycle so it runs as part of {@code ./gradlew check}.
   *
   * <p>Disable this if upstream {@code spotbugsMain}/{@code checkstyleMain} tasks fail the
   * build (e.g. {@code ignoreFailures = false}) and you want to generate the GitLab report
   * as a separate, explicit step instead.
   */
  public abstract Property<Boolean> getWireIntoCheck();

}
