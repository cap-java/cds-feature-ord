/*
 * © 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cds.feature.ord.processor.impl;

import static com.sap.cds.feature.ord.common.Utils.Resources.getResourceAsStream;
import static com.sap.cds.services.utils.cert.UclAuthUtils.AccessStrategy.MTLS;
import static com.sap.cds.services.utils.cert.UclAuthUtils.AccessStrategy.OPEN;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sap.cds.feature.ord.provider.AuthenticationManagerProvider;
import com.sap.cds.impl.parser.JsonParser;
import com.sap.cds.services.runtime.CdsRuntime;
import com.sap.cds.services.runtime.CdsRuntimeConfigurer;
import java.io.IOException;
import java.util.List;
import java.util.function.Predicate;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ResourceDefinitionsProcessorTest {

  private ObjectNode ord;
  private CdsRuntime runtime;
  private ArrayNode resourceDefinitions;
  private AuthenticationManagerProvider authenticationManagerProvider;

  @BeforeEach
  void prepare() throws IOException {
    this.authenticationManagerProvider = mock(AuthenticationManagerProvider.class);
    this.ord = JsonParser.parseJson(IOUtils.toString(getResourceAsStream("ord/ord-document.json"), UTF_8));
    this.resourceDefinitions = (ArrayNode) ord.at("/apiResources/0/resourceDefinitions");
    this.runtime = CdsRuntimeConfigurer.create()
        .serviceConfigurations()
        .eventHandlerConfigurations()
        .provider(authenticationManagerProvider)
        .complete();
  }

  @Test
  void testPredicate() {
    var accessStrategyProcessor = new ResourceDefinitionsProcessor();
    accessStrategyProcessor.setCdsRuntime(this.runtime);
    Predicate<String> predicate = accessStrategyProcessor.predicate();

    assertThat(predicate.test("resourceDefinitions"), is(true));
  }

  @Test
  void testPredicate_Negative() {
    var accessStrategyProcessor = new ResourceDefinitionsProcessor();
    accessStrategyProcessor.setCdsRuntime(this.runtime);
    Predicate<String> predicate = accessStrategyProcessor.predicate();

    assertThat(predicate.test("integrationDependencies"), is(false));
  }

  @Test
  void testProcess_AccessStrategy_mTLS() {
    doReturn(List.of(MTLS.getValue())).when(authenticationManagerProvider).getAccessStrategies();

    var resourceDefinitionsProcessor = new ResourceDefinitionsProcessor();
    resourceDefinitionsProcessor.setCdsRuntime(this.runtime);
    var newResourceDefinitions = resourceDefinitionsProcessor.process("resourceDefinitions", resourceDefinitions);

    newResourceDefinitions.get().forEach(resourceDefinition -> {
      ArrayNode strategies = (ArrayNode) resourceDefinition.get("accessStrategies");
      assertThat(strategies.toString(), is("[{\"type\":\"sap:cmp-mtls:v1\"}]"));
    });
  }

  @Test
  void testProcess_AccessStrategy_Open() {
    doReturn(List.of(OPEN.getValue())).when(authenticationManagerProvider).getAccessStrategies();
    this.runtime
        .getEnvironment()
        .getCdsProperties()
        .getSecurity()
        .getAuthentication()
        .setAuthenticateMetadataEndpoints(false);

    var resourceDefinitionsProcessor = new ResourceDefinitionsProcessor();
    resourceDefinitionsProcessor.setCdsRuntime(this.runtime);
    var newResourceDefinitions = resourceDefinitionsProcessor.process("resourceDefinitions", resourceDefinitions);

    newResourceDefinitions.get().forEach(resourceDefinition -> {
      ArrayNode strategies = (ArrayNode) resourceDefinition.get("accessStrategies");
      assertThat(strategies.toString(), is("[{\"type\":\"open\"}]"));
    });
  }
}
