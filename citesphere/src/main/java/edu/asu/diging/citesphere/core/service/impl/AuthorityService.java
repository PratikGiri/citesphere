package edu.asu.diging.citesphere.core.service.impl;

import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.asu.diging.citesphere.core.authority.AuthorityImporter;
import edu.asu.diging.citesphere.core.authority.IImportedAuthority;
import edu.asu.diging.citesphere.core.exceptions.AuthorityServiceConnectionException;
import edu.asu.diging.citesphere.core.model.IUser;
import edu.asu.diging.citesphere.core.model.authority.IAuthorityEntry;
import edu.asu.diging.citesphere.core.model.authority.impl.AuthorityEntry;
import edu.asu.diging.citesphere.core.repository.AuthorityEntryRepository;
import edu.asu.diging.citesphere.core.service.IAuthorityService;

@Service
public class AuthorityService implements IAuthorityService {

    @Autowired
    private AuthorityEntryRepository entryRepository;
    
    @Autowired
    private Set<AuthorityImporter> importers;
    
    /* (non-Javadoc)
     * @see edu.asu.diging.citesphere.core.service.impl.IAuthorityService#register(edu.asu.diging.citesphere.authority.AuthorityImporter)
     */
    @Override
    public void register(AuthorityImporter importer) {
        this.importers.add(importer);
    }
    
    /* (non-Javadoc)
     * @see edu.asu.diging.citesphere.core.service.impl.IAuthorityService#importAuthority(java.lang.String)
     */
    @Override
    public IAuthorityEntry importAuthority(String uri) throws URISyntaxException, AuthorityServiceConnectionException {
        for(AuthorityImporter importer : importers) {
            if (importer.isResponsible(uri)) {
                IImportedAuthority importedAuthority = importer.retrieveAuthorityData(uri);
                if (importedAuthority == null) {
                    return null;
                }
                
                IAuthorityEntry entry = new AuthorityEntry();
                entry.setName(importedAuthority.getName());
                entry.setUri(importedAuthority.getUri());
                entry.setImporterId(importer.getId());
                return entry;
            }
        }
        return null;
    }
    
    @Override
    public IAuthorityEntry find(String id) {
        Optional<AuthorityEntry> entryOptional = entryRepository.findById(id);
        if (entryOptional.isPresent()) {
            return entryOptional.get();
        }
        return null;
    }
    
    @Override
    public boolean deleteAuthority(String id) {
        entryRepository.deleteById(id);
        return true;
    }
    
    @Override
    public List<IAuthorityEntry> getAll(IUser user) {
        return entryRepository.findByUsernameOrderByName(user.getUsername());
    }
    
    @Override
    public IAuthorityEntry save(IAuthorityEntry entry, IUser user) {
        entry.setUsername(user.getUsername());
        entry.setCreatedOn(OffsetDateTime.now());
        return (IAuthorityEntry) entryRepository.save((AuthorityEntry)entry);
    }
}