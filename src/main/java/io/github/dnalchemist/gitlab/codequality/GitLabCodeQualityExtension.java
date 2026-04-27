package io.github.dnalchemist.gitlab.codequality;

import org.gradle.api.provider.Property;

/** Configures plugin integration with the surrounding build via {@code gitlabCodeQuality { ... }}. */
public abstract class GitLabCodeQualityExtension {

  /**
   * When {@code true} (default), wires {@code generateGitLabCodeQualityReport} into {@code check}.
   * Set to {@code false} to run it as an explicit step instead.
   */
  public abstract Property<Boolean> getWireIntoCheck();

}
