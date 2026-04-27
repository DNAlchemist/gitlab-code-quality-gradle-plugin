package io.github.dnalchemist.gitlab.codequality;

import java.io.InputStream;
import java.util.List;

public interface FindingProvider {

  String getName();

  List<Finding> getFindings(InputStream stream);

}
