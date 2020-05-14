import org.apache.logging.log4j.LogManager
import org.nuxeo.ecm.platform.task.Task
import org.nuxeo.ecm.core.api.IdRef
import org.nuxeo.ecm.core.api.ScrollResult
import org.nuxeo.runtime.transaction.TransactionHelper
import java.util.HashMap

log = LogManager.getLogger("taskGC.groovy")
nxql = "SELECT * FROM Document WHERE ecm:mixinType = 'Task'"
dryRun = true
session = Context.getCoreSession()
ScrollResult<String> scrollResult = session.scroll(nxql, 500, 60)
bucketCount = 0
documentCount = 0

while (scrollResult.hasResults()) {
    documentCount += scrollResult.getResults().size()
    cache = new HashMap<IdRef>()
    for(id in scrollResult.getResults()) {
        task = session.getDocument(new IdRef(id)).getAdapter(Task.class)
        workflowRef = new IdRef(task.getProcessId())
        if(!cache.computeIfAbsent(workflowRef, session.&exists)) {
            log.info("removing orphan task " + id)
            if(!dryRun) {
                session.removeDocument(new IdRef(id))
            }
        }
    }
    bucketCount += 1
    scrollResult = session.scroll(scrollResult.getScrollId())
    TransactionHelper.commitOrRollbackTransaction()
    TransactionHelper.startTransaction()
}
