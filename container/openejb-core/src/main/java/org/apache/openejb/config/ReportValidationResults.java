/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.openejb.config;

import org.apache.openejb.OpenEJBException;
import org.apache.openejb.loader.SystemInstance;
import org.apache.openejb.util.Logger;
import org.apache.openejb.util.LogCategory;

/**
 * @version $Rev$ $Date$
 */
public class ReportValidationResults implements DynamicDeployer {

    private static final String VALIDATION_LEVEL = "openejb.validation.level";

    private enum Level {
        UNUSED,
        TERSE,
        MEDIUM,
        VERBOSE
    }

    public AppModule deploy(AppModule appModule) throws OpenEJBException {
        String levelString = SystemInstance.get().getProperty(VALIDATION_LEVEL, Level.MEDIUM.toString());

        Level level;
        try {
            level = Level.valueOf(levelString);
        } catch (IllegalArgumentException noSuchEnumConstant) {
            try {
                int i = Integer.parseInt(levelString);
                level = Level.values()[i];
            } catch (NumberFormatException e) {
                level = Level.MEDIUM;
            }
        }
        
        if (level == Level.UNUSED) level = Level.MEDIUM;

        if (!appModule.hasErrors() && !appModule.hasFailures()) return appModule;

        ValidationFailedException validationFailedException = null;


        for (DeploymentModule module : appModule.getEjbModules()) {
            validationFailedException = logResults(module, validationFailedException, level);
        }

        for (DeploymentModule module : appModule.getClientModules()) {
            validationFailedException = logResults(module, validationFailedException, level);
        }

        validationFailedException = logResults(appModule, validationFailedException, level);

        if (validationFailedException != null)
            throw validationFailedException;

        return appModule;
    }

    private ValidationFailedException logResults(DeploymentModule module, ValidationFailedException validationFailedException, Level level) {
        ValidationResults results = module.getValidation();

        if (results.hasErrors() || results.hasFailures()) {
            Logger logger = Logger.getInstance(LogCategory.OPENEJB_STARTUP_VALIDATION, "org.apache.openejb.config.rules");

            ValidationError[] errors = results.getErrors();
            for (int j = 0; j < errors.length; j++) {
                ValidationError e = errors[j];
                String ejbName = e.getComponentName();
                logger.error(e.getPrefix() + " ... " + ejbName + ":\t" + e.getMessage(level.ordinal()));
            }
            ValidationFailure[] failures = results.getFailures();
            for (int j = 0; j < failures.length; j++) {
                ValidationFailure e = failures[j];
                logger.error(e.getPrefix() + " ... " + e.getComponentName() + ":\t" + e.getMessage(level.ordinal()));
            }

            validationFailedException = new ValidationFailedException("Module failed validation. "+results.getModuleType()+"(path="+results.getJarPath()+")", results, validationFailedException);
        }

        return validationFailedException;
    }

}
