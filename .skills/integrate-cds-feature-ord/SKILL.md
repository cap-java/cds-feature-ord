---
name: integrate-cds-feature-ord
description: Integrate the com.sap.cds:cds-feature-ord plugin into an existing CAP Java project. Use when the user asks to add ORD support, configure ORD endpoints, set up the ORD document, extend resource resolution with OrdResolver, or asks what configuration options are available for the Java ORD plugin.
allowed-tools:
  - Read
  - Write
  - Edit
  - Bash(mvn *)
  - Bash(cds *)
  - Bash(npm *)
  - Bash(ls *)
  - Bash(curl *)
---

# /integrate-cds-feature-ord

Integrates `com.sap.cds:cds-feature-ord` into an existing CAP Java project to expose Open Resource Discovery (ORD) metadata endpoints.

Arguments passed: `$ARGUMENTS`

---

## Dispatch on arguments

| Argument | Action |
| -------- | ------ |
| *(none)* | Full integration: Steps 1 → 2a → 3 → 4 → 5; include Step 2b when the project uses a dynamic model |
| `maven`  | Add Maven dependency only → Step 1 |
| `config` | Configure Java runtime properties only (endpoints, classpath paths, OData V4 path) → Step 3 |
| `auth`   | Configure authentication only → Step 4 |
| `mtx`    | Configure MTX sidecar integration only → Step 2b |
| `resolver` | Extend document or resource definition resolution → Custom resolvers |

---

## Before starting

Inspect the files relevant to the requested action. For a full integration, establish the following:

1. Run `ls` on the project root to confirm the module layout (single-module vs multi-module with `srv/`, `db/` etc.).
2. Read the root `pom.xml` — check whether a `<dependencyManagement>` BOM already exists.
3. Read `.cdsrc.json` if it exists — check whether a `build.tasks` array is already present.
4. Read the service module's `pom.xml` — check whether `cds-maven-plugin` executions already drive `cds build` commands.
5. Check the CAP Java version and whether the project's BOM already manages `cds-feature-ord`. Preserve a compatible managed version. When adding or upgrading an unmanaged dependency, discover available releases from Maven Central:
   ```bash
   curl -fsSL "https://search.maven.org/solrsearch/select?q=g:com.sap.cds+AND+a:cds-feature-ord&rows=20&core=gav&wt=json"
   ```
6. Check existing `@sap/cds`, `@cap-js/ord`, and lockfile versions in the CDS project and sidecar. When a new dependency is needed, discover the current npm release and compatibility requirements:
   ```bash
   npm view @cap-js/ord version
   npm view @cap-js/ord engines peerDependencies
   ```

Use compatible versions wherever `ORD_MAVEN_VERSION` and `ORD_NPM_VERSION` appear below. These are placeholders to replace, not literal dependency versions. Maven and npm packages have independent version numbers; matching them numerically is not required. Check release notes and the selected Maven artifact's dependency requirements against the application's CAP runtime. Confirm that the selected release exposes `OrdResolver` before using the custom resolver instructions; this checkout may contain changes not yet published.

For `config` or `auth`, inspect current runtime configuration without changing dependencies or querying registries. For `resolver`, inspect the installed Java API and service module. Perform version lookups only when dependency changes are needed.

---

## Step 1 — Add the Maven dependency

### Single-module project

Add directly to the service module's `pom.xml`:

If the existing BOM or parent already manages this dependency, omit the explicit version. Otherwise use the compatible version selected above:

```xml
<dependency>
  <groupId>com.sap.cds</groupId>
  <artifactId>cds-feature-ord</artifactId>
  <version>ORD_MAVEN_VERSION</version>
</dependency>
```

### Multi-module project (recommended pattern)

If the dependency is not already managed, merge its version into the existing root `pom.xml` `<dependencyManagement>`; preserve imported BOMs and other entries:

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>com.sap.cds</groupId>
      <artifactId>cds-feature-ord</artifactId>
      <version>ORD_MAVEN_VERSION</version>
    </dependency>
  </dependencies>
</dependencyManagement>
```

Then add the dependency (without version) in the service module's `pom.xml`:

```xml
<dependency>
  <groupId>com.sap.cds</groupId>
  <artifactId>cds-feature-ord</artifactId>
</dependency>
```

**The plugin activates automatically via Java `ServiceLoader` — no Spring `@EnableXxx` annotation or `application.yaml` activation key is needed.**

---

## Step 2a — Generate ORD resources at build time

By default, the plugin reads a static ORD document from the classpath at `ord/ord-document.json`. Generate this file during the Maven build using the CDS build tool. Applications that supply documents through a custom `OrdResolver` can replace this default resolution (see Custom resolvers).

Use whichever build mechanism the project already uses (identified in "Before starting"). An existing Maven execution of `cds build` can run an ORD task configured in `.cdsrc.json`; in that case, no separate ORD execution is needed. Ensure the Maven build runs ORD generation once, before resource copying, with a consistent output path.

**Prerequisite — `@cap-js/ord` must be available at build time.** The `cds build --for ord` command is provided by the `@cap-js/ord` Node.js package. A successful build command alone does not prove that ORD resources were generated. Verify the dependency and the resulting output:

```bash
# In the project root (or srv/ if that is the CDS project root):
npm ls @cap-js/ord
```

If missing, install it as a dev dependency using the compatible version selected in "Before starting" and the project's existing package manager. For npm:

```bash
npm install --save-dev @cap-js/ord@ORD_NPM_VERSION
```

---

### Approach A — `.cdsrc.json` (preferred when a `build` section already exists there)

Add an `ord` task to the `build.tasks` array in `.cdsrc.json`. Preserve all existing tasks:

```json
{
  "build": {
    "target": ".",
    "tasks": [
      { "for": "java", "src": "srv" },
      { "for": "ord",  "src": "srv", "dest": "srv/src/main/resources/ord" }
    ]
  }
}
```

- `"src"` points to the CDS service source directory (typically `srv`).
- `"dest"` is the ORD output destination. Check how the installed CDS build tool resolves it relative to the task source and build target; the packaged resources must land on the Java service module's classpath.

Run the project's CDS build and inspect its actual output before accepting the configuration. Adjust `src`, `dest`, and the build target to the project's layout rather than copying the example unchanged.

---

### Approach B — `pom.xml` execution (preferred when CDS build is driven entirely from Maven)

Find the `cds-maven-plugin` in the service module's `pom.xml` and add an execution:

```xml
<execution>
  <id>cds-build-ord</id>
  <goals><goal>cds</goal></goals>
  <configuration>
    <commands>
      <command>build --for ord -o "${project.basedir}/src/main/resources/ord"</command>
    </commands>
  </configuration>
</execution>
```

Confirm the execution runs before Maven copies resources and inspect the actual CDS output. The intended destination is `src/main/resources/ord/`, which Maven packages as the default classpath resource root `ord/`.

---

> **Do not hand-edit files in the output directory.** They are regenerated on every build. Use the ORD configuration in `.cdsrc.json` (or `package.json` under `"cds": { "ord": { ... } }`) to control the document content.

> **Ignore generated output according to the project's convention.** Ignore only the directory owned by ORD generation. Preserve application-owned files, including custom resolver resources and service registrations, and keep the package-manager lockfile tracked.

---

## Step 2b — MTX sidecar integration (dynamic model / extensibility)

If the project uses an MTX sidecar for multitenancy or extensibility, the sidecar must also expose ORD documents.

### Condition for MTX mode activation

The plugin includes static and dynamic `OrdResolver` implementations. The built-in dynamic resolvers handle `system-instance` requests when the runtime uses a dynamic model. Dynamic model support requires **all** of the following:

- A model provider URL is configured: either `cds.model.provider.url` or `cds.multitenancy.sidecar.url` is non-empty.
- At least one of `cds.model.provider.extensibility=true` or `cds.model.provider.toggles=true` is set.

If only the URL is set but neither `extensibility` nor `toggles` is enabled, MTX mode is **not** activated.

### Sidecar setup

Add `@cap-js/ord` as a **production** dependency (not dev) in the sidecar's `package.json`:

```json
{
  "dependencies": {
    "@cap-js/ord": "ORD_NPM_VERSION"
  }
}
```

> Using `@cap-js/ord` as a devDependency in the root `package.json` is sufficient for local development and CDS build-time generation, but the sidecar needs it at runtime to serve dynamic ORD documents per tenant.

### Well-known endpoint in MTX mode

Both ORD servlets create a CAP system-user request context using the `Local-Tenant-Id` header. Pass the subscribed tenant ID when verifying tenant-specific behavior. The built-in sidecar client sends that tenant when model extensibility is enabled and sends enabled feature toggles when toggle support is enabled. For EDMX, the dynamic resource resolver uses CAP's `EdmxV4Provider` instead of fetching the definition from the sidecar.

When a request arrives and the runtime determines that a dynamic (tenant/toggle-specific) model must be used — based on the current request's tenant and enabled feature toggles — the well-known endpoint includes a second entry with `"perspective": "system-instance"`. Whether the static or dynamic model is used is evaluated per request, not at startup.

---

## Step 3 — Configuration reference

All ORD properties are optional; the defaults below apply without ORD-specific YAML. Serving built-in documents still requires generated resources, and protected requests require the configured authentication to succeed.

### ORD endpoints

| Property | Type | Default | Description |
| -------- | ---- | ------- | ----------- |
| `cds.ord.wellKnownEndpoint.enabled` | `boolean` | `true` | Enable/disable `/.well-known/open-resource-discovery` |
| `cds.ord.wellKnownEndpoint.path` | `String` | `/.well-known/open-resource-discovery` | Path of the well-known endpoint |
| `cds.ord.documentsEndpoint.enabled` | `boolean` | `true` | Enable/disable the ORD documents endpoint |
| `cds.ord.documentsEndpoint.path` | `String` | `/ord/v1` | Base path for document and resource definition files |

### ORD document location

| Property | Type | Default | Description |
| -------- | ---- | ------- | ----------- |
| `cds.ord.ordResourcesRoot` | `String` | `ord/` | Classpath root folder for static ORD files |
| `cds.ord.ordDocumentPath` | `String` | `ord-document.json` | Relative path of the main ORD document under `ordResourcesRoot` |

The full classpath path resolved at runtime is `<ordResourcesRoot><ordDocumentPath>`, e.g. `ord/ord-document.json`.

### OData V4 entry points

The plugin rewrites `entryPoints` in the ORD document to match the configured OData V4 path. If the project uses a non-default path, set:

```yaml
cds:
  odataV4:
    endpoint:
      path: /my-custom-path
```

The built-in `EntryPointsProcessor` replaces occurrences of `/odata/v4` in `entryPoints` with this value. It runs in the built-in document resolvers; custom document resolvers must explicitly apply processors if they need the same adaptation.

### Minimal `application.yaml` (all defaults, nothing to add)

Only add YAML properties when overriding defaults:

```yaml
cds:
  ord:
    documentsEndpoint:
      path: /ord/v1        # default — omit unless changing
    wellKnownEndpoint:
      enabled: true        # default — omit unless disabling
```

---

## Step 4 — Configure authentication

By default, the ORD endpoints enforce **mTLS certificate validation** (`sap:cmp-mtls:v1`). This is controlled by:

| Property | Type | Default | Description |
| -------- | ---- | ------- | ----------- |
| `cds.security.authentication.authenticateMetadataEndpoints` | `boolean` | `true` | `true` = mTLS required; `false` = endpoints are open |

### Production (SAP BTP Cloud Foundry with mTLS)

The default `authenticateMetadataEndpoints: true` expects UCL certificate validation. Provide the cert subject and issuer via `application.yaml` (use environment-specific config files or secret management — do not commit production cert details):

```yaml
cds:
  ucl:
    x509:
      cert-subject: "CN=cmp-prod,OU=SAP Cloud Platform Clients,OU=cmp-cf-eu10,O=SAP SE,L=Prod,C=DE"
      cert-issuer:  "CN=SAP PKI Certificate Service Client CA,OU=SAP BTP Clients,O=SAP SE,L=cf-eu10,C=DE"
```

The well-known document entries and resource definitions processed by built-in document resolvers advertise `[{"type":"sap:cmp-mtls:v1"}]` as their `accessStrategies`.

### Open access (local development / non-production)

To disable mTLS and expose endpoints publicly:

```yaml
cds:
  security:
    authentication:
      authenticateMetadataEndpoints: false
```

The well-known document entries and resource definitions processed by built-in document resolvers advertise `[{"type":"open"}]` as their `accessStrategies`.

> **Do not use `authenticateMetadataEndpoints: false` in production.** Always enforce mTLS when deploying to Cloud Foundry.

---

## Step 5 — Verify

Run the project's existing build first. For built-in static resolution, confirm that `ord-document.json` and referenced resource definitions are generated, copied into the service module's `target/classes/ord/` (or the configured resource root), and included in the packaged application. Use the project's existing launch command; for a Spring Boot service, run this from its module:

```bash
mvn spring-boot:run
```

Check the well-known endpoint:

```bash
curl -i http://localhost:8080/.well-known/open-resource-discovery
```

Expected response (with `authenticateMetadataEndpoints: false`):

```json
{
  "openResourceDiscoveryV1": {
    "documents": [
      {
        "url": "/ord/v1/documents/ord-document",
        "perspective": "system-version",
        "accessStrategies": [{ "type": "open" }]
      }
    ]
  }
}
```

Check the ORD document itself:

```bash
curl -i http://localhost:8080/ord/v1/documents/ord-document
```

Use the configured endpoint paths and the document URLs returned by the well-known response. Fetch representative `resourceDefinitions[].url` values as well, and check their content types. These unauthenticated examples assume local open access; for mTLS use the deployment's established certificate-based access.

For MTX verification, use the same subscribed tenant on both requests:

```bash
curl -i -H 'Local-Tenant-Id: SUBSCRIBED_TENANT_ID' \
  http://localhost:8080/.well-known/open-resource-discovery
curl -i -H 'Local-Tenant-Id: SUBSCRIBED_TENANT_ID' \
  'http://localhost:8080/ord/v1/documents/ord-document?perspective=system-instance'
```

Replace `SUBSCRIBED_TENANT_ID` with the test tenant. For a dynamic resource definition, also pass `?perspective=system-instance`; otherwise the documents endpoint defaults to `system-version`. A custom resolver does not automatically change the well-known response's document list.

---

## Custom resolvers — extend document and resource definition resolution

Implement `com.sap.cds.feature.ord.resolver.OrdResolver` in the Java service module to serve ORD documents or resource definitions from an application-specific source, or to override built-in resolution for selected requests. Keep build-time metadata customisation in CDS annotations and ORD configuration when the default source is sufficient.

### Implement and register

1. Create a public implementation with a public no-argument constructor. Implement the inherited `CdsRuntimeAware.setCdsRuntime(CdsRuntime runtime)` method and retain the injected runtime to access CAP APIs.
2. Implement the resolver methods:

   | Method | Contract |
   | ------ | -------- |
   | `isApplicable(String resource, String perspective)` | Return `true` only for requests the resolver handles. `resource` is the path relative to the documents endpoint, without a leading slash (e.g. `documents/ord-document` or `MyService.edmx`). `perspective` is the requested perspective; the endpoint defaults it to `system-version`. |
   | `order()` | Return the selection priority. The plugin selects the applicable resolver with the lowest value. All built-in resolvers use `Integer.MAX_VALUE`; use a smaller value, such as `0`, to override them for matching requests. Avoid equal priorities for overlapping custom resolvers. |
   | `resolve(String resource, String perspective)` | Return an `InputStream` containing the document or resource definition. The plugin invokes this method on the selected resolver and closes the stream after writing the response. |

3. Add the service registration file in the service module:

   ```text
   src/main/resources/META-INF/services/com.sap.cds.feature.ord.resolver.OrdResolver
   ```

   List each custom implementation's fully qualified class name on a separate line, for example:

   ```text
   com.example.ord.CustomOrdResolver
   ```

The plugin discovers implementations through CAP's `ExtendedServiceLoader` and injects `CdsRuntime` before use. Service registration is sufficient; no Spring bean registration is required. Scope `isApplicable` to the intended resource and perspective so other requests continue to use the built-in resolvers. If no resolver is applicable, the endpoint returns HTTP 404.

Return a non-null stream from `resolve`. Selection is final: returning `null` or throwing does not fall back to the next resolver. For an expected missing resource, throw a CAP `ServiceException` with an appropriate error status (for example, `ErrorStatuses.NOT_FOUND`); unexpected exceptions result in HTTP 500.

### CAP request context and tenant access

The ORD feature ensures that resolver methods run with a valid CAP `RequestContext`. For the `system-instance` perspective, that context contains the correct subscribed tenant ID. Access it using the injected runtime:

```java
import com.sap.cds.services.request.RequestContext;

// Inside the resolver, where runtime was injected via setCdsRuntime:
String tenant = RequestContext.getCurrent(runtime).getUserInfo().getTenant();
```

Use this context when resolving tenant-specific content. The injected runtime also provides access to CAP services and providers.

### Endpoint behavior and verification

Custom resolution uses the existing documents endpoint and its authentication. The servlet determines the response content type from the resource path: `documents/ord-document` is JSON; resource definitions must use `.json`, `.edmx`, or `.graphql`. A custom resolver does not add support for other file extensions.

When serving a custom document, return the final ORD JSON, including appropriate URLs and access strategies. The built-in document resolvers apply `CdsOrdNodeProcessor` implementations; the servlet does not automatically apply those processors to a custom resolver's stream.

After starting the application, request the custom resource through the configured documents endpoint with the intended perspective. Verify its content and content type, and check that a resource outside the custom resolver's scope still uses built-in resolution. For tenant-specific resolution, verify the response using the subscribed tenant's request context.

---

## Troubleshooting

| Symptom | Cause | Fix |
| ------- | ----- | --- |
| `GET /.well-known/open-resource-discovery` returns 404 | The servlet is disabled or not registered | Verify the dependency is on the classpath (`mvn dependency:tree \| grep cds-feature-ord`). Check `cds.ord.wellKnownEndpoint.enabled` is not set to `false`. |
| A document or resource request returns 404 | No resolver is applicable, or the requested content is missing | Check the requested path and perspective. For custom resolution, verify the service registration and `isApplicable`. For built-in static resolution, verify the generated files under the configured `cds.ord.ordResourcesRoot`. |
| `GET /ord/v1/documents/ord-document` returns 500 with built-in static resolution | The ORD document is missing or cannot be parsed or processed | The static document resolver matches the document path without checking file existence. Confirm the configured classpath resource exists in the packaged application, inspect the JSON and processor errors, and rerun the configured ORD build if generation failed. |
| ORD endpoint returns 401 or certificate error | mTLS validation is failing | Verify `cds.ucl.x509.cert-subject` and `cds.ucl.x509.cert-issuer` match the certificate presented by the ORD consumer. For local testing set `cds.security.authentication.authenticateMetadataEndpoints: false`. |
| `system-instance` perspective is missing from the well-known response | MTX mode is not active | Both `cds.model.provider.url` (or `cds.multitenancy.sidecar.url`) and at least one of `cds.model.provider.extensibility=true` / `cds.model.provider.toggles=true` must be set (see Step 2b). |
| A resource definition URL cannot be fetched | No applicable resolver can supply the referenced resource | After `ResourceDefinitionsProcessor` prefixes URLs with the documents endpoint path, the servlet selects an `OrdResolver` for each request. For built-in static resolution, verify the generated files under `src/main/resources/ord/`. For custom resolution, verify the URL matches `isApplicable` and the resolver returns the expected content. |
| A custom resolver is ignored | It is not registered, does not match the request, or has insufficient priority | Check the packaged `META-INF/services/com.sap.cds.feature.ord.resolver.OrdResolver` file, the resource and perspective passed to `isApplicable`, and `order()`. Use a value below `Integer.MAX_VALUE` to take precedence over built-in resolvers. |

---

## Implementation notes

- **No Spring auto-configuration** — the plugin registers itself entirely via `META-INF/services/` (`ServiceLoader`). There is no `@EnableOrd` annotation or Spring bean to define.
- **Choose compatible releases.** `cds-feature-ord` and `@cap-js/ord` have independent release versions. Check CAP Java and Node.js/CDS compatibility and release notes when upgrading; preserve existing compatible BOM and lockfile management.
- **EDMX is generated through CAP at runtime.** Built-in resource definition resolvers use `EdmxV4Provider.getEdmx()` with the resource filename's base name as the service name. The static resolver still requires the corresponding classpath resource to exist for applicability. Check both the resource path and service name when diagnosing missing metadata.
- **Resolver selection is per request.** Built-in static resolvers serve `system-version`; built-in dynamic resolvers serve `system-instance` when a dynamic model is used. If no resolver matches the resource and perspective, the endpoint returns HTTP 404. A custom resolver can handle additional cases; advertising documents through the well-known index is a separate concern.
- **Unknown resource definition file extensions cause HTTP 500.** The documents servlet only serves `.json` (→ `application/json`), `.edmx` (→ `application/xml`), and `.graphql` (→ `text/plain`). Any other extension in the ORD document's `resourceDefinitions[].url` results in an `IllegalArgumentException` at request time. The ORD document itself (`documents/ord-document`) is always treated as JSON regardless of extension.
- **MTX sidecar calls use SAP Cloud SDK destination `com.sap.cds.mtxSidecar`.** If the sidecar request fails with a non-2xx response or `IOException`, the plugin throws a `ServiceException` that surfaces as an HTTP 500 to the ORD consumer.
- **Custom documents endpoint path propagates through built-in processors.** Setting `cds.ord.documentsEndpoint.path` to e.g. `/custom-api/ord/v1` causes `ResourceDefinitionsProcessor` to prefix `resourceDefinitions[].url` values in built-in document resolution. A custom resolver returning raw JSON must supply suitable URLs or explicitly apply processors.

---

## Next steps — customising the generated ORD metadata

After integration, the ORD document is generated automatically from the CDS model with default values for visibility, titles, versions, package assignment, and resource identifiers. In most projects these defaults need to be adjusted to match product, namespace, and compliance requirements.

Customise the generated metadata on the Node.js/CDS side via CDS annotations in `.cds` files and the `ord` block in `.cdsrc.json`, which control what `cds build --for ord` produces. For application-specific runtime sources or resolution behavior, implement `OrdResolver` as described in Custom resolvers. Built-in document resolvers process the resulting document at runtime, including endpoint path prefixing and access strategy rewriting.

The reference for all available customisations is the `custom-ord-annotations` skill in the [`cap-js/ord`](https://github.com/cap-js/ord) repository at `.skills/custom-ord-annotations/SKILL.md`. It covers:

- Setting `visibility` (`public` / `internal` / `private`) per service or entity
- Overriding titles, descriptions, and versions
- Controlling ORD IDs and package assignment
- Configuring consumption bundles
- Defining data products and external integration dependencies
- Using `customOrdContentFile` for anything the annotations cannot express
