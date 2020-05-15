/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Nour AL KOTOB
 */
package org.nuxeo.ecm.core.migration.test;

import java.util.List;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.migrator.AbstractRepositoryMigrator;
import org.nuxeo.ecm.core.repository.RepositoryService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.migration.MigrationService.MigrationContext;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * @since 11.1
 */
public class DummyMigrator extends AbstractRepositoryMigrator {

    protected String state;

    @Override
    public void run(String step, MigrationContext migrationContext) {
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        state = "after";
    }

    @Override
    public void notifyStatusChange() {
        // Do nothing
    }

    @Override
    protected void migrateSession(String step, MigrationContext context, CoreSession session) {
        // Do nothing
    }

    @Override
    protected String probeSession(CoreSession session) {
        // Default state in descriptor is enough
        return "Do nothing";
    }

}
