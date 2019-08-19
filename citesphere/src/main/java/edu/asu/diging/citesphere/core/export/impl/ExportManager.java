package edu.asu.diging.citesphere.core.export.impl;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;

import edu.asu.diging.citesphere.core.exceptions.ExportFailedException;
import edu.asu.diging.citesphere.core.exceptions.ExportTooBigException;
import edu.asu.diging.citesphere.core.exceptions.ExportTypeNotSupportedException;
import edu.asu.diging.citesphere.core.exceptions.GroupDoesNotExistException;
import edu.asu.diging.citesphere.core.export.ExportFinishedCallback;
import edu.asu.diging.citesphere.core.export.ExportType;
import edu.asu.diging.citesphere.core.export.IExportManager;
import edu.asu.diging.citesphere.core.export.IExportProcessor;
import edu.asu.diging.citesphere.core.model.IUser;
import edu.asu.diging.citesphere.core.model.bib.ICitationCollection;
import edu.asu.diging.citesphere.core.model.bib.ICitationGroup;
import edu.asu.diging.citesphere.core.model.bib.impl.CitationResults;
import edu.asu.diging.citesphere.core.model.export.ExportStatus;
import edu.asu.diging.citesphere.core.model.export.IExportTask;
import edu.asu.diging.citesphere.core.model.export.impl.ExportTask;
import edu.asu.diging.citesphere.core.repository.export.ExportTaskRepository;
import edu.asu.diging.citesphere.core.service.ICitationCollectionManager;
import edu.asu.diging.citesphere.core.service.ICitationManager;
import edu.asu.diging.citesphere.core.service.IGroupManager;

@Service
@Transactional
@PropertySource("classpath:config.properties")
public class ExportManager implements IExportManager, ExportFinishedCallback {
    
    private final Logger logger = LoggerFactory.getLogger(getClass());
    
    private ConcurrentHashMap<String, IExportTask> runningTasks; 
    
    @Value("${_tasks_page_size}")
    private Integer tasksPageSize;
    
    @Value("${_max_export_size}")
    private Integer maxExportSize;
    
    @Autowired
    private ExportTaskRepository taskRepo;
    
    @Autowired
    private IExportProcessor processor;
    
    @Autowired
    private ICitationManager citationManager;
    
    @Autowired
    private IGroupManager groupManager;
    
    @Autowired
    private ICitationCollectionManager collectionManager;
    
    @PostConstruct
    public void init() {
        runningTasks = new ConcurrentHashMap<>();
    }

    /* (non-Javadoc)
     * @see edu.asu.diging.citesphere.core.export.impl.IExportManager#runExport(edu.asu.diging.citesphere.core.export.ExportType, edu.asu.diging.citesphere.core.model.IUser, java.lang.String)
     */
    @Override
    public void export(ExportType exportType, IUser user, String groupId, String collectionId) throws GroupDoesNotExistException, ExportTypeNotSupportedException, ExportFailedException, ExportTooBigException {
        
        ICitationGroup group = groupManager.getGroup(user, groupId);
        if (group == null) {
            throw new GroupDoesNotExistException("Group does not exist.");
        }
        
        // FIXME: sort field should not be hard coded!
        CitationResults results = citationManager.getGroupItems(user, groupId, collectionId, 1, "title");
        if (results.getTotalResults() > maxExportSize) {
            throw new ExportTooBigException("Can't export " + results.getTotalResults() + " records.");
        }
        
        ICitationCollection collection = null;
        if (collectionId != null && !collectionId.trim().isEmpty()) {
            collection = collectionManager.getCollection(user, groupId, collectionId);
        }
        
        
        IExportTask task = new ExportTask();
        task.setExportType(exportType);
        task.setUsername(user.getUsername());
        task.setStatus(ExportStatus.PENDING);
        task.setCreatedOn(OffsetDateTime.now());
        task.setGroupId(groupId);
        task.setGroupName(group.getName());
        if (collection != null) {
            task.setCollectionId(collectionId);
            task.setCollectionName(collection.getName());
        }
        
        task = taskRepo.saveAndFlush((ExportTask) task);
        runningTasks.put(task.getId(), task);
        
        processor.runExport(exportType, user, groupId, collectionId, task, this);
    }
    
    @Override
    public IExportTask getTask(String id) {
        Optional<ExportTask> optional = taskRepo.findById(id);
        if (optional.isPresent()) {
            return optional.get();
        }
        return null;
    }
    
    @Override
    public List<IExportTask> getTasks(IUser user, int page) {
        List<IExportTask> tasks = new ArrayList<>();
        taskRepo.findByUsername(user.getUsername(), PageRequest.of(page, tasksPageSize, Sort.by(Direction.DESC, "createdOn", "id"))).forEach(t -> tasks.add(t));
        return tasks;
    }
    
    @Override
    public int getTasksTotal(IUser user) {
        return taskRepo.countByUsername(user.getUsername());
    }
    
    @Override
    public int getTasksTotalPages(IUser user) {
        int totalTasks = getTasksTotal(user);
        int pagesTotal = totalTasks/tasksPageSize;
        if (totalTasks % tasksPageSize > 0) {
            pagesTotal += 1;
        }
        return pagesTotal;
    }

    @Override
    public void exportFinished(String taskId) {
        runningTasks.remove(taskId);
    }
    
    @PreDestroy
    public void shutdown() {
        logger.info("Failing " + runningTasks.size() + " unfinished tasks (number of actually failed tasks might be lower).");
        for (String taskId : runningTasks.keySet()) {
            Optional<ExportTask> taskOptional = taskRepo.findById(taskId);
            if (taskOptional.isPresent()) {
                IExportTask task = taskOptional.get();
                if (Arrays.asList(ExportStatus.PENDING, ExportStatus.INITIALIZING, ExportStatus.STARTED).contains(task.getStatus())) {
                    task.setStatus(ExportStatus.FAILED);
                    taskRepo.save((ExportTask) task);
                }
            }
        }
    }
}