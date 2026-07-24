/*
 * © 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cds.feature.ord.provider.impl;

import static com.sap.cds.feature.ord.common.Constants.PERSPECTIVE_SYSTEM_INSTANCE;
import static com.sap.cds.feature.ord.common.Constants.PERSPECTIVE_SYSTEM_VERSION;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.sap.cds.feature.ord.clients.MtxSidecarClient;
import com.sap.cds.feature.ord.provider.OrdResourcesProvider;
import com.sap.cds.services.environment.CdsEnvironment;
import com.sap.cds.services.environment.CdsProperties;
import com.sap.cds.services.environment.CdsProperties.OpenResourceDiscovery;
import com.sap.cds.services.runtime.CdsRuntime;
import com.sap.cds.services.utils.model.DynamicModelUtils;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DynamicOrdResourcesProviderImplTest {

  private CdsRuntime cdsRuntime;
  private CdsProperties cdsProperties;
  private CdsEnvironment cdsEnvironment;
  private MtxSidecarClient mtxSidecarClient;
  private DynamicModelUtils dynamicModelUtils;
  private OrdResourcesProvider ordResourcesProvider;
  private OpenResourceDiscovery openResourceDiscovery;

  private DynamicOrdResourcesProviderImpl classUnderTest;

  @BeforeEach
  void setUp() {
    cdsRuntime = mock(CdsRuntime.class);
    cdsProperties = mock(CdsProperties.class);
    cdsEnvironment = mock(CdsEnvironment.class);
    mtxSidecarClient = mock(MtxSidecarClient.class);
    dynamicModelUtils = mock(DynamicModelUtils.class);
    ordResourcesProvider = mock(OrdResourcesProvider.class);
    openResourceDiscovery = mock(OpenResourceDiscovery.class);
    classUnderTest = new DynamicOrdResourcesProviderImpl(mtxSidecarClient, dynamicModelUtils, List.of());

    classUnderTest.setPrevious(ordResourcesProvider);
    doReturn(cdsEnvironment).when(cdsRuntime).getEnvironment();
    doReturn(openResourceDiscovery).when(cdsProperties).getOrd();
    doReturn(cdsProperties).when(cdsEnvironment).getCdsProperties();
  }

  @Test
  void givenThatStaticModelShouldBeUsed_and_perspectiveIsSystemVersion_whenReadIsCalled_thenCorrectResultIsReturned()
      throws IOException {
    doReturn(true).when(dynamicModelUtils).useStaticModel();
    doReturn(new ByteArrayInputStream("{}".getBytes()))
        .when(ordResourcesProvider)
        .read("documents/ord-document", PERSPECTIVE_SYSTEM_VERSION);

    assertEquals(
        "{}",
        IOUtils.toString(classUnderTest.read("documents/ord-document", PERSPECTIVE_SYSTEM_VERSION), UTF_8));

    verify(dynamicModelUtils).useStaticModel();
    verify(ordResourcesProvider).read("documents/ord-document", PERSPECTIVE_SYSTEM_VERSION);
    verifyNoMoreInteractions(
        cdsRuntime,
        cdsProperties,
        cdsEnvironment,
        mtxSidecarClient,
        dynamicModelUtils,
        openResourceDiscovery,
        ordResourcesProvider);
  }

  @Test
  void givenThatStaticModelShouldBeUsed_and_perspectiveIsSystemInstance_whenReadIsCalled_thenCorrectResultIsReturned()
      throws IOException {
    doReturn(true).when(dynamicModelUtils).useStaticModel();
    doReturn(new ByteArrayInputStream("{}".getBytes()))
        .when(ordResourcesProvider)
        .read("documents/ord-document", PERSPECTIVE_SYSTEM_INSTANCE);

    assertEquals(
        "{}",
        IOUtils.toString(classUnderTest.read("documents/ord-document", PERSPECTIVE_SYSTEM_INSTANCE), UTF_8));

    verify(dynamicModelUtils).useStaticModel();
    verify(ordResourcesProvider).read("documents/ord-document", PERSPECTIVE_SYSTEM_INSTANCE);
    verifyNoMoreInteractions(
        cdsRuntime,
        cdsProperties,
        cdsEnvironment,
        mtxSidecarClient,
        dynamicModelUtils,
        openResourceDiscovery,
        ordResourcesProvider);
  }

  @Test
  void
      givenThatDynamicModelShouldBeUsed_and_perspectiveIsSystemVersion_and_ordDocumentIsRequested_whenReadIsCalled_thenCorrectResultIsReturned()
          throws IOException {
    doReturn(false).when(dynamicModelUtils).useStaticModel();
    doReturn(new ByteArrayInputStream("{}".getBytes()))
        .when(ordResourcesProvider)
        .read("documents/ord-document", PERSPECTIVE_SYSTEM_VERSION);

    assertEquals(
        "{}",
        IOUtils.toString(classUnderTest.read("documents/ord-document", PERSPECTIVE_SYSTEM_VERSION), UTF_8));

    verify(dynamicModelUtils).useStaticModel();
    verify(ordResourcesProvider).read("documents/ord-document", PERSPECTIVE_SYSTEM_VERSION);
    verifyNoMoreInteractions(
        cdsRuntime,
        cdsProperties,
        cdsEnvironment,
        mtxSidecarClient,
        dynamicModelUtils,
        openResourceDiscovery,
        ordResourcesProvider);
  }

  @Test
  void
      givenThatDynamicModelShouldBeUsed_and_perspectiveIsSystemInstance_and_ordDocumentIsRequested_whenReadIsCalled_thenCorrectResultIsReturned()
          throws IOException {
    doReturn(false).when(dynamicModelUtils).useStaticModel();
    doReturn("{}").when(mtxSidecarClient).getOrdDocument();

    assertEquals(
        "{}",
        IOUtils.toString(classUnderTest.read("documents/ord-document", PERSPECTIVE_SYSTEM_INSTANCE), UTF_8));

    verify(mtxSidecarClient).getOrdDocument();
    verify(dynamicModelUtils).useStaticModel();
    verifyNoMoreInteractions(
        cdsRuntime,
        cdsProperties,
        cdsEnvironment,
        mtxSidecarClient,
        dynamicModelUtils,
        openResourceDiscovery,
        ordResourcesProvider);
  }

  @Test
  void
      givenThatStaticModelShouldBeUsed_and_perspectiveIsSystemVersion_and_resourceDefinitionIsRequested_whenReadIsCalled_thenCorrectResultIsReturned()
          throws IOException {
    doReturn(true).when(dynamicModelUtils).useStaticModel();
    doReturn("ord/").when(openResourceDiscovery).getOrdResourcesRoot();
    doReturn(new ByteArrayInputStream("{}".getBytes()))
        .when(ordResourcesProvider)
        .read("test:ord:service/test.oas3.json", PERSPECTIVE_SYSTEM_VERSION);

    assertEquals(
        "{}",
        IOUtils.toString(
            classUnderTest.read("test:ord:service/test.oas3.json", PERSPECTIVE_SYSTEM_VERSION), UTF_8));

    verify(dynamicModelUtils).useStaticModel();
    verify(ordResourcesProvider).read("test:ord:service/test.oas3.json", PERSPECTIVE_SYSTEM_VERSION);
    verifyNoMoreInteractions(
        cdsRuntime,
        cdsProperties,
        cdsEnvironment,
        mtxSidecarClient,
        dynamicModelUtils,
        openResourceDiscovery,
        ordResourcesProvider);
  }

  @Test
  void
      givenThatStaticModelShouldBeUsed_and_perspectiveIsSystemInstance_and_resourceDefinitionIsRequested_whenReadIsCalled_thenCorrectResultIsReturned()
          throws IOException {
    doReturn(true).when(dynamicModelUtils).useStaticModel();
    doReturn("ord/").when(openResourceDiscovery).getOrdResourcesRoot();
    doReturn(new ByteArrayInputStream("{}".getBytes()))
        .when(ordResourcesProvider)
        .read("test:ord:service/test.oas3.json", PERSPECTIVE_SYSTEM_INSTANCE);

    assertEquals(
        "{}",
        IOUtils.toString(
            classUnderTest.read("test:ord:service/test.oas3.json", PERSPECTIVE_SYSTEM_INSTANCE), UTF_8));

    verify(dynamicModelUtils).useStaticModel();
    verify(ordResourcesProvider).read("test:ord:service/test.oas3.json", PERSPECTIVE_SYSTEM_INSTANCE);
    verifyNoMoreInteractions(
        cdsRuntime,
        cdsProperties,
        cdsEnvironment,
        mtxSidecarClient,
        dynamicModelUtils,
        openResourceDiscovery,
        ordResourcesProvider);
  }

  @Test
  void
      givenThatDynamicModelShouldBeUsed_and_perspectiveIsSystemInstance_and_resourceDefinitionIsRequested_whenReadIsCalled_thenCorrectResultIsReturned()
          throws IOException {
    doReturn("ord/").when(openResourceDiscovery).getOrdResourcesRoot();
    doReturn(false).when(dynamicModelUtils).useStaticModel();
    doReturn("{}").when(mtxSidecarClient).getOrdResourceDefinition("test:ord:service/test.oas3.json");

    assertEquals(
        "{}",
        IOUtils.toString(
            classUnderTest.read("test:ord:service/test.oas3.json", PERSPECTIVE_SYSTEM_INSTANCE), UTF_8));

    verify(dynamicModelUtils).useStaticModel();
    verify(mtxSidecarClient).getOrdResourceDefinition("test:ord:service/test.oas3.json");
    verifyNoMoreInteractions(
        cdsRuntime,
        cdsProperties,
        cdsEnvironment,
        mtxSidecarClient,
        dynamicModelUtils,
        openResourceDiscovery,
        ordResourcesProvider);
  }

  @Test
  void
      givenThatDynamicModelShouldBeUsed_and_perspectiveIsSystemVersion_and_resourceDefinitionIsRequested_whenReadIsCalled_thenCorrectResultIsReturned()
          throws IOException {
    doReturn("ord/").when(openResourceDiscovery).getOrdResourcesRoot();
    doReturn(false).when(dynamicModelUtils).useStaticModel();
    doReturn(new ByteArrayInputStream("{}".getBytes()))
        .when(ordResourcesProvider)
        .read("test:ord:service/test.oas3.json", PERSPECTIVE_SYSTEM_VERSION);

    assertEquals(
        "{}",
        IOUtils.toString(
            classUnderTest.read("test:ord:service/test.oas3.json", PERSPECTIVE_SYSTEM_VERSION), UTF_8));

    verify(dynamicModelUtils).useStaticModel();
    verify(ordResourcesProvider).read("test:ord:service/test.oas3.json", PERSPECTIVE_SYSTEM_VERSION);
    verifyNoMoreInteractions(
        cdsRuntime,
        cdsProperties,
        cdsEnvironment,
        mtxSidecarClient,
        dynamicModelUtils,
        openResourceDiscovery,
        ordResourcesProvider);
  }
}
