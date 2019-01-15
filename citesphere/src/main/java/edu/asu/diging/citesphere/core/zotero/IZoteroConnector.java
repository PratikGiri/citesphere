package edu.asu.diging.citesphere.core.zotero;

import java.util.List;

import org.springframework.social.zotero.api.FieldInfo;
import org.springframework.social.zotero.api.Group;
import org.springframework.social.zotero.api.Item;
import org.springframework.social.zotero.api.ZoteroResponse;
import org.springframework.social.zotero.exception.ZoteroConnectionException;

import edu.asu.diging.citesphere.core.model.IUser;

public interface IZoteroConnector {

    ZoteroResponse<Item> getGroupItems(IUser user, String groupId, int page, String sortBy);

    ZoteroResponse<Group> getGroups(IUser user);

    Item getItem(IUser user, String groupId, String itemKey);

    ZoteroResponse<Group> getGroupsVersions(IUser user);

    ZoteroResponse<Item> getGroupItemsWithLimit(IUser user, String groupId, int limit, String sortBy);

    Group getGroup(IUser user, String groupId, boolean forceRefresh);

    Item updateItem(IUser user, Item item, String groupId, List<String> ignoreFields) throws ZoteroConnectionException;

    FieldInfo[] getFields(IUser user, String itemType);

    long getItemVersion(IUser user, String groupId, String itemKey);

}