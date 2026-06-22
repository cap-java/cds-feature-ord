/*
 * © 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cds.feature.ord.configuration;

import static com.sap.cds.services.runtime.ExtendedServiceLoader.loadAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.sap.cds.feature.ord.processor.CdsOrdNodeProcessor;
import com.sap.cds.feature.ord.provider.impl.DynamicOrdResourcesProviderImpl;
import com.sap.cds.feature.ord.provider.impl.StaticOrdResourcesProviderImpl;
import com.sap.cds.services.environment.CdsEnvironment;
import com.sap.cds.services.environment.CdsProperties;
import com.sap.cds.services.environment.CdsProperties.Model;
import com.sap.cds.services.environment.CdsProperties.Model.Provider;
import com.sap.cds.services.environment.CdsProperties.MultiTenancy;
import com.sap.cds.services.environment.CdsProperties.MultiTenancy.Sidecar;
import com.sap.cds.services.runtime.CdsRuntime;
import com.sap.cds.services.runtime.CdsRuntimeConfigurer;
import com.sap.cds.services.runtime.ExtendedServiceLoader;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class OrdResourcesProviderConfigurationTest {

  private Model model;
  private Sidecar sidecar;
  private Provider provider;
  private CdsRuntime cdsRuntime;
  private MultiTenancy multiTenancy;
  private CdsProperties cdsProperties;
  private CdsEnvironment cdsEnvironment;
  private CdsRuntimeConfigurer cdsRuntimeConfigurer;

  private OrdResourcesProviderConfiguration classUnderTest;

  @BeforeEach
  void setUp() {
    model = mock(Model.class);
    sidecar = mock(Sidecar.class);
    provider = mock(Provider.class);
    cdsRuntime = mock(CdsRuntime.class);
    multiTenancy = mock(MultiTenancy.class);
    cdsProperties = mock(CdsProperties.class);
    cdsEnvironment = mock(CdsEnvironment.class);
    cdsRuntimeConfigurer = mock(CdsRuntimeConfigurer.class);

    classUnderTest = new OrdResourcesProviderConfiguration();

    doReturn(provider).when(model).getProvider();
    doReturn(model).when(cdsProperties).getModel();
    doReturn(sidecar).when(multiTenancy).getSidecar();
    doReturn(cdsEnvironment).when(cdsRuntime).getEnvironment();
    doReturn(multiTenancy).when(cdsProperties).getMultiTenancy();
    doReturn(cdsRuntime).when(cdsRuntimeConfigurer).getCdsRuntime();
    doReturn(cdsProperties).when(cdsEnvironment).getCdsProperties();
  }

  @Test
  void givenDynamicModelsNotSupported_whenProvidersIsCalled_thenNoOrdResourceProviderIsRegistered() {
    try (MockedStatic<ExtendedServiceLoader> extendedServiceLoader = mockStatic(ExtendedServiceLoader.class)) {
      extendedServiceLoader
          .when(() -> loadAll(CdsOrdNodeProcessor.class, cdsRuntime))
          .thenReturn(Collections.<CdsOrdNodeProcessor>emptyIterator());

      classUnderTest.providers(cdsRuntimeConfigurer);
    }

    verify(sidecar).getUrl();
    verify(provider).getUrl();
    verify(model).getProvider();
    verify(cdsProperties).getModel();
    verify(multiTenancy).getSidecar();
    verify(cdsProperties).getMultiTenancy();
    verify(cdsRuntimeConfigurer).getCdsRuntime();
    verify(cdsRuntime, times(2)).getEnvironment();
    verify(cdsEnvironment, times(2)).getCdsProperties();
    verify(cdsRuntimeConfigurer).provider(any(StaticOrdResourcesProviderImpl.class));
    verifyNoMoreInteractions(
        model,
        sidecar,
        provider,
        cdsRuntime,
        multiTenancy,
        cdsProperties,
        cdsEnvironment,
        cdsRuntimeConfigurer);
  }

  @Test
  void givenDynamicModelsAreSupported_whenProvidersIsCalled_thenCorrectProvidersAreRegistered() {
    doReturn(Boolean.TRUE).when(provider).isExtensibility();
    doReturn("https://dummy.provider.sap.com/").when(provider).getUrl();

    try (MockedStatic<ExtendedServiceLoader> extendedServiceLoader = mockStatic(ExtendedServiceLoader.class)) {
      extendedServiceLoader
          .when(() -> loadAll(CdsOrdNodeProcessor.class, cdsRuntime))
          .thenAnswer(in -> List.of(mock(CdsOrdNodeProcessor.class)).iterator());

      classUnderTest.providers(cdsRuntimeConfigurer);
    }

    verify(model).getProvider();
    verify(cdsProperties).getModel();
    verify(multiTenancy).getSidecar();
    verify(provider).isExtensibility();
    verify(cdsProperties).getMultiTenancy();
    verify(cdsRuntimeConfigurer).getCdsRuntime();
    verify(provider, times(2)).getUrl();
    verify(cdsRuntime, times(2)).getEnvironment();
    verify(cdsEnvironment, times(2)).getCdsProperties();
    verify(cdsRuntimeConfigurer).provider(any(StaticOrdResourcesProviderImpl.class));
    verify(cdsRuntimeConfigurer).provider(any(DynamicOrdResourcesProviderImpl.class));
    verifyNoMoreInteractions(
        model,
        sidecar,
        provider,
        cdsRuntime,
        multiTenancy,
        cdsProperties,
        cdsEnvironment,
        cdsRuntimeConfigurer);
  }
}
