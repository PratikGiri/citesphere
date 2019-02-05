package edu.asu.diging.citesphere.core.factory.impl;

import org.springframework.social.zotero.api.Collection;
import org.springframework.stereotype.Component;

import edu.asu.diging.citesphere.core.factory.ICitationCollectionFactory;
import edu.asu.diging.citesphere.core.model.bib.ICitationCollection;
import edu.asu.diging.citesphere.core.model.bib.impl.CitationCollection;

@Component
public class CitationCollectionFactory implements ICitationCollectionFactory {

    /* (non-Javadoc)
     * @see edu.asu.diging.citesphere.core.factory.impl.ICitationCollectionFactory#createCitationCollection(org.springframework.social.zotero.api.Collection)
     */
    @Override
    public ICitationCollection createCitationCollection(Collection collection) {
        ICitationCollection citationCollection = new CitationCollection();
        citationCollection.setKey(collection.getKey());
        citationCollection.setName(collection.getData().getName());
        citationCollection.setNumberOfCollections(collection.getMeta().getNumCollections());
        citationCollection.setNumberOfItems(collection.getMeta().getNumItems());
        citationCollection.setParentCollection(collection.getData().getParentCollection());
        citationCollection.setVersion(collection.getVersion());
        return citationCollection;
    }
}
