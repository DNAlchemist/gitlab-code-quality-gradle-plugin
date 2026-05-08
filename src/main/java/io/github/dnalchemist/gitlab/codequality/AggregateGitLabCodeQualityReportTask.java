package io.github.dnalchemist.gitlab.codequality;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

/**
 * Merges multiple per-project {@code gl-code-quality-report.json} files into a single JSON array,
 * suitable for GitLab CI's {@code reports:codequality} artifact in multi-module builds.
 */
public abstract class AggregateGitLabCodeQualityReportTask extends DefaultTask {

  @InputFiles
  @PathSensitive(PathSensitivity.RELATIVE)
  public abstract ConfigurableFileCollection getInputReports();

  @OutputFile
  public abstract RegularFileProperty getOutputFile();

  @TaskAction
  public void aggregate() {
    PluginLogger log = new PluginLogger(getLogger());
    File output = getOutputFile().getAsFile().get();
    output.getParentFile().mkdirs();

    JsonArray merged = new JsonArray();
    for (File input : getInputReports()) {
      if (!input.isFile() || input.length() == 0) {
        continue;
      }
      try (Reader reader = new InputStreamReader(
          Files.newInputStream(input.toPath()), StandardCharsets.UTF_8)) {
        JsonElement parsed = JsonParser.parseReader(reader);
        if (parsed != null && parsed.isJsonArray()) {
          parsed.getAsJsonArray().forEach(merged::add);
        } else {
          log.warn("Skipping {}: not a JSON array", input);
        }
      } catch (IOException e) {
        throw new GradleException("Failed to read " + input, e);
      }
    }

    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    try (FileOutputStream out = new FileOutputStream(output);
        Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
      gson.toJson(merged, writer);
    } catch (IOException e) {
      throw new GradleException("Failed to write aggregated report", e);
    }
    log.info("Aggregated GitLab code quality report with {} findings: {}",
        merged.size(), output);
  }

}
