/*
 * © 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cds.feature.ord.configuration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.sap.cds.feature.ord.provider.AuthenticationManagerProvider;
import com.sap.cds.services.environment.CdsEnvironment;
import com.sap.cds.services.environment.CdsProperties;
import com.sap.cds.services.environment.CdsProperties.Security;
import com.sap.cds.services.environment.CdsProperties.Security.Authentication;
import com.sap.cds.services.runtime.CdsRuntime;
import com.sap.cds.services.runtime.CdsRuntimeConfigurer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuthorizationManagerProviderConfigurationTest {

  private Security security;
  private CdsRuntime cdsRuntime;
  private CdsProperties cdsProperties;
  private CdsEnvironment cdsEnvironment;
  private Authentication authentication;
  private CdsRuntimeConfigurer cdsRuntimeConfigurer;

  private AuthenticationManagerProviderConfiguration classUnderTest;

  @BeforeEach
  void setUp() {
    security = mock(Security.class);
    cdsRuntime = mock(CdsRuntime.class);
    cdsProperties = mock(CdsProperties.class);
    authentication = mock(Authentication.class);
    cdsEnvironment = mock(CdsEnvironment.class);
    cdsRuntimeConfigurer = mock(CdsRuntimeConfigurer.class);
    classUnderTest = new AuthenticationManagerProviderConfiguration();

    doReturn(security).when(cdsProperties).getSecurity();
    doReturn(cdsEnvironment).when(cdsRuntime).getEnvironment();
    doReturn(authentication).when(security).getAuthentication();
    doReturn(cdsRuntime).when(cdsRuntimeConfigurer).getCdsRuntime();
    doReturn(cdsProperties).when(cdsEnvironment).getCdsProperties();
  }

  @Test
  void whenProvidersIsCalled_thenAuthorizationManagerProviderIsRegistered() {
    classUnderTest.providers(cdsRuntimeConfigurer);

    verify(cdsProperties).getSecurity();
    verify(cdsRuntime).getEnvironment();
    verify(security).getAuthentication();
    verify(cdsEnvironment).getCdsProperties();
    verify(authentication).isAuthenticateMetadataEndpoints();
    verify(cdsRuntimeConfigurer, times(2)).getCdsRuntime();
    verify(cdsRuntimeConfigurer).provider(any(AuthenticationManagerProvider.class));
    verifyNoMoreInteractions(
        security,
        cdsRuntime,
        cdsProperties,
        authentication,
        cdsEnvironment,
        cdsRuntimeConfigurer,
        cdsRuntimeConfigurer);
  }
}
