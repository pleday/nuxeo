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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.MigrationFeature;
import org.nuxeo.runtime.migration.Migration;
import org.nuxeo.runtime.migration.MigrationService;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * @since 11.1
 */
@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class, MigrationFeature.class, TransactionalFeature.class })
public class TestMigrationService {

    @Inject
    protected TransactionalFeature txFeature;

    @Inject
    protected MigrationService migrationService;

    @Test
    public void testGetMigrations() {
        var migrations = migrationService.getMigrations();
        assertEquals(2, migrations.size());
    }

    @Test
    public void testGetMigration() {
        Migration migration = migrationService.getMigration("trash-storage");
        assertEquals(migration.getId(), "trash-storage");
        assertEquals(migration.getDescription(), "Migration of in the trash storage model");
        assertEquals(migration.getLabel(), "migration.trash-storage");
        assertEquals(migration.getStatus().getState(), "property");
        assertEquals(migration.getSteps().get("lifecycle-to-property").getDescription(),
                "Migrate trashed state from lifecycle to property");

    }
    @Test
    public void testRunMigration() {
        String dummy = "dummy-migration";
        assertEquals("before", migrationService.getMigration(dummy).getStatus().getState());
        migrationService.run(dummy);
        txFeature.nextTransaction();
        assertEquals("after", migrationService.getMigration(dummy).getStatus().getState());
        try {
            migrationService.run(dummy);
            fail("should fail");
        } catch (java.lang.IllegalStateException e) {
            assertEquals("Migration dummy-migration needs to have exactly one step to run", e.getMessage());
        }
    }

}
