/*
 * © 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cds.feature.ord.provider;

import com.sap.cds.feature.ord.model.OrdDocument;
import com.sap.cds.services.runtime.CdsProvider;
import java.io.InputStream;

public interface OrdResourcesProvider extends CdsProvider<OrdResourcesProvider> {

  /**
   * Provides the {@link OrdDocument} for the given CAP service.
   *
   * @return InputStream
   */
  InputStream read(String resource, String perspective);
}
