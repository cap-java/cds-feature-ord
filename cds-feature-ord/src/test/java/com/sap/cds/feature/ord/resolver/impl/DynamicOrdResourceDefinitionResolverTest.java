/*
 * © 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cds.feature.ord.resolver.impl;

import static com.sap.cds.feature.ord.common.Constants.PERSPECTIVE_SYSTEM_INSTANCE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.sap.cds.adapter.edmx.EdmxV4Provider;
import com.sap.cds.feature.ord.clients.MtxSidecarClient;
import com.sap.cds.services.runtime.CdsRuntime;
import com.sap.cds.services.utils.model.DynamicModelUtils;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DynamicOrdResourceDefinitionResolverTest {

  private MtxSidecarClient client;
  private CdsRuntime runtime;
  private DynamicModelUtils dynamicModelUtils;
  private DynamicOrdResourceDefinitionResolver classUnderTest;

  @BeforeEach
  void setUp() {
    client = mock(MtxSidecarClient.class);
    runtime = mock(CdsRuntime.class);
    dynamicModelUtils = mock(DynamicModelUtils.class);
    classUnderTest = new DynamicOrdResourceDefinitionResolver(runtime, client, dynamicModelUtils);
  }

  @Test
  void resolvesDefinitionReturnedBySidecar() throws IOException {
    doReturn("definition".getBytes(UTF_8)).when(client).getOrdResourceDefinition("api:service.oas3.json");

    assertEquals(Integer.MAX_VALUE, classUnderTest.order());
    try (var result = classUnderTest.resolve("api:service.oas3.json", PERSPECTIVE_SYSTEM_INSTANCE)) {
      assertEquals("definition", new String(result.readAllBytes(), UTF_8));
    }
    verifyNoInteractions(runtime);
  }

  @Test
  void delegatesEdmxResourcesToProvider() {
    EdmxV4Provider provider = mock(EdmxV4Provider.class);
    var expected = new ByteArrayInputStream("edmx".getBytes(UTF_8));
    doReturn(provider).when(runtime).getProvider(EdmxV4Provider.class);
    doReturn(expected).when(provider).getEdmx("AdminService");

    assertEquals(expected, classUnderTest.resolve("api/AdminService.EDMX", PERSPECTIVE_SYSTEM_INSTANCE));
    verify(provider).getEdmx("AdminService");
    verifyNoInteractions(client);
  }

  @Test
  void appliesOnlyToDynamicInstanceResourcesOtherThanTheDocument() {
    doReturn(false).when(dynamicModelUtils).useStaticModel();

    assertTrue(classUnderTest.isApplicable("api:service.oas3.json", PERSPECTIVE_SYSTEM_INSTANCE));
    assertFalse(classUnderTest.isApplicable("documents/ord-document", PERSPECTIVE_SYSTEM_INSTANCE));
    assertFalse(classUnderTest.isApplicable("api:service.oas3.json", "system-version"));

    doReturn(true).when(dynamicModelUtils).useStaticModel();
    assertFalse(classUnderTest.isApplicable("api:service.oas3.json", PERSPECTIVE_SYSTEM_INSTANCE));
  }
}
