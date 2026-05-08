package io.github.dnalchemist.gitlab.codequality;

import org.gradle.api.provider.Property;

/** Configures plugin integration with the surrounding build via {@code gitlabCodeQuality { ... }}. */
public abstract class GitLabCodeQualityExtension {

  /**
   * When {@code true} (default), wires {@code generateGitLabCodeQualityReport} into {@code check}.
   * Set to {@code false} to run it as an explicit step instead.
   */
  public abstract Property<Boolean> getWireIntoCheck();

  /**
   * When {@code true}, automatically applies this plugin to all subprojects of the current project.
   * Convenient for multi-module builds where every module participates. Defaults to {@code false}
   * — apply the plugin manually (e.g. via {@code subprojects { ... }}) if you need finer control.
   */
  public abstract Property<Boolean> getApplyToSubprojects();

}
