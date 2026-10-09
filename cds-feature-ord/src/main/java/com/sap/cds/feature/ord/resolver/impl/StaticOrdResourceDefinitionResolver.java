/*
 * © 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cds.feature.ord.resolver.impl;

import static com.sap.cds.feature.ord.common.Constants.PERSPECTIVE_SYSTEM_VERSION;
import static com.sap.cds.feature.ord.common.Utils.CdsRuntimeProperties.getOrdProperties;
import static com.sap.cds.feature.ord.common.Utils.Resources.getResourceAsStream;
import static com.sap.cds.feature.ord.common.Utils.Resources.resourceExists;
import static org.apache.commons.io.FilenameUtils.concat;
import static org.apache.commons.io.FilenameUtils.getBaseName;
import static org.apache.commons.io.FilenameUtils.getExtension;
import static org.apache.commons.io.FilenameUtils.normalize;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import com.sap.cds.adapter.edmx.EdmxV4Provider;
import com.sap.cds.feature.ord.resolver.OrdResolver;
import com.sap.cds.services.runtime.CdsRuntime;
import java.io.InputStream;
import java.util.Objects;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class StaticOrdResourceDefinitionResolver implements OrdResolver {

  private static final String EDMX_EXTENSION = "edmx";

  private CdsRuntime runtime;

  @Override
  public void setCdsRuntime(CdsRuntime runtime) {
    this.runtime = runtime;
  }

  @Override
  public int order() {
    return Integer.MAX_VALUE;
  }

  @Override
  public InputStream resolve(String resource) {
    String path = resolveResourcePath(resource);

    if (EDMX_EXTENSION.equalsIgnoreCase(getExtension(path))) {
      return runtime.getProvider(EdmxV4Provider.class).getEdmx(getBaseName(path));
    }

    return getResourceAsStream(path);
  }

  @Override
  public boolean isApplicable(String resource, String perspective) {
    return resourceExists(resolveResourcePath(resource))
        && (isEmpty(perspective) || Objects.equals(PERSPECTIVE_SYSTEM_VERSION, perspective))
        && !Objects.equals("documents/ord-document", normalize(resource.replace(":", "_")));
  }

  private String resolveResourcePath(String resource) {
    return concat(getOrdProperties(runtime).getOrdResourcesRoot(), normalize(resource.replace(":", "_")));
  }
}
