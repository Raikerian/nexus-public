/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.repository.conda.datastore.internal.proxy

import javax.annotation.Nonnull
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton

import org.sonatype.nexus.repository.Format
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.Type
import org.sonatype.nexus.repository.conda.CondaFormat
import org.sonatype.nexus.repository.conda.datastore.internal.CondaContentFacet
import org.sonatype.nexus.repository.conda.datastore.internal.CondaRecipeSupport
import org.sonatype.nexus.repository.http.HttpHandlers
import org.sonatype.nexus.repository.proxy.ProxyHandler
import org.sonatype.nexus.repository.types.ProxyType
import org.sonatype.nexus.repository.view.ConfigurableViewFacet
import org.sonatype.nexus.repository.view.Route
import org.sonatype.nexus.repository.view.Router.Builder
import org.sonatype.nexus.repository.view.ViewFacet

/**
 * Conda proxy recipe.
 *
 * @since 3.next
 */
@Named(CondaProxyRecipe.NAME)
@Singleton
class CondaProxyRecipe
    extends CondaRecipeSupport
{
  public static final String NAME = 'conda-proxy'

  @Inject
  Provider<CondaContentFacet> contentFacet

  @Inject
  Provider<CondaProxyFacet> proxyFacet;

  @Inject
  ProxyHandler proxyHandler

  @Inject
  CondaProxyRecipe(@Named(ProxyType.NAME) final Type type, @Named(CondaFormat.NAME) final Format format) {
    super(type, format)
  }

  @Override
  void apply(@Nonnull final Repository repository) throws Exception {
    repository.attach(contentFacet.get())
    repository.attach(browseFacet.get())
    repository.attach(proxyFacet.get())
    repository.attach(securityFacet.get())
    repository.attach(configure(viewFacet.get()))
    repository.attach(httpClientFacet.get())
    repository.attach(negativeCacheFacet.get())
    repository.attach(maintenanceFacet.get())
    repository.attach(searchFacet.get())
    repository.attach(purgeUnusedFacet.get())
  }

  /**
   * Configure {@link ViewFacet}.
   */
  private ViewFacet configure(final ConfigurableViewFacet facet) {
    Builder builder = new Builder()

    addBrowseUnsupportedRoute(builder)

    [rootChannelIndexHtmlMatcher(),
     rootChannelDataJsonMatcher(),
     rootChannelRssXmlMatcher(),
     archIndexHtmlMatcher(),
     archRepodataJsonMatcher(),
     archRepodataJsonBz2Matcher(),
     archRepodata2JsonMatcher(),
     archTarPackageMatcher(),
     archCondaPackageMatcher()].
        each { matcher ->
          builder.route(new Route.Builder().matcher(matcher)
              .handler(timingHandler)
              .handler(securityHandler)
              .handler(routingHandler)
              .handler(exceptionHandler)
              .handler(handlerContributor)
              .handler(negativeCacheHandler)
              .handler(conditionalRequestHandler)
              .handler(partialFetchHandler)
              .handler(contentHeadersHandler)
              .handler(lastDownloadedHandler)
              .handler(proxyHandler)
              .create())
        }

    builder.defaultHandlers(HttpHandlers.notFound())

    facet.configure(builder.create())

    return facet
  }
}
