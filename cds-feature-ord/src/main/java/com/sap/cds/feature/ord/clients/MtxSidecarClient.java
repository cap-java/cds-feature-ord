/*
 * © 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cds.feature.ord.clients;

public interface MtxSidecarClient {

  byte[] getOrdDocument();

  byte[] getOrdResourceDefinition(String resource);
}
