/*
 * © 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cds.feature.ord.resolver.impl;

import static com.sap.cds.feature.ord.common.Constants.PERSPECTIVE_SYSTEM_INSTANCE;
import static org.apache.commons.io.FilenameUtils.normalize;

import com.sap.cds.feature.ord.clients.MtxSidecarClient;
import com.sap.cds.feature.ord.clients.impl.MtxSidecarClientImpl;
import com.sap.cds.feature.ord.resolver.OrdResolver;
import com.sap.cds.services.runtime.CdsRuntime;
import com.sap.cds.services.utils.model.DynamicModelUtils;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
public class DynamicOrdResourceDefinitionResolver implements OrdResolver {

  private MtxSidecarClient mtxSidecarClient;
  private DynamicModelUtils dynamicModelUtils;

  @Override
  public void setCdsRuntime(CdsRuntime runtime) {
    this.dynamicModelUtils = new DynamicModelUtils(runtime);
    this.mtxSidecarClient = new MtxSidecarClientImpl(runtime);
  }

  @Override
  public int order() {
    return Integer.MAX_VALUE;
  }

  @Override
  public InputStream resolve(String resource) {
    return new ByteArrayInputStream(mtxSidecarClient.getOrdResourceDefinition(resource));
  }

  @Override
  public boolean isApplicable(String resource, String perspective) {
    return !dynamicModelUtils.useStaticModel() //
        && Objects.equals(PERSPECTIVE_SYSTEM_INSTANCE, perspective) //
        && !Objects.equals("documents/ord-document", normalize(resource.replace(":", "_")));
  }
}
