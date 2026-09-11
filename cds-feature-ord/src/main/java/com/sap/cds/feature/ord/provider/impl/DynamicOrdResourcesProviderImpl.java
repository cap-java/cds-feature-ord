/*
 * © 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cds.feature.ord.provider.impl;

import static com.sap.cds.feature.ord.common.Constants.PERSPECTIVE_SYSTEM_INSTANCE;
import static com.sap.cds.feature.ord.common.Utils.Resources.asOrdJsonInputStream;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.FilenameUtils.getBaseName;
import static org.apache.commons.io.FilenameUtils.getExtension;

import com.sap.cds.adapter.edmx.EdmxV4Provider;
import com.sap.cds.feature.ord.clients.MtxSidecarClient;
import com.sap.cds.feature.ord.processor.CdsOrdNodeProcessor;
import com.sap.cds.feature.ord.provider.OrdResourcesProvider;
import com.sap.cds.services.runtime.CdsRuntime;
import com.sap.cds.services.utils.model.DynamicModelUtils;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DynamicOrdResourcesProviderImpl implements OrdResourcesProvider {

  private static final String EDMX_EXTENSION = "edmx";
  private static final String ORD_DOCUMENT_PATH = "documents/ord-document";

  private final CdsRuntime cdsRuntime;
  private final MtxSidecarClient mtxSidecarClient;
  private final DynamicModelUtils dynamicModelUtils;
  private final List<CdsOrdNodeProcessor> cdsOrdNodeProcessors;

  private OrdResourcesProvider previous;

  @Override
  public void setPrevious(OrdResourcesProvider previous) {
    this.previous = previous;
  }

  @Override
  public InputStream read(String resource, String perspective) {
    return !dynamicModelUtils.useStaticModel() && Objects.equals(PERSPECTIVE_SYSTEM_INSTANCE, perspective)
        ? this.loadResource(resource)
        : this.previous.read(resource, perspective);
  }

  private InputStream loadResource(String resource) {
    if (EDMX_EXTENSION.equalsIgnoreCase(getExtension(resource))) {
      return cdsRuntime.getProvider(EdmxV4Provider.class).getEdmx(getBaseName(resource));
    }

    return this.loadFromMTXSidecar(resource);
  }

  private InputStream loadFromMTXSidecar(String resource) {
    return Objects.equals(ORD_DOCUMENT_PATH, resource)
        ? asOrdJsonInputStream(
            new ByteArrayInputStream(
                mtxSidecarClient.getOrdDocument().getBytes(UTF_8)),
            cdsOrdNodeProcessors)
        : new ByteArrayInputStream(
            mtxSidecarClient.getOrdResourceDefinition(resource).getBytes(UTF_8));
  }
}
