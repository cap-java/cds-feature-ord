/*
 * © 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cds.feature.ord.provider.impl;

import static com.sap.cds.feature.ord.common.Constants.PERSPECTIVE_SYSTEM_INSTANCE;
import static com.sap.cds.feature.ord.common.Utils.Resources.asOrdJsonInputStream;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.sap.cds.feature.ord.clients.MtxSidecarClient;
import com.sap.cds.feature.ord.processor.CdsOrdNodeProcessor;
import com.sap.cds.feature.ord.provider.OrdResourcesProvider;
import com.sap.cds.services.utils.model.DynamicModelUtils;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DynamicOrdResourcesProviderImpl implements OrdResourcesProvider {

  private static final String ORD_DOCUMENT_PATH = "documents/ord-document";

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
        ? loadFromMTXSidecar(resource)
        : this.previous.read(resource, perspective);
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
