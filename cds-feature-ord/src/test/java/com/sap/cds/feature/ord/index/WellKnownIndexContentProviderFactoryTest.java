/*
 * © 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cds.feature.ord.index;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sap.cds.adapter.IndexContentProvider;
import com.sap.cds.services.runtime.CdsRuntime;
import com.sap.cds.services.runtime.CdsRuntimeConfigurer;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WellKnownIndexContentProviderFactoryTest {

  private static final String EXPECTED_INDEX_CONTENT = """
<h3 class="header">
<a href="/.well-known/open-resource-discovery"><span>/.well-known/open-resource-discovery</span></a>
</h3>
""";

  private CdsRuntime cdsRuntime;

  private WellKnownIndexContentProviderFactory classUnderTest;

  @BeforeEach
  void setUp() {
    cdsRuntime = CdsRuntimeConfigurer.create().complete();
    classUnderTest = new WellKnownIndexContentProviderFactory();
    classUnderTest.setCdsRuntime(cdsRuntime);
  }

  @Test
  void testEnabled() {
    cdsRuntime
        .getEnvironment()
        .getCdsProperties()
        .getOrd()
        .getWellKnownEndpoint()
        .setEnabled(true);

    assertTrue(classUnderTest.isEnabled());
  }

  @Test
  void testDisabled() {
    cdsRuntime
        .getEnvironment()
        .getCdsProperties()
        .getOrd()
        .getWellKnownEndpoint()
        .setEnabled(false);

    assertFalse(classUnderTest.isEnabled());
  }

  @Test
  void testCreate() {
    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter(stringWriter);

    IndexContentProvider created = classUnderTest.create();
    created.writeContent(printWriter, "/context");

    assertEquals("Open Resource Discovery well-known endpoints", created.getSectionTitle());
    assertEquals(EXPECTED_INDEX_CONTENT, stringWriter.toString());
  }
}
