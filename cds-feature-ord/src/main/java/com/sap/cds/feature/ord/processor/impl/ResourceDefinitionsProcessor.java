/*
 * © 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cds.feature.ord.processor.impl;

import static com.sap.cds.feature.ord.common.Utils.CdsRuntimeProperties.getOrdProperties;
import static com.sap.cds.feature.ord.common.Utils.Streams.asStream;
import static org.apache.commons.io.FilenameUtils.concat;

import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.sap.cds.feature.ord.processor.CdsOrdNodeProcessor;
import com.sap.cds.feature.ord.provider.AuthenticationManagerProvider;
import com.sap.cds.services.runtime.CdsRuntime;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Processor for resource definitions in the CDS ORD service that (a) removes all openApi resource
 * definitions because there is no runtime support for openApi documents (b) replaces
 * "accessStrategies" element to comply with the configured security model for metadata endpoints.
 */
public class ResourceDefinitionsProcessor implements CdsOrdNodeProcessor {

  private CdsRuntime cdsRuntime;

  @Override
  public void setCdsRuntime(CdsRuntime cdsRuntime) {
    this.cdsRuntime = cdsRuntime;
  }

  @Override
  public Predicate<String> predicate() {
    return "resourceDefinitions"::equals;
  }

  @Override
  public <T extends TreeNode> Optional<T> process(String nodeName, T resourceDefinitions) {
    return Optional.ofNullable(resourceDefinitions) //
        .map(ArrayNode.class::cast) //
        .map(this::process);
  }

  @SuppressWarnings("unchecked")
  private <T extends TreeNode> T process(ArrayNode resourceDefinitions) {
    ArrayNode result = JsonNodeFactory.instance.arrayNode();
    String apiRoot = getOrdProperties(cdsRuntime).getDocumentsEndpoint().getPath();
    AuthenticationManagerProvider provider = cdsRuntime.getProvider(AuthenticationManagerProvider.class);
    JsonNode accessStrategiesNode = asAccessStrategiesNode(provider.getAccessStrategies());

    asStream(resourceDefinitions)
        .forEach(resourceDefinition -> result.add(resourceDefinition
            .<ObjectNode>deepCopy()
            .<ObjectNode>set("accessStrategies", accessStrategiesNode.deepCopy())
            .<ObjectNode>set(
                "url",
                asTextNode(concat(
                    apiRoot, resourceDefinition.get("url").asText())))));

    return (T) result;
  }

  private static TextNode asTextNode(String value) {
    return JsonNodeFactory.instance.textNode(value);
  }

  private static JsonNode asAccessStrategiesNode(List<String> accessStrategies) {
    return JsonNodeFactory.instance
        .arrayNode()
        .addAll(accessStrategies.stream()
            .map(strategy -> JsonNodeFactory.instance.objectNode().put("type", strategy))
            .toList());
  }
}
