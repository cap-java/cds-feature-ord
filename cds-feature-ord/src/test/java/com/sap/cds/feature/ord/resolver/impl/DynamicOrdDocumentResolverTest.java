/*
 * © 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cds.feature.ord.resolver.impl;

import static com.sap.cds.feature.ord.common.Constants.PERSPECTIVE_SYSTEM_INSTANCE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.sap.cds.feature.ord.clients.MtxSidecarClient;
import com.sap.cds.services.runtime.CdsRuntimeConfigurer;
import com.sap.cds.services.utils.model.DynamicModelUtils;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DynamicOrdDocumentResolverTest {

  private MtxSidecarClient client;
  private DynamicModelUtils dynamicModelUtils;
  private DynamicOrdDocumentResolver classUnderTest;

  @BeforeEach
  void setUp() {
    client = mock(MtxSidecarClient.class);
    dynamicModelUtils = mock(DynamicModelUtils.class);
    classUnderTest =
        new DynamicOrdDocumentResolver(CdsRuntimeConfigurer.create().complete(), client, dynamicModelUtils);
  }

  @Test
  void resolvesDocumentReturnedBySidecar() throws IOException {
    doReturn("{}".getBytes(UTF_8)).when(client).getOrdDocument();

    assertEquals(Integer.MAX_VALUE, classUnderTest.order());
    try (var result = classUnderTest.resolve("documents/ord-document", PERSPECTIVE_SYSTEM_INSTANCE)) {
      assertNotNull(result);
    }

    verify(client).getOrdDocument();
  }

  @Test
  void appliesOnlyToDynamicInstanceDocument() {
    doReturn(false).when(dynamicModelUtils).useStaticModel();

    assertTrue(classUnderTest.isApplicable("documents/ord-document", PERSPECTIVE_SYSTEM_INSTANCE));
    assertFalse(classUnderTest.isApplicable("api:service.oas3.json", PERSPECTIVE_SYSTEM_INSTANCE));
    assertFalse(classUnderTest.isApplicable("documents/ord-document", "system-version"));

    doReturn(true).when(dynamicModelUtils).useStaticModel();
    assertFalse(classUnderTest.isApplicable("documents/ord-document", PERSPECTIVE_SYSTEM_INSTANCE));
  }
}
