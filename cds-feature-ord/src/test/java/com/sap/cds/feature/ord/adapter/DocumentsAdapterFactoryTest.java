/*
 * © 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cds.feature.ord.adapter;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sap.cds.adapter.UrlResourcePath;
import com.sap.cds.feature.ord.servlet.DocumentsServlet;
import com.sap.cds.services.runtime.CdsRuntimeConfigurer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DocumentsAdapterFactoryTest {

  private DocumentsAdapterFactory classUnderTest;

  @BeforeEach
  void setUp() {
    classUnderTest = new DocumentsAdapterFactory();

    classUnderTest.setCdsRuntime(CdsRuntimeConfigurer.create().complete());
  }

  @Test
  void whenCreateIsCalled_thenCorrectResultIsReturned() {
    assertInstanceOf(DocumentsServlet.class, classUnderTest.create());
  }

  @Test
  void whenIsEnabledIsCalled_thenCorrectResultIsReturned() {
    assertTrue(classUnderTest.isEnabled()); // default value
  }

  @Test
  void whenGetMappingsIsCalled_thenCorrectResultIsReturned() {
    assertArrayEquals(new String[] {"/ord/v1/**"}, classUnderTest.getMappings());
  }

  @Test
  void whenGetBasePathIsCalled_thenCorrectResultIsReturned() {
    assertEquals("/ord/v1", classUnderTest.getBasePath());
  }

  @Test
  void whenGetServletPathIsCalled_thenCorrectResultIsReturned() {
    UrlResourcePath found = classUnderTest.getServletPath();

    assertTrue(found.isPublic());
    assertTrue(found.subPaths().toList().isEmpty());
    assertTrue(found.publicEvents().toList().isEmpty());
    assertEquals("/ord/v1/**", found.getPath());
  }
}
