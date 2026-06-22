/*
 * © 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cds.feature.ord.provider.impl;

import static com.sap.cds.feature.ord.common.Utils.CdsRuntimeProperties.getAuthenticationProperties;

import com.sap.cds.feature.ord.provider.AuthenticationManagerProvider;
import com.sap.cds.services.runtime.CdsRuntime;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AuthenticationManagerProviderImpl implements AuthenticationManagerProvider {

  private final CdsRuntime cdsRuntime;
  private final List<String> accessStrategies;
  private final Consumer<CdsRuntime> validator;

  @Override
  public void checkAuthorization(String uri) {
    if (getAuthenticationProperties(cdsRuntime).isAuthenticateMetadataEndpoints()) {
      validator.accept(cdsRuntime);
    }
  }

  @Override
  public List<String> getAccessStrategies() {
    return accessStrategies;
  }
}
