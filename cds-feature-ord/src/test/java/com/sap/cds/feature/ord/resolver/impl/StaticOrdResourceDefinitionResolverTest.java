/*
 * © 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cds.feature.ord.resolver.impl;

import static com.sap.cds.feature.ord.common.Constants.PERSPECTIVE_SYSTEM_VERSION;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.sap.cds.adapter.edmx.EdmxV4Provider;
import com.sap.cds.services.runtime.CdsRuntimeConfigurer;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StaticOrdResourceDefinitionResolverTest {

  private StaticOrdResourceDefinitionResolver classUnderTest;

  @BeforeEach
  void setUp() {
    classUnderTest = new StaticOrdResourceDefinitionResolver(
        CdsRuntimeConfigurer.create().complete());
  }

  @Test
  void resolvesClasspathResource() throws IOException {
    assertEquals(Integer.MAX_VALUE, classUnderTest.order());
    try (var result = classUnderTest.resolve("test_ord_service/test.oas3.json", PERSPECTIVE_SYSTEM_VERSION)) {
      assertEquals("{}", new String(result.readAllBytes(), UTF_8));
    }
  }

  @Test
  void delegatesEdmxResourcesToProvider() throws IOException {
    EdmxV4Provider provider = mock(EdmxV4Provider.class);
    var expected = new ByteArrayInputStream("edmx".getBytes(UTF_8));
    doReturn(expected).when(provider).getEdmx("AdminService");
    classUnderTest.setCdsRuntime(
        CdsRuntimeConfigurer.create().provider(provider).complete());

    assertEquals(expected, classUnderTest.resolve("AdminService.edmx", PERSPECTIVE_SYSTEM_VERSION));
    verify(provider).getEdmx("AdminService");
  }

  @Test
  void appliesToExistingNonDocumentResourcesForSupportedPerspectives() {
    assertTrue(classUnderTest.isApplicable("test_ord_service/test.oas3.json", PERSPECTIVE_SYSTEM_VERSION));
    assertTrue(classUnderTest.isApplicable("test_ord_service/test.oas3.json", null));
    assertFalse(classUnderTest.isApplicable("missing.json", PERSPECTIVE_SYSTEM_VERSION));
    assertFalse(classUnderTest.isApplicable("test_ord_service/test.oas3.json", "system-instance"));
    assertFalse(classUnderTest.isApplicable("documents/ord-document", PERSPECTIVE_SYSTEM_VERSION));
  }
}
