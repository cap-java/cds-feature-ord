/*
 * © 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cds.feature.ord.provider;

import com.google.common.annotations.Beta;
import com.sap.cds.services.runtime.CdsProvider;
import java.util.List;

@Beta
public interface AuthenticationManagerProvider extends CdsProvider<AuthenticationManagerProvider> {

  void checkAuthorization(String uri);

  List<String> getAccessStrategies();
}
