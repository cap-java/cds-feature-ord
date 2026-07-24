/*
 * © 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cds.feature.ord.configuration;

import static com.sap.cds.feature.ord.common.Utils.Streams.asList;
import static com.sap.cds.services.runtime.ExtendedServiceLoader.loadAll;

import com.sap.cds.feature.ord.clients.impl.MtxSidecarClientImpl;
import com.sap.cds.feature.ord.processor.CdsOrdNodeProcessor;
import com.sap.cds.feature.ord.provider.impl.DynamicOrdResourcesProviderImpl;
import com.sap.cds.feature.ord.provider.impl.StaticOrdResourcesProviderImpl;
import com.sap.cds.services.runtime.CdsRuntime;
import com.sap.cds.services.runtime.CdsRuntimeConfiguration;
import com.sap.cds.services.runtime.CdsRuntimeConfigurer;
import com.sap.cds.services.utils.model.DynamicModelUtils;

public class OrdResourcesProviderConfiguration implements CdsRuntimeConfiguration {

  @Override
  public void providers(CdsRuntimeConfigurer configurer) {
    CdsRuntime cdsRuntime = configurer.getCdsRuntime();
    DynamicModelUtils dynamicModelUtils = new DynamicModelUtils(cdsRuntime);

    configurer.provider(
        new StaticOrdResourcesProviderImpl(cdsRuntime, asList(loadAll(CdsOrdNodeProcessor.class, cdsRuntime))));

    if (dynamicModelUtils.isModelProviderEnabled()) {
      configurer.provider(new DynamicOrdResourcesProviderImpl(
          new MtxSidecarClientImpl(cdsRuntime),
          dynamicModelUtils,
          asList(loadAll(CdsOrdNodeProcessor.class, cdsRuntime))));
    }
  }
}
