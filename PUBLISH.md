# Release procedure

## Maven Central via the Sonatype Central Portal (`io.github.dnalchemist`)

Sonatype retired the legacy OSSRH service (`oss.sonatype.org` / `s01.oss.sonatype.org`) in 2025. All publishing now goes through the **Central Portal** ([central.sonatype.com](https://central.sonatype.com/)).

Artifact coordinates: **`io.github.dnalchemist:gitlab-code-quality-gradle-plugin`** (Gradle plugin marker: **`io.github.dnalchemist.gitlab-code-quality-gradle`**).

### 1. Claim the namespace

1. Sign in to [central.sonatype.com](https://central.sonatype.com/) (typically via GitHub).
2. Create the namespace **`io.github.dnalchemist`**. For `io.github.*` namespaces it is enough to confirm ownership of the matching GitHub account (`dnalchemist`).
3. Wait for approval.

### 2. Credentials

Generate a **User Token** at [central.sonatype.com](https://central.sonatype.com/) (Account → User Tokens). It produces a username/password pair distinct from your login.

Either store them locally in `~/.gradle/gradle.properties`:

```properties
centralPortalUsername=your_token_username
centralPortalPassword=your_token_password
```

…or expose them through environment variables in CI (already wired up in `.github/workflows/deploy.yml`):

```text
ORG_GRADLE_PROJECT_centralPortalUsername  # GitHub secret CENTRAL_PORTAL_USERNAME
ORG_GRADLE_PROJECT_centralPortalPassword  # GitHub secret CENTRAL_PORTAL_PASSWORD
```

### 3. Snapshots

Versions ending in **`-SNAPSHOT`** are published directly with the bundled `maven-publish` plugin to the Central Portal snapshot endpoint:

```
https://central.sonatype.com/repository/maven-snapshots/
```

Already configured in `build.gradle.kts`. Just run:

```bash
./gradlew publish
```

Snapshot uploads do not require GPG signing. The Central Portal UI does not browse snapshots; verify by fetching `maven-metadata.xml` directly:

```
https://central.sonatype.com/repository/maven-snapshots/io/github/dnalchemist/gitlab-code-quality-gradle-plugin/1.2.0-SNAPSHOT/maven-metadata.xml
```

### 4. Releases

Direct HTTP PUT for releases is **not** accepted by the Central Portal — uploads happen through a Publisher API that takes a single signed zip bundle. The plain `maven-publish` plugin cannot do this on its own.

Use a dedicated plugin. Recommended: [`com.gradleup.nmcp`](https://www.gradleup.com/nmcp/).

The repository already wires `com.gradleup.nmcp` and `com.gradleup.nmcp.aggregation` 1.4.4 in `build.gradle.kts`. They expose:

```bash
./gradlew publishAggregationToCentralPortal
```

GPG signing is required for releases. The `signing` plugin is already applied; supply the key via Gradle properties (in `~/.gradle/gradle.properties`):

```properties
signingKey=-----BEGIN PGP PRIVATE KEY BLOCK-----\n...\n-----END PGP PRIVATE KEY BLOCK-----\n
signingPassword=your_gpg_passphrase
```

…or in CI:

```text
ORG_GRADLE_PROJECT_signingKey       # GitHub secret SIGNING_KEY (ASCII-armored private key)
ORG_GRADLE_PROJECT_signingPassword  # GitHub secret SIGNING_PASSWORD (passphrase)
```

Generate the ASCII-armored key with `gpg --armor --export-secret-keys <KEYID>`. Snapshots have `isRequired = false`, so missing keys do not break snapshot publishes — they only block `-` (non-SNAPSHOT) versions.

After uploading, open [central.sonatype.com](https://central.sonatype.com/) → **Deployments**, wait for validation and click **Publish**. Switch `publishingType` to `"AUTOMATIC"` in `nmcpAggregation.centralPortal` if you want the plugin to release without that manual gate.

---

## Versioning and tags

Example variables:

```bash
export RELEASE_VERSION="1.0.0"
export DEVELOP_VERSION="1.1.0-SNAPSHOT"
```

Update `version` in `build.gradle.kts` and the `:version_stable:` / `:version_snapshot:` attributes in `README.adoc`, then:

```bash
# Set version = RELEASE_VERSION in build.gradle.kts and tweak README.adoc
git commit -am "Preparing ${RELEASE_VERSION} release"
git tag -a -m "v${RELEASE_VERSION}" "v${RELEASE_VERSION}"
```

Bump to the next development version:

```bash
# Set version = DEVELOP_VERSION in build.gradle.kts
git commit -am "Preparing for next development iteration"
git push --follow-tags
```

Snapshots: `./gradlew publish`. Releases: `./gradlew publishAggregationToCentralPortal` (with `nmcp` and signing applied — see step 4 above).
