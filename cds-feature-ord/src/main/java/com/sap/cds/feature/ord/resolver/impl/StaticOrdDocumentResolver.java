/*
 * © 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cds.feature.ord.resolver.impl;

import static com.sap.cds.feature.ord.common.Constants.PERSPECTIVE_SYSTEM_VERSION;
import static com.sap.cds.feature.ord.common.Utils.CdsRuntimeProperties.getOrdProperties;
import static com.sap.cds.feature.ord.common.Utils.Resources.asOrdJsonInputStream;
import static com.sap.cds.feature.ord.common.Utils.Resources.getResourceAsStream;
import static com.sap.cds.feature.ord.common.Utils.Streams.asList;
import static com.sap.cds.services.runtime.ExtendedServiceLoader.loadAll;
import static org.apache.commons.io.FilenameUtils.concat;
import static org.apache.commons.io.FilenameUtils.normalize;

import com.sap.cds.feature.ord.core.customizers.CdsOrdNodeCustomizer;
import com.sap.cds.feature.ord.core.generators.CdsOrdNodeGenerator;
import com.sap.cds.feature.ord.resolver.OrdResolver;
import com.sap.cds.services.environment.CdsProperties.OpenResourceDiscovery;
import com.sap.cds.services.runtime.CdsRuntime;
import java.io.InputStream;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
public class StaticOrdDocumentResolver implements OrdResolver {

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
    OpenResourceDiscovery properties = getOrdProperties(runtime);

    return asOrdJsonInputStream(
        getResourceAsStream(concat(properties.getOrdResourcesRoot(), properties.getOrdDocumentPath())),
        asList(loadAll(CdsOrdNodeGenerator.class, runtime)),
        asList(loadAll(CdsOrdNodeCustomizer.class, runtime)));
  }

  @Override
  public boolean isApplicable(String resource, String perspective) {
    return Objects.equals(PERSPECTIVE_SYSTEM_VERSION, perspective)
        && Objects.equals("documents/ord-document", normalize(resource.replace(":", "_")));
  }
}
