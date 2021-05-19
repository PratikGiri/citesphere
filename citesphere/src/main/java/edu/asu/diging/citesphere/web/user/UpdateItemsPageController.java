package edu.asu.diging.citesphere.web.user;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.gson.Gson;

import edu.asu.diging.citesphere.core.exceptions.GroupDoesNotExistException;
import edu.asu.diging.citesphere.core.exceptions.ZoteroHttpStatusException;
import edu.asu.diging.citesphere.core.service.ICitationCollectionManager;
import edu.asu.diging.citesphere.core.service.ICitationManager;
import edu.asu.diging.citesphere.core.service.IGroupManager;
import edu.asu.diging.citesphere.model.bib.ICitationCollection;
import edu.asu.diging.citesphere.model.bib.ICitationGroup;
import edu.asu.diging.citesphere.model.bib.impl.CitationResults;
import edu.asu.diging.citesphere.user.IUser;
import edu.asu.diging.citesphere.web.BreadCrumb;
import edu.asu.diging.citesphere.web.BreadCrumbType;

@Controller
public class UpdateItemsPageController {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Value("${_zotero_page_size}")
    private Integer zoteroPageSize;
    
    @Value("${_available_item_columns}")
    private String availableColumns;

    @Autowired
    private ICitationManager citationManager;
    
    @Autowired
    private ICitationCollectionManager collectionManager;
    
    @Autowired
    private IGroupManager groupManager;

    @RequestMapping(value= "/auth/group/{zoteroGroupId}/items/data")
    public @ResponseBody ItemsDataDto show(Authentication authentication, @PathVariable("zoteroGroupId") String groupId,
            @PathVariable(value="collectionId", required=false) String collectionId,
            @RequestParam(defaultValue = "1", required = false, value = "page") String page,
            @RequestParam(defaultValue = "title", required = false, value = "sort") String sort,
            @RequestParam(required = false, value = "columns") String[] columns) {
        Integer pageInt = 1;
        try {
            pageInt = new Integer(page);
        } catch (NumberFormatException ex) {
            logger.warn("Trying to access invalid page number: " + page);
        }
        IUser user = (IUser) authentication.getPrincipal();
        CitationResults results;
        try {
            results = citationManager.getGroupItems(user, groupId, collectionId, pageInt, sort);
        } catch(ZoteroHttpStatusException e) {
            logger.error("Exception occured", e);
            return null;
        } catch(GroupDoesNotExistException e) {
            logger.error("Exception occured", e);
            return null;
        }
        
        ItemsDataDto itemsData = new ItemsDataDto();
        //itemsData.setCitations(results.getCitations());
        itemsData.setTotalResults(results.getTotalResults());
        itemsData.setTotalPages(Math.ceil(new Float(results.getTotalResults()) / new Float(zoteroPageSize)));
        itemsData.setCurrentPage(pageInt);
        itemsData.setZoteroGroupId(groupId);
        itemsData.setResults(results);
        
        try {
            itemsData.setCitationCollections(collectionManager.getAllCollections(user, groupId, collectionId, "title", 200));
        } catch(GroupDoesNotExistException e) {
            logger.error("Exception occured", e);
            return null;
        }
        List<String> allowedColumns = Arrays.asList(availableColumns.split(","));
        List<String> shownColumns = new ArrayList<>();
        if (columns != null && columns.length > 0) {
            for (String column : columns) {
                if (allowedColumns.contains(column)) {
                    shownColumns.add(column);
                }
            }
        }
        itemsData.setShownColumns(shownColumns);
        itemsData.setAllowedColumns(allowedColumns);
       
        Gson gson = new Gson();
        return itemsData;
    }
    
}
