package com.sap.cds.feature.ord.resolver;

import com.sap.cds.services.runtime.CdsRuntimeAware;
import java.io.InputStream;

/**
 * Resolves ORD documents and resource definitions.
 *
 * <p>Resolver implementations receive an injected {@link
 * com.sap.cds.services.runtime.CdsRuntime} through {@link
 * CdsRuntimeAware#setCdsRuntime(com.sap.cds.services.runtime.CdsRuntime)}. The injected runtime
 * provides access to the CAP APIs.
 *
 * <p>The ORD feature ensures that resolvers are always called with a valid CAP {@link
 * com.sap.cds.services.request.RequestContext}. For the {@code system-instance} perspective, the
 * request context contains the correct subscribed tenant ID, which can be accessed via
 * {@code RequestContext.getCurrent(runtime).getUserInfo().getTenant()}, where {@code runtime} is
 * the injected {@code CdsRuntime}.
 */
public interface OrdResolver extends CdsRuntimeAware {

  /**
   * Returns the resolver's priority among applicable resolvers. The ORD feature selects the
   * applicable resolver with the lowest order value.
   *
   * @return the order value; lower values have higher priority
   */
  int order();

  /**
   * Resolves the requested ORD document or resource definition for the given perspective.
   * Called when this resolver is selected from those for which {@link #isApplicable(String,
   * String)} returns {@code true}.
   *
   * @param resource the requested resource path, without a leading slash
   * @param perspective the requested ORD perspective, such as {@code system-version} or
   *     {@code system-instance}
   * @return an input stream containing the resolved content; the caller closes the stream
   */
  InputStream resolve(String resource, String perspective);

  /**
   * Determines whether this resolver can handle the requested resource and perspective.
   *
   * @param resource the requested resource path, without a leading slash
   * @param perspective the requested ORD perspective, such as {@code system-version} or
   *     {@code system-instance}
   * @return {@code true} if this resolver can handle the request; {@code false} otherwise
   */
  boolean isApplicable(String resource, String perspective);
}
