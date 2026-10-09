package com.sap.cds.feature.ord.resolver;

import com.sap.cds.services.runtime.CdsRuntimeAware;
import java.io.InputStream;

public interface OrdResolver extends CdsRuntimeAware {

  int order();

  InputStream resolve(String resource);

  boolean isApplicable(String resource, String perspective);
}
