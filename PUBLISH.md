# Release procedure

## Maven Central (`io.github.dnalchemist`)

Artifact coordinates: **`io.github.dnalchemist:gitlab-code-quality-gradle-plugin`** (Gradle plugin marker: **`io.github.dnalchemist.gitlab-code-quality-gradle`**). Make sure the `pom { scm { ... } url { ... } }` block in `build.gradle.kts` points to your actual GitHub repository.

### 1. Claim the namespace on Central

1. Sign in to [central.sonatype.com](https://central.sonatype.com/) (typically via GitHub).
2. Create the namespace **`io.github.dnalchemist`**. For `io.github.*` namespaces it is usually enough to confirm ownership of the matching GitHub account (`dnalchemist`).
3. Wait for approval.

### 2. Publishing credentials for `gradle publish`

In [central.sonatype.com](https://central.sonatype.com/) generate a **User Token** (a username/password pair).

Either store them locally in `~/.gradle/gradle.properties`:

```properties
ossrhUsername=your_token_username
ossrhPassword=your_token_password
```

…or expose them through environment variables in CI (already wired up in `.github/workflows/deploy.yml`):

```text
ORG_GRADLE_PROJECT_ossrhUsername
ORG_GRADLE_PROJECT_ossrhPassword
```

### 3. Sonatype host

New projects publish to **`s01.oss.sonatype.org`** (the default in `build.gradle.kts`). If your account is still on the legacy OSSRH host, override it via `gradle.properties`:

```properties
sonatype.host=oss.sonatype.org
```

### 4. Releasing to Maven Central (non-SNAPSHOT)

Maven Central requires release artifacts to be **GPG-signed**. Apply the [Signing Plugin](https://docs.gradle.org/current/userguide/signing_plugin.html) and configure a key (locally, or via `ORG_GRADLE_PROJECT_signing.*` / in-memory PGP in CI), then run:

```bash
./gradlew clean publish
```

After uploading, open [central.sonatype.com](https://central.sonatype.com/) → **Deployments**, wait for validation and click **Publish** (or use the classic Nexus Staging UI if your account is still on legacy OSSRH).

### 5. Snapshots

Versions ending in **`-SNAPSHOT`** go to the snapshot repository (`…/content/repositories/snapshots/`). Signing is usually not required for snapshots.

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

Publish: `./gradlew build publish` (with signing enabled for releases — see above).
