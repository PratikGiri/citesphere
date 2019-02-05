package edu.asu.diging.citesphere.core.zotero;

import java.util.List;

import org.springframework.social.zotero.api.Collection;
import org.springframework.social.zotero.api.CreatorType;
import org.springframework.social.zotero.api.FieldInfo;
import org.springframework.social.zotero.api.Group;
import org.springframework.social.zotero.api.Item;
import org.springframework.social.zotero.api.ZoteroResponse;
import org.springframework.social.zotero.exception.ZoteroConnectionException;

import edu.asu.diging.citesphere.core.exceptions.ZoteroItemCreationFailedException;
import edu.asu.diging.citesphere.core.model.IUser;

public interface IZoteroConnector {

    ZoteroResponse<Item> getGroupItems(IUser user, String groupId, int page, String sortBy, Long lastGroupVersion);

    ZoteroResponse<Group> getGroups(IUser user);

    Item getItem(IUser user, String groupId, String itemKey);

    ZoteroResponse<Group> getGroupsVersions(IUser user);

    ZoteroResponse<Item> getGroupItemsWithLimit(IUser user, String groupId, int limit, String sortBy, Long lastGroupVersion);

    Group getGroup(IUser user, String groupId, boolean forceRefresh);

    Item updateItem(IUser user, Item item, String groupId, List<String> ignoreFields, List<String> validCreatorTypes) throws ZoteroConnectionException;

    FieldInfo[] getFields(IUser user, String itemType);

    long getItemVersion(IUser user, String groupId, String itemKey);

    Item createItem(IUser user, Item item, String groupId, List<String> ignoreFields, List<String> validCreatorTypes)
            throws ZoteroConnectionException, ZoteroItemCreationFailedException;

    CreatorType[] getItemTypeCreatorTypes(IUser user, String itemType);

    ZoteroResponse<Collection> getCitationCollections(IUser user, String groupId, int page, String sortBy, Long lastGroupVersion);

}