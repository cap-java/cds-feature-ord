/*
 * © 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cds.feature.ord.core.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.TextNode;
import com.sap.cds.feature.ord.core.customizers.impl.EntryPointsCustomizer;
import com.sap.cds.services.environment.CdsProperties;
import com.sap.cds.services.impl.environment.SimplePropertiesProvider;
import com.sap.cds.services.runtime.CdsRuntime;
import com.sap.cds.services.runtime.CdsRuntimeConfigurer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EntryPointsProcessorTest {

  private CdsRuntime runtime;
  private JsonStreamContext jsonStreamContext;
  private EntryPointsCustomizer classUnderTest;

  @BeforeEach
  void setUp() {
    CdsProperties properties = new CdsProperties();
    properties.getOdataV4().getEndpoint().setPath("/api");

    this.jsonStreamContext = mock(JsonStreamContext.class);
    this.runtime = CdsRuntimeConfigurer.create(new SimplePropertiesProvider(properties))
        .serviceConfigurations()
        .eventHandlerConfigurations()
        .complete();

    this.classUnderTest = new EntryPointsCustomizer(this.runtime);
  }

  @Test
  void testPredicate() {
    doReturn("entryPoints").when(jsonStreamContext).getCurrentName();

    assertThat(this.classUnderTest.predicate().test(this.jsonStreamContext), is(true));
  }

  @Test
  void testPredicate_Negative() {
    doReturn("resourceDefinitions").when(jsonStreamContext).getCurrentName();

    assertThat(this.classUnderTest.predicate().test(this.jsonStreamContext), is(false));
  }

  @Test
  void testProcess() {
    ArrayNode node = JsonNodeFactory.instance.arrayNode().add(new TextNode("/odata/v4/admin"));

    var entryPoints = this.classUnderTest.customize("entryPoints", node);

    assertThat(entryPoints.toString(), is("[\"/api/admin\"]"));
  }
}
