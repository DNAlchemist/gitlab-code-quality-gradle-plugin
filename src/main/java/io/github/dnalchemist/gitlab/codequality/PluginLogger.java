package io.github.dnalchemist.gitlab.codequality;

import org.gradle.api.logging.Logger;

public final class PluginLogger {

  private final Logger log;

  public PluginLogger(Logger log) {
    this.log = log;
  }

  public void debug(String pattern, Object... arguments) {
    log.debug(buildMessage(pattern, arguments));
  }

  public void info(String pattern, Object... arguments) {
    log.lifecycle(buildMessage(pattern, arguments));
  }

  public void warn(String pattern, Object... arguments) {
    log.warn(buildMessage(pattern, arguments));
  }

  public void error(String pattern, Object... arguments) {
    log.error(buildMessage(pattern, arguments));
  }

  private static String buildMessage(String pattern, Object... arguments) {
    String message = pattern;
    for (Object argument : arguments) {
      message = message.replaceFirst("\\{}", argument != null ? argument.toString() : "null");
    }
    return message;
  }

}
