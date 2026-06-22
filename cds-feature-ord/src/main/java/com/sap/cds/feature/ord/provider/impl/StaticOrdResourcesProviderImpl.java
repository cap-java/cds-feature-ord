/*
 * © 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cds.feature.ord.provider.impl;

import static com.sap.cds.feature.ord.common.Constants.PERSPECTIVE_SYSTEM_VERSION;
import static com.sap.cds.feature.ord.common.Utils.CdsRuntimeProperties.getOrdProperties;
import static com.sap.cds.feature.ord.common.Utils.Resources.asOrdJsonInputStream;
import static com.sap.cds.feature.ord.common.Utils.Resources.getResourceAsStream;
import static java.util.Objects.nonNull;
import static org.apache.commons.io.FilenameUtils.concat;
import static org.apache.commons.io.FilenameUtils.normalize;

import com.sap.cds.feature.ord.processor.CdsOrdNodeProcessor;
import com.sap.cds.feature.ord.provider.OrdResourcesProvider;
import com.sap.cds.services.runtime.CdsRuntime;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class StaticOrdResourcesProviderImpl implements OrdResourcesProvider {

  private final CdsRuntime cdsRuntime;
  private final List<CdsOrdNodeProcessor> cdsOrdNodeProcessors;

  @Override
  public InputStream read(String resource, String perspective) {
    if (nonNull(perspective) && !Objects.equals(PERSPECTIVE_SYSTEM_VERSION, perspective)) {
      throw new IllegalArgumentException("Perspective '%s' not supported".formatted(perspective));
    }

    String normalized = normalize(resource.replace(":", "_"));
    String ordResourcesRoot = getOrdProperties(cdsRuntime).getOrdResourcesRoot();
    String ordDocumentRelativePath = getOrdProperties(cdsRuntime).getOrdDocumentPath();
    String ordDocumentAbsolutePath = concat(ordResourcesRoot, ordDocumentRelativePath);

    return Objects.equals("documents/ord-document", normalized)
        ? asOrdJsonInputStream(getResourceAsStream(ordDocumentAbsolutePath), cdsOrdNodeProcessors)
        : getResourceAsStream(concat(ordResourcesRoot, normalized));
  }
}
