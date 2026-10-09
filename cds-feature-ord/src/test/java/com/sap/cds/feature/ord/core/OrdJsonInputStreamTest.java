/*
 * © 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cds.feature.ord.core;

import static com.sap.cds.feature.ord.common.Utils.CdsRuntimeProperties.getOrdProperties;
import static com.sap.cds.feature.ord.common.Utils.Resources.getResourceAsStream;
import static com.sap.cds.services.utils.cert.UclAuthUtils.AccessStrategy.MTLS;
import static org.apache.commons.io.FilenameUtils.concat;
import static org.apache.commons.io.FilenameUtils.getBaseName;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.JsonNode;
import com.sap.cds.feature.ord.provider.AuthenticationManagerProvider;
import com.sap.cds.feature.ord.resolver.impl.StaticOrdDocumentResolver;
import com.sap.cds.impl.parser.JsonParser;
import com.sap.cds.services.runtime.CdsRuntime;
import com.sap.cds.services.runtime.CdsRuntimeConfigurer;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrdJsonInputStreamTest {

  private CdsRuntime cdsRuntime;
  private AuthenticationManagerProvider authenticationManagerProvider;

  @BeforeEach
  void setUp() {
    this.authenticationManagerProvider = mock(AuthenticationManagerProvider.class);
    this.cdsRuntime = CdsRuntimeConfigurer.create()
        .provider(authenticationManagerProvider)
        .complete();

    doReturn(List.of(MTLS.getValue())).when(authenticationManagerProvider).getAccessStrategies();
  }

  @Test
  void processingOrdDocumentIsSuccessful() throws IOException {
    String ordDocumentPath = getOrdProperties(cdsRuntime).getOrdDocumentPath();
    String document = concat("documents", getBaseName(ordDocumentPath));
    StaticOrdDocumentResolver provider = new StaticOrdDocumentResolver(cdsRuntime);

    try (InputStream is = provider.resolve(document)) {
      JsonNode actual = JsonParser.parseJson(new String(is.readAllBytes()));
      JsonNode expected = load("ord/ord-document-target.json");

      Assertions.assertEquals(expected.toPrettyString(), actual.toPrettyString());
    }
  }

  private static JsonNode load(String path) throws IOException {
    try (InputStreamReader reader = new InputStreamReader(getResourceAsStream(path))) {
      return JsonParser.parseJson(reader);
    }
  }
}
