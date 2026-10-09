/*
 * © 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cds.feature.ord.core;

import static com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_SINGLE_QUOTES;
import static com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES;
import static com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS;
import static com.google.common.primitives.Bytes.asList;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.ObjectUtils.firstNonNull;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.cds.feature.ord.core.customizers.CdsOrdNodeCustomizer;
import com.sap.cds.feature.ord.core.generators.CdsOrdNodeGenerator;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

public class OrdJsonInputStream extends InputStream {

  // Central Object Mapper is wrapped in a class in cds4j which doesn't provide an API that we need.
  private static final ObjectMapper MAPPER = new ObjectMapper()
      .enable(ALLOW_SINGLE_QUOTES)
      .enable(ALLOW_UNQUOTED_FIELD_NAMES)
      .configure(USE_BIG_DECIMAL_FOR_FLOATS, true);

  private final JsonParser parser;
  private final InputStream inputStream;
  private final List<CdsOrdNodeGenerator> generators;
  private final List<CdsOrdNodeCustomizer> customizers;

  private JsonToken token;
  private JsonToken previous;
  private final Deque<Byte> buffer = new ArrayDeque<>();

  public OrdJsonInputStream(
      InputStream inputStream, List<CdsOrdNodeGenerator> generators, List<CdsOrdNodeCustomizer> customizers)
      throws IOException {
    this.previous = null;
    this.generators = List.copyOf(generators);
    this.customizers = List.copyOf(customizers);
    this.inputStream = new BufferedInputStream(inputStream);
    this.parser = MAPPER.getFactory().createParser(this.inputStream);
    this.token = this.parser.nextToken();
  }

  @Override
  public void close() throws IOException {
    parser.close();
    inputStream.close();
  }

  @Override
  public int read() throws IOException {
    if (!buffer.isEmpty()) {
      // Return the first byte from the buffer as unsigned byte and remove it from the buffer
      return buffer.removeFirst() & 0xFF;
    }

    if (isNull(token) || parser.isClosed()) {
      return -1;
    }

    return switch (token) {
      case FIELD_NAME -> handleFieldName();
      case END_ARRAY, END_OBJECT -> handleStructEnd();
      case START_ARRAY, START_OBJECT -> handleStructStart();
      default -> handleFieldValue();
    };
  }

  private void appendToBuffer(String... values) {
    Arrays.stream(values) //
        .map(value -> value.getBytes(UTF_8)) //
        .forEach(value -> buffer.addAll(asList(value)));
  }

  private int handleFieldName() throws IOException {
    String current = parser.currentName();
    List<CdsOrdNodeCustomizer> customizers = lookupCustomizers(parser.getParsingContext());

    appendToBuffer(isStructStart(previous) ? "" : ",", format("\"%s\": ", current));
    if (!customizers.isEmpty() && nonNull(parser.nextToken())) {
      appendToBuffer(process(current, parser.readValueAsTree(), customizers));
    }

    previous = firstNonNull(parser.currentToken(), parser.getLastClearedToken());
    token = parser.nextToken();

    return buffer.removeFirst() & 0xFF;
  }

  private int handleStructEnd() throws IOException {
    JsonStreamContext context = parser.getParsingContext();
    String generated = generators.stream()
        .filter(generator -> generator.predicate().test(context))
        .map(generator -> generator.generate(context))
        .collect(joining(", "));

    appendToBuffer((isStructStart(previous) || isEmpty(generated) ? "" : ","), generated, token.asString());

    previous = firstNonNull(parser.currentToken(), parser.getLastClearedToken());
    token = parser.nextToken();

    return buffer.removeFirst() & 0xFF;
  }

  private int handleFieldValue() throws IOException {
    appendToBuffer(
        isScalarValue(previous) ? "," : "",
        !isValueString(token) ? parser.getValueAsString() : format("\"%s\"", parser.getValueAsString()));

    previous = firstNonNull(parser.currentToken(), parser.getLastClearedToken());
    token = parser.nextToken();

    return buffer.removeFirst() & 0xFF;
  }

  private int handleStructStart() throws IOException {
    appendToBuffer(isStructEnd(previous) ? "," : "", token.asString());

    previous = firstNonNull(parser.currentToken(), parser.getLastClearedToken());
    token = parser.nextToken();

    return buffer.removeFirst() & 0xFF;
  }

  private List<CdsOrdNodeCustomizer> lookupCustomizers(JsonStreamContext context) {
    return customizers.stream() //
        .filter(processor -> processor.predicate().test(context)) //
        .toList();
  }

  private static boolean isStructEnd(JsonToken token) {
    return nonNull(token) && token.isStructEnd();
  }

  private static boolean isValueString(JsonToken token) {
    return JsonToken.VALUE_STRING == token;
  }

  private static boolean isScalarValue(JsonToken token) {
    return nonNull(token) && token.isScalarValue();
  }

  private static boolean isStructStart(JsonToken token) {
    return nonNull(token) && token.isStructStart();
  }

  private static String process(String name, JsonNode node, List<CdsOrdNodeCustomizer> customizers) {
    return customizers.stream()
        .reduce(node, (current, processor) -> processor.customize(name, current), (a, b) -> a)
        .toPrettyString();
  }
}
