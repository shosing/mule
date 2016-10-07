/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.enricher;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.module.extension.internal.introspection.describer.MuleExtensionAnnotationParser.parseRepeatableAnnotation;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.extension.api.annotation.Import;
import org.mule.runtime.extension.api.annotation.ImportedTypes;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.introspection.ImportedTypeModel;
import org.mule.runtime.extension.api.introspection.declaration.DescribingContext;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.extension.api.introspection.declaration.type.ExtensionsTypeLoaderFactory;

import java.util.List;

/**
 * Test the extension type to be annotated with {@link Import}, in which case it adds an {@link ImportedTypeModel} on the
 * extension level.
 *
 * @since 4.0
 */
public final class ImportedTypesModelEnricher extends AbstractAnnotatedModelEnricher {

  private ClassTypeLoader typeLoader;

  @Override
  public void enrich(DescribingContext describingContext) {
    ExtensionDeclarer descriptor = describingContext.getExtensionDeclarer();
    ExtensionDeclaration extensionDeclaration = descriptor.getDeclaration();

    final Class<?> type = extractExtensionType(extensionDeclaration);
    typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader(type.getClassLoader());

    final List<Import> importTypes = parseRepeatableAnnotation(type, Import.class, c -> ((ImportedTypes) c).value());

    if (!importTypes.isEmpty()) {
      if (importTypes.stream().map(Import::type).distinct().collect(toList()).size() != importTypes.size()) {
        throw new IllegalModelDefinitionException(
                                                  format("There should be only one Import declaration for any given type in extension [%s]."
                                                      + " Multiple imports of the same type are not allowed",
                                                         extensionDeclaration.getName()));
      }

      importTypes.forEach(imported -> extensionDeclaration
          .addImportedType(new ImportedTypeModel(imported.from(), typeLoader.load(imported.type()))));
    }
  }
}
