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

class DocumentsIndexContentProviderFactoryTest {

  private static final String EXPECTED_INDEX_CONTENT = """
<h3 class="header">
<a href="/ord/v1/"><span>/ord/v1</span></a>
</h3>
<ul>
<li>
<div><a href="/ord/v1/ord-document.json"><span>/ord-document.json</span></a></div>
</li>
</ul>
""";

  private CdsRuntime cdsRuntime;

  private DocumentsIndexContentProviderFactory classUnderTest;

  @BeforeEach
  void setUp() {
    cdsRuntime = CdsRuntimeConfigurer.create().complete();
    classUnderTest = new DocumentsIndexContentProviderFactory();
    classUnderTest.setCdsRuntime(cdsRuntime);
  }

  @Test
  void testEnabled() {
    cdsRuntime
        .getEnvironment()
        .getCdsProperties()
        .getOrd()
        .getDocumentsEndpoint()
        .setEnabled(true);

    assertTrue(classUnderTest.isEnabled());
  }

  @Test
  void testDisabled() {
    cdsRuntime
        .getEnvironment()
        .getCdsProperties()
        .getOrd()
        .getDocumentsEndpoint()
        .setEnabled(false);

    assertFalse(classUnderTest.isEnabled());
  }

  @Test
  void testCreate() {
    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter(stringWriter);

    IndexContentProvider created = classUnderTest.create();
    created.writeContent(printWriter, "/context");

    assertEquals("Open Resource Discovery documents endpoints", created.getSectionTitle());
    assertEquals(EXPECTED_INDEX_CONTENT, stringWriter.toString());
  }
}
