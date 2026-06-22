/*
 * © 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cds.feature.ord.servlet;

import static com.sap.cds.feature.ord.common.Constants.HEADER_LOCAL_TENANT_ID;
import static com.sap.cds.feature.ord.common.Utils.Http.handleException;
import static jakarta.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static jakarta.servlet.http.HttpServletResponse.SC_OK;
import static org.apache.commons.io.FilenameUtils.getExtension;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;
import static org.apache.http.entity.ContentType.APPLICATION_XML;
import static org.apache.http.entity.ContentType.TEXT_PLAIN;

import com.sap.cds.feature.ord.provider.AuthenticationManagerProvider;
import com.sap.cds.feature.ord.provider.OrdResourcesProvider;
import com.sap.cds.services.runtime.CdsRuntime;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.entity.ContentType;

@Slf4j
@RequiredArgsConstructor
public class DocumentsServlet extends HttpServlet {

  private final transient CdsRuntime cdsRuntime;

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      log.debug("Received request for Open Resource Discovery(ORD) Document");

      process(request, response);
    } catch (Exception exception) {
      handleException(response, exception);
    }
  }

  private void process(HttpServletRequest request, HttpServletResponse response) {
    cdsRuntime.getProvider(AuthenticationManagerProvider.class).checkAuthorization(request.getRequestURI());

    cdsRuntime
        .requestContext()
        .systemUser(request.getHeader(HEADER_LOCAL_TENANT_ID))
        .run(rc -> {
          try {
            processDocument(request, response);
          } catch (Exception exception) {
            handleException(response, exception);
          }
        });
  }

  private void processDocument(HttpServletRequest request, HttpServletResponse response) throws IOException {
    String path = normalize(request.getPathInfo());
    String perspective = request.getParameter("perspective");
    ContentType contentType = determineContentType(request.getPathInfo());
    OrdResourcesProvider ordResourcesProvider = cdsRuntime.getProvider(OrdResourcesProvider.class);

    try (InputStream stream = ordResourcesProvider.read(path, perspective)) {
      if (stream == null) {
        response.setStatus(SC_NOT_FOUND);
        return;
      }

      response.setStatus(SC_OK);
      response.setContentType(contentType.getMimeType());
      response.setCharacterEncoding(contentType.getCharset().toString());

      stream.transferTo(response.getOutputStream());
    }
  }

  private static String normalize(String path) {
    return path.startsWith("/") ? path.substring(1) : path;
  }

  private static ContentType asContentType(String extension) {
    return switch (extension) {
      case "graphql" -> TEXT_PLAIN;
      case "edmx" -> APPLICATION_XML;
      case "json" -> APPLICATION_JSON;
      default -> throw new IllegalArgumentException("Unknown extension: " + extension);
    };
  }

  private static ContentType determineContentType(String path) {
    return asContentType(Objects.equals("documents/ord-document", normalize(path)) ? "json" : getExtension(path));
  }
}
