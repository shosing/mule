/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static java.util.Optional.empty;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.processor.MessageProcessors.newChain;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.lifecycle.LifecycleUtils;
import org.mule.runtime.core.api.processor.MessageProcessorChain;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.util.ObjectNameHelper;
import org.mule.runtime.core.api.util.func.Once;
import org.mule.runtime.extension.api.runtime.route.Chain;
import org.mule.runtime.module.extension.internal.runtime.operation.ImmutableProcessorChainExecutor;

import java.util.List;

import javax.inject.Inject;

/**
 * An {@link ValueResolver} which wraps the given {@link Processor} in a {@link Chain},
 * using the event of the current {@link ValueResolvingContext}.
 * This resolver returns new instances per every invocation
 *
 * @since 4.0
 */
public final class ProcessorRouteValueResolver implements ValueResolver<Chain> {

  private final MessageProcessorChain chain;
  private final Once initialiser;

  @Inject
  private MuleContext muleContext;


  public ProcessorRouteValueResolver(MessageProcessorChain chain) {
    this.chain = chain;
    initialiser = getInitialiaser();
  }

  public ProcessorRouteValueResolver(List<Processor> processors) {
    chain = newChain(empty(), processors);
    initialiser = getInitialiaser();
  }

  /**
   * Returns a {@link Chain} that wraps the given {@link Processor} using the current {@code event}
   *
   * @param context a {@link ValueResolvingContext}
   * @return a {@link Chain}
   * @throws MuleException
   */
  @Override
  public Chain resolve(ValueResolvingContext context) throws MuleException {
    initialiser.runOnce();
    return new ImmutableProcessorChainExecutor(context.getEvent(), chain);
  }

  /**
   * @return {@code false}
   */
  @Override
  public boolean isDynamic() {
    return false;
  }

  private Once getInitialiaser() {
    return Once.of(() -> {
      try {
        muleContext.getRegistry().registerObject(new ObjectNameHelper(muleContext).getUniqueName(""), chain);
        LifecycleUtils.initialiseIfNeeded(chain, muleContext);
      } catch (Exception e) {
        throw new MuleRuntimeException(createStaticMessage("Could not register nested operation message processor"), e);
      }
    });
  }

}
