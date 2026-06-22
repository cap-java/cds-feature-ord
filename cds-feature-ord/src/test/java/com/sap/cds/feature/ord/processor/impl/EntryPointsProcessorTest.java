/*
 * © 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cds.feature.ord.processor.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.sap.cds.services.environment.CdsProperties;
import com.sap.cds.services.impl.environment.SimplePropertiesProvider;
import com.sap.cds.services.runtime.CdsRuntime;
import com.sap.cds.services.runtime.CdsRuntimeConfigurer;
import java.util.function.Predicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EntryPointsProcessorTest {

  private CdsRuntime runtime;

  @BeforeEach
  void prepare() {
    CdsProperties properties = new CdsProperties();
    properties.getOdataV4().getEndpoint().setPath("/api");

    this.runtime = CdsRuntimeConfigurer.create(new SimplePropertiesProvider(properties))
        .serviceConfigurations()
        .eventHandlerConfigurations()
        .complete();
  }

  @Test
  void testPredicate() {
    var entryPointsProcessor = new EntryPointsProcessor();
    entryPointsProcessor.setCdsRuntime(this.runtime);
    Predicate<String> predicate = entryPointsProcessor.predicate();

    assertThat(predicate.test("entryPoints"), is(true));
  }

  @Test
  void testPredicate_Negative() {
    var entryPointsProcessor = new EntryPointsProcessor();
    entryPointsProcessor.setCdsRuntime(this.runtime);
    Predicate<String> predicate = entryPointsProcessor.predicate();

    assertThat(predicate.test("integrationDependencies"), is(false));
  }

  @Test
  void testProcess() {
    var entryPointsProcessor = new EntryPointsProcessor();
    entryPointsProcessor.setCdsRuntime(this.runtime);
    ArrayNode node = JsonNodeFactory.instance.arrayNode();
    node.add(JsonNodeFactory.instance.textNode("/odata/v4/admin"));
    var entryPoints = entryPointsProcessor.process("entryPoints", node);

    assertThat(entryPoints.orElseThrow().toString(), is("[\"/api/admin\"]"));
  }
}
