/*
 * © 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cds.feature.ord.configuration;

import static com.sap.cds.services.utils.cert.UclAuthUtils.checkAuthorization;
import static com.sap.cds.services.utils.cert.UclAuthUtils.createCertValidator;

import com.sap.cds.feature.ord.provider.impl.AuthenticationManagerProviderImpl;
import com.sap.cds.services.runtime.CdsRuntimeConfiguration;
import com.sap.cds.services.runtime.CdsRuntimeConfigurer;
import com.sap.cds.services.utils.cert.UclAuthUtils.AccessStrategy;
import java.util.List;

public class AuthenticationManagerProviderConfiguration implements CdsRuntimeConfiguration {

  @Override
  public void providers(CdsRuntimeConfigurer configurer) {
    configurer.provider(new AuthenticationManagerProviderImpl(
        configurer.getCdsRuntime(),
        List.of(AccessStrategy.fromConfig(configurer.getCdsRuntime()).getValue()),
        cdsRuntime -> checkAuthorization(createCertValidator(cdsRuntime), cdsRuntime)));
  }
}
