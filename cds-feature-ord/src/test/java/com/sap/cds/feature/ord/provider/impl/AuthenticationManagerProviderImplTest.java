/*
 * © 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cds.feature.ord.provider.impl;

import static com.sap.cds.services.utils.cert.UclAuthUtils.AccessStrategy.MTLS;
import static com.sap.cds.services.utils.cert.UclAuthUtils.AccessStrategy.OPEN;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.sap.cds.services.environment.CdsEnvironment;
import com.sap.cds.services.environment.CdsProperties;
import com.sap.cds.services.environment.CdsProperties.Security;
import com.sap.cds.services.environment.CdsProperties.Security.Authentication;
import com.sap.cds.services.runtime.CdsRuntime;
import com.sap.cds.services.utils.cert.UclAuthUtils.AccessStrategy;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuthenticationManagerProviderImplTest {

  private Security security;
  private CdsRuntime cdsRuntime;
  private CdsProperties cdsProperties;
  private Authentication authentication;
  private CdsEnvironment cdsEnvironment;
  private Consumer<CdsRuntime> validator;

  private AuthenticationManagerProviderImpl classUnderTest;

  @BeforeEach
  void setUp() {
    validator = mock();
    security = mock(Security.class);
    cdsRuntime = mock(CdsRuntime.class);
    cdsProperties = mock(CdsProperties.class);
    authentication = mock(Authentication.class);
    cdsEnvironment = mock(CdsEnvironment.class);

    doReturn(security).when(cdsProperties).getSecurity();
    doReturn(cdsEnvironment).when(cdsRuntime).getEnvironment();
    doReturn(authentication).when(security).getAuthentication();
    doReturn(cdsProperties).when(cdsEnvironment).getCdsProperties();
  }

  @Test
  void givenMetadataEndpointsAuthenticated_whenGetAccessStrategiesIsCalled_thenCorrectResultIsReturned() {
    doReturn(TRUE).when(authentication).isAuthenticateMetadataEndpoints();

    classUnderTest = new AuthenticationManagerProviderImpl(
        cdsRuntime, List.of(AccessStrategy.fromConfig(cdsRuntime).getValue()), validator);

    assertEquals(List.of(MTLS.getValue()), classUnderTest.getAccessStrategies());

    verify(cdsRuntime).getEnvironment();
    verify(cdsProperties).getSecurity();
    verify(security).getAuthentication();
    verify(cdsEnvironment).getCdsProperties();
    verify(authentication).isAuthenticateMetadataEndpoints();
    verifyNoMoreInteractions(security, cdsRuntime, validator, cdsProperties, authentication, cdsEnvironment);
  }

  @Test
  void givenMetadataEndpointsNotAuthenticated_whenGetAccessStrategiesIsCalled_thenCorrectResultIsReturned() {
    doReturn(FALSE).when(authentication).isAuthenticateMetadataEndpoints();

    classUnderTest = new AuthenticationManagerProviderImpl(
        cdsRuntime, List.of(AccessStrategy.fromConfig(cdsRuntime).getValue()), validator);

    assertEquals(List.of(OPEN.getValue()), classUnderTest.getAccessStrategies());

    verify(cdsRuntime).getEnvironment();
    verify(cdsProperties).getSecurity();
    verify(security).getAuthentication();
    verify(cdsEnvironment).getCdsProperties();
    verify(authentication).isAuthenticateMetadataEndpoints();
    verifyNoMoreInteractions(security, cdsRuntime, validator, cdsProperties, authentication, cdsEnvironment);
  }

  @Test
  void givenMetadataEndpointsNotAuthenticated_whenCheckAuthorizationIsCalled_thenNothingIsDone() {
    doReturn(FALSE).when(authentication).isAuthenticateMetadataEndpoints();

    new AuthenticationManagerProviderImpl(
            cdsRuntime,
            List.of(AccessStrategy.fromConfig(cdsRuntime).getValue()),
            validator)
        .checkAuthorization("");

    verify(validator, times(0)).accept(cdsRuntime);
    verify(cdsRuntime, times(2)).getEnvironment();
    verify(cdsProperties, times(2)).getSecurity();
    verify(security, times(2)).getAuthentication();
    verify(cdsEnvironment, times(2)).getCdsProperties();
    verify(authentication, times(2)).isAuthenticateMetadataEndpoints();
    verifyNoMoreInteractions(security, cdsRuntime, validator, cdsProperties, authentication, cdsEnvironment);
  }

  @Test
  void givenMetadataEndpointsAuthenticated_whenCheckAuthorizationIsCalled_thenUclAuthUtilsAreInvoked() {
    doReturn(TRUE).when(authentication).isAuthenticateMetadataEndpoints();

    new AuthenticationManagerProviderImpl(
            cdsRuntime,
            List.of(AccessStrategy.fromConfig(cdsRuntime).getValue()),
            validator)
        .checkAuthorization("");

    verify(validator).accept(cdsRuntime);
    verify(cdsRuntime, times(2)).getEnvironment();
    verify(cdsProperties, times(2)).getSecurity();
    verify(security, times(2)).getAuthentication();
    verify(cdsEnvironment, times(2)).getCdsProperties();
    verify(authentication, times(2)).isAuthenticateMetadataEndpoints();
    verifyNoMoreInteractions(security, cdsRuntime, validator, cdsProperties, authentication, cdsEnvironment);
  }
}
