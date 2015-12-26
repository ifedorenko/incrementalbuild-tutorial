package com.ifedorenko.incrementalbuild.tutorial;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Collections;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import io.takari.incrementalbuild.BuildContext;
import io.takari.incrementalbuild.MessageSeverity;
import io.takari.incrementalbuild.Resource;

/**
 * An simple mojo that copies input files specified by from directory and includes/excludes patterns
 * to the specified output directory specified. Inputs that are 13 bytes long are considered invalid
 * and result in build failure.
 */
@Mojo(name = "copy-resources", threadSafe = true)
public class CopyFilesMojo extends AbstractMojo {

  @Parameter(defaultValue = "${project.basedir}/src")
  private File from;

  @Parameter(defaultValue = "**/*")
  private Collection<String> includes = Collections.singleton("**/*");

  @Parameter
  private Collection<String> excludes;

  @Parameter(defaultValue = "${project.basedir}/target")
  private File to;

  @Component
  private BuildContext context;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    try {
      // register all inputs and determine inputs that require processing
      for (Resource<File> input : context.registerAndProcessInputs(from, includes, excludes)) {
        File inputFile = input.getResource();

        if (inputFile.length() == 13) {
          // add error messages for invalid messages and skip output generation
          input.addMessage(0 /* line */, 0 /* column */, "invalid file length",
              MessageSeverity.ERROR, null);
          continue;
        }

        File outputFile = getOutputFile(inputFile);

        // generate outputs
        try (OutputStream out = input.associateOutput(outputFile).newOutputStream()) {
          Files.copy(inputFile.toPath(), out);
        }
      }
    } catch (IOException e) {
      throw new MojoExecutionException("Could not copy resource(s)", e);
    }
  }

  private File getOutputFile(File inputFile) {
    String relpath = from.toPath().relativize(inputFile.toPath()).toString();
    return new File(to, relpath);
  }

}
