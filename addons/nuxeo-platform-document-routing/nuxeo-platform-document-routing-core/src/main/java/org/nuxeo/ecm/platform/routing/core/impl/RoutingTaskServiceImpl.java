/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     ldoguin
 */
package org.nuxeo.ecm.platform.routing.core.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;
import org.nuxeo.ecm.platform.routing.api.RoutingTaskService;
import org.nuxeo.ecm.platform.routing.api.exception.DocumentRouteException;
import org.nuxeo.ecm.platform.routing.core.api.DocumentRoutingEngineService;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.platform.task.TaskEventNames;
import org.nuxeo.ecm.platform.task.TaskService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 *
 */
public class RoutingTaskServiceImpl extends DefaultComponent implements
        RoutingTaskService {

    protected DocumentRoutingEngineService routingEngineService;

    @Override
    public void makeRoutingTasks(CoreSession coreSession, final List<Task> tasks)
            throws ClientException {
        new UnrestrictedSessionRunner(coreSession) {
            @Override
            public void run() throws ClientException {
                for (Task task : tasks) {
                    DocumentModel taskDoc = task.getDocument();
                    taskDoc.addFacet(DocumentRoutingConstants.ROUTING_TASK_FACET_NAME);
                    session.saveDocument(taskDoc);
                }
            }
        }.runUnrestricted();
    }

    @Override
    public void endTask(CoreSession session, Task task,
            Map<String, Object> data, String status)
            throws DocumentRouteException {
        String comment = (String) data.get("comment");
        try {
            TaskService taskService = Framework.getLocalService(TaskService.class);
            taskService.endTask(session,
                    (NuxeoPrincipal) session.getPrincipal(), task, comment,
                    TaskEventNames.WORKFLOW_TASK_COMPLETED, false);
        } catch (ClientException e) {
            throw new DocumentRouteException("Can not resume workflow", e);
        }

        Map<String, String> taskVariables;
        try {
            taskVariables = task.getVariables();
        } catch (ClientException e) {
            throw new DocumentRouteException("Can not resume workflow", e);
        }
        String routeInstanceId = taskVariables.get(DocumentRoutingConstants.TASK_ROUTE_INSTANCE_DOCUMENT_ID_KEY);
        if (StringUtils.isEmpty(routeInstanceId)) {
            throw new DocumentRouteException(
                    "Can not resume workflow, no related route");
        }
        String nodeId = taskVariables.get(DocumentRoutingConstants.TASK_NODE_ID_KEY);
        if (StringUtils.isEmpty(nodeId)) {
            throw new DocumentRouteException(
                    "Can not resume workflow, nodeId is empty");
        }
        DocumentModel routeDoc;
        try {
            routeDoc = session.getDocument(new IdRef(routeInstanceId));
        } catch (ClientException e) {
            throw new DocumentRouteException(
                    "can not resume workflow, no workflow with the id:"
                            + routeInstanceId);
        }
        DocumentRoute route = routeDoc.getAdapter(DocumentRoute.class);
        // unrestricted , the graph runner can create new tasks
        try {
            new RouteResumer(session, route, nodeId, data, status).runUnrestricted();
        } catch (ClientException e) {
            throw new DocumentRouteException("Can not resume workflow", e);
        }
    }

    protected DocumentRoutingEngineService getDocumentRoutingEngineService() {
        if (routingEngineService == null) {
            try {
                routingEngineService = Framework.getService(DocumentRoutingEngineService.class);
            } catch (Exception e) {
                throw new ClientRuntimeException(e);
            }
        }
        return routingEngineService;
    }

    @Override
    public void grantPermissionToTaskAssignees(CoreSession session,
            final String permission, final DocumentModel doc, final Task task)
            throws DocumentRouteException {
        try {
            new UnrestrictedSessionRunner(session) {

                @Override
                public void run() throws ClientException {
                    List<String> actorIds = new ArrayList<String>();
                    for (String actor : task.getActors()) {
                        if (actor.contains(":")) {
                            actorIds.add(actor.split(":")[1]);
                        } else {
                            actorIds.add(actor);
                        }
                    }
                    ACP acp = doc.getACP();
                    ACL acl = acp.getOrCreateACL(ACL.LOCAL_ACL);
                    for (String actorId : actorIds) {
                        acl.add(new ACE(actorId, permission, true));
                    }
                    acp.addACL(acl);
                    doc.setACP(acp, true);
                    session.saveDocument(doc);
                }

            }.runUnrestricted();
        } catch (ClientException e) {
            throw new DocumentRouteException(e);
        }
    }

    class RouteResumer extends UnrestrictedSessionRunner {

        DocumentRoute routeInstance;

        String nodeId;

        Map<String, Object> data;

        String status;

        protected RouteResumer(CoreSession session,
                DocumentRoute routeInstance, String nodeId,
                Map<String, Object> data, String status) {
            super(session);
            this.routeInstance = routeInstance;
            this.nodeId = nodeId;
            this.data = data;
            this.status = status;
        }

        @Override
        public void run() throws ClientException {
            getDocumentRoutingEngineService().resume(routeInstance, session,
                    nodeId, data, status);
        }
    }
}