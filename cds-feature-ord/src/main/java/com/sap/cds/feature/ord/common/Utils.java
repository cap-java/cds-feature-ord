/*
 * © 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cds.feature.ord.common;

import static com.sap.cds.feature.ord.common.Utils.Exceptions.asHttpStatus;
import static com.sap.cds.feature.ord.common.Utils.Exceptions.asLocalizedErrorMessage;
import static com.sap.cds.services.utils.CdsErrorStatuses.ERROR_READING_ORD_DOCUMENT;
import static java.lang.Thread.currentThread;
import static java.util.Objects.nonNull;
import static java.util.Spliterator.ORDERED;
import static java.util.Spliterators.spliteratorUnknownSize;

import com.sap.cds.feature.ord.processor.CdsOrdNodeProcessor;
import com.sap.cds.services.ServiceException;
import com.sap.cds.services.environment.CdsProperties.Model.Provider;
import com.sap.cds.services.environment.CdsProperties.ODataV4;
import com.sap.cds.services.environment.CdsProperties.OpenResourceDiscovery;
import com.sap.cds.services.environment.CdsProperties.Security.Authentication;
import com.sap.cds.services.runtime.CdsRuntime;
import com.sap.cds.services.utils.ErrorStatusException;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.http.HttpStatus;

@UtilityClass
public class Utils {

  @Slf4j
  @UtilityClass
  public static class Http {

    public static void respondWith(HttpServletResponse response, String payload) {
      try (PrintWriter writer = response.getWriter()) {
        writer.write(payload);
      } catch (IOException exception) {
        throw new ServiceException("Failed to write response", exception);
      }
    }

    public static void handleException(HttpServletResponse response, Throwable throwable) {
      int status = asHttpStatus(throwable);

      if (status >= 500 && status < 600) {
        log.error("Unexpected error", throwable);
      } else {
        log.debug("Service exception thrown", throwable);
      }

      response.setStatus(status);
      Optional.ofNullable(asLocalizedErrorMessage(throwable))
          .ifPresent(message -> respondWith(response, message));
    }

    public static ClassicHttpResponse assertSuccessful(ClassicHttpResponse response) {
      if (response.getCode() < 200 || response.getCode() >= 300) {
        throw new ServiceException("Request failed: {} - {}", response.getCode(), response.getReasonPhrase());
      }

      return response;
    }
  }

  @UtilityClass
  public static class Streams {

    public static <T> Stream<T> asStream(Iterable<T> iterable) {
      return StreamSupport.stream(iterable.spliterator(), false);
    }

    public static <T> List<T> asList(Iterator<T> iterator) {
      return StreamSupport.stream(spliteratorUnknownSize(iterator, ORDERED), false)
          .toList();
    }
  }

  @UtilityClass
  public static class Resources {

    public static boolean resourceExists(String path) {
      return nonNull(path)
          && nonNull(currentThread().getContextClassLoader().getResource(path));
    }

    public static InputStream getResourceAsStream(String path) {
      return !resourceExists(path)
          ? null
          : currentThread().getContextClassLoader().getResourceAsStream(path);
    }

    public static InputStream asOrdJsonInputStream(
        InputStream inputStream, List<CdsOrdNodeProcessor> cdsOrdNodeProcessors) {
      try {
        return new OrdJsonInputStream(inputStream, cdsOrdNodeProcessors);
      } catch (IOException exception) {
        throw new ErrorStatusException(ERROR_READING_ORD_DOCUMENT, "documents/ord-document", exception);
      }
    }
  }

  public static class Exceptions {

    private Exceptions() {}

    public static int asHttpStatus(Throwable throwable) {
      if (throwable instanceof ServiceException exception) {
        return exception.getErrorStatus().getHttpStatus();
      }

      return HttpStatus.SC_INTERNAL_SERVER_ERROR;
    }

    public static String asLocalizedErrorMessage(Throwable throwable) {
      if (throwable instanceof ServiceException exception) {
        return exception.getLocalizedMessage();
      }

      return null;
    }
  }

  public static class CdsRuntimeProperties {

    private CdsRuntimeProperties() {}

    public static ODataV4 getOdataV4Properties(CdsRuntime cdsRuntime) {
      return cdsRuntime.getEnvironment().getCdsProperties().getOdataV4();
    }

    public static Provider getModelProviderProperties(CdsRuntime cdsRuntime) {
      return cdsRuntime.getEnvironment().getCdsProperties().getModel().getProvider();
    }

    public static OpenResourceDiscovery getOrdProperties(CdsRuntime cdsRuntime) {
      return cdsRuntime.getEnvironment().getCdsProperties().getOrd();
    }

    public static Authentication getAuthenticationProperties(CdsRuntime cdsRuntime) {
      return cdsRuntime.getEnvironment().getCdsProperties().getSecurity().getAuthentication();
    }
  }
}
