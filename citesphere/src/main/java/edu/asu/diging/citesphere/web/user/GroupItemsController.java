package edu.asu.diging.citesphere.web.user;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.JsonObject;
import edu.asu.diging.citesphere.core.exceptions.GroupDoesNotExistException;
import edu.asu.diging.citesphere.core.model.IUser;
import edu.asu.diging.citesphere.core.model.bib.ICitation;
import edu.asu.diging.citesphere.core.model.bib.ICitationCollection;
import edu.asu.diging.citesphere.core.model.bib.ICitationGroup;
import edu.asu.diging.citesphere.core.model.bib.ICollectionsJSON;
import edu.asu.diging.citesphere.core.model.bib.impl.CitationResults;
import edu.asu.diging.citesphere.core.service.ICitationCollectionManager;
import edu.asu.diging.citesphere.core.service.ICitationManager;
import edu.asu.diging.citesphere.core.service.IGroupManager;
import edu.asu.diging.citesphere.web.BreadCrumb;
import edu.asu.diging.citesphere.web.BreadCrumbType;

@Controller
@PropertySource("classpath:/config.properties")
public class GroupItemsController {

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

    @RequestMapping(value= { "/auth/group/{zoteroGroupId}/items", "/auth/group/{zoteroGroupId}/collection/{collectionId}/items"})
    public String show(Authentication authentication, Model model, @PathVariable("zoteroGroupId") String groupId,
            @PathVariable(value="collectionId", required=false) String collectionId,
            @RequestParam(defaultValue = "1", required = false, value = "page") String page,
            @RequestParam(defaultValue = "title", required = false, value = "sort") String sort,
            @RequestParam(required = false, value = "columns") String[] columns)
            throws GroupDoesNotExistException {
        
        Integer pageInt = 1;
        try {
            pageInt = new Integer(page);
        } catch (NumberFormatException ex) {
            logger.warn("Trying to access invalid page number: " + page);
        }

        IUser user = (IUser) authentication.getPrincipal();
        CitationResults results = citationManager.getGroupItems(user, groupId, collectionId, pageInt, sort);
        model.addAttribute("items", results.getCitations());
        model.addAttribute("total", results.getTotalResults());
        model.addAttribute("totalPages", Math.ceil(new Float(results.getTotalResults()) / new Float(zoteroPageSize)));
        model.addAttribute("currentPage", pageInt);
        model.addAttribute("zoteroGroupId", groupId);
        model.addAttribute("group", groupManager.getGroup(user, groupId));
        model.addAttribute("collectionId", collectionId);
        // more than 200 really don't make sense here, this needs to be changed
        model.addAttribute("citationCollections", collectionManager.getAllCollections(user, groupId, null, "title", 200));
        
        List<String> allowedColumns = Arrays.asList(availableColumns.split(","));
        List<String> shownColumns = new ArrayList<>();
        if (columns != null && columns.length > 0) {
            for (String column : columns) {
                if (allowedColumns.contains(column)) {
                    shownColumns.add(column);
                }
            }
        }
        
        model.addAttribute("columns", shownColumns);
        model.addAttribute("availableColumns", allowedColumns);
        
        
        ICitationGroup group = groupManager.getGroup(user, groupId);
        List<BreadCrumb> breadCrumbs = new ArrayList<>();
        
        ICitationCollection collection = null;
        if (collectionId != null) {
            collection = collectionManager.getCollection(user, groupId, collectionId);
        }
        while(collection != null) {
            breadCrumbs.add(new BreadCrumb(collection.getName(), BreadCrumbType.COLLECTION, collection.getKey(), collection));
            if (collection.getParentCollectionKey() != null) {
                collection = collectionManager.getCollection(user, groupId, collection.getParentCollectionKey());
            } else {
                collection = null;
            }
        }
        breadCrumbs.add(new BreadCrumb(group.getName(), BreadCrumbType.GROUP, group.getId() + "", group));
        Collections.reverse(breadCrumbs);
        model.addAttribute("breadCrumbs", breadCrumbs);
        return "auth/group/items";
    }
    
    @RequestMapping(value= {"/auth/group/{zoteroGroupId}/collections/{collectionId}/items"})
    public ResponseEntity<String> display(Authentication authentication, Model model, @PathVariable("zoteroGroupId") String groupId,
            @PathVariable(value="collectionId", required=false) String collectionId,
            @RequestParam(defaultValue = "1", required = false, value = "page") String page,
            @RequestParam(defaultValue = "title", required = false, value = "sort") String sort,
            @RequestParam(required = false, value = "columns") String[] columns)
            throws GroupDoesNotExistException {
        
        Integer pageInt = 1;
        try {
            pageInt = new Integer(page);
        } catch (NumberFormatException ex) {
            logger.warn("Trying to access invalid page number: " + page);
        }

        IUser user = (IUser) authentication.getPrincipal();
        CitationResults results = citationManager.getGroupItems(user, groupId, null, pageInt, sort);
        
        ICollectionsJSON object = new ICollectionsJSON();

        object.setTotal(results.getTotalResults());
        object.setTotalPages(Math.ceil(new Float(results.getTotalResults()) / new Float(zoteroPageSize)));
        object.setCurrentPage(pageInt);
        object.setZoteroGroupId(groupId);
        object.setCollectionId(collectionId);
        object.setCitationCollections(collectionManager.getCitationCollections(user, groupId, null, pageInt, "title").getCitationCollections());
        List<ICitation> list = results.getCitations();
        object.setItems(list);
        
        return new ResponseEntity<String>(new com.google.gson.Gson().toJson(object), HttpStatus.OK);
        
        /*
         * To deserialize
         *  MyObject obj = new com.google.gson.Gson().fromJSON(responseAsString, MyObject.class);
          *    obj.getMessage();
           *    obj.getSuccess();
         */
    }
}
