package edu.asu.diging.citesphere.core.authority.impl;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import edu.asu.diging.citesphere.core.authority.IImportedAuthority;
import edu.asu.diging.citesphere.core.authority.impl.ViafRecordData.Data;
import edu.asu.diging.citesphere.core.authority.impl.ViafSearchResponseWrapper.ViafSearchItem;
import edu.asu.diging.citesphere.core.authority.impl.ViafSearchResponseWrapper.ViafSearchResponse;
import edu.asu.diging.citesphere.core.exceptions.AuthorityServiceConnectionException;
import edu.asu.diging.citesphere.model.authority.IAuthorityEntry;
import edu.asu.diging.citesphere.model.authority.impl.AuthorityEntry;
import edu.asu.diging.citesphere.web.user.AuthoritySearchResult;

@Component
@PropertySource(value = "classpath:/config.properties")
public class ViafAuthorityImporter extends BaseAuthorityImporter {

    private final String ID = "authority.importer.viaf";

    @Value("${_viaf_url_regex}")
    private String viafUrlRegex;

    @Value("${_viaf_authority_search_keyword}")
    private String viafSearchKeyword;

    @Value("${_viaf_url}")
    private String viafURL;

    private RestTemplate restTemplate;

    @PostConstruct
    private void postConstruct() {

        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        HttpClient httpClient = HttpClientBuilder.create().setRedirectStrategy(new LaxRedirectStrategy()).build();
        factory.setHttpClient(httpClient);

        restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(factory);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * edu.asu.diging.citesphere.authority.impl.AuthorityImporter#isResponsible(java
     * .lang.String)
     */
    @Override
    public boolean isResponsible(String uri) {
        Pattern pattern = Pattern.compile(viafUrlRegex);
        Matcher matcher = pattern.matcher(uri);

        if (matcher.matches()) {
            return true;
        }

        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.asu.diging.citesphere.authority.impl.AuthorityImporter#
     * retrieveAuthorityData(java.lang.String)
     */
    @Override
    @Cacheable("viafAuthorities")
    public IImportedAuthority retrieveAuthorityData(String uri)
            throws URISyntaxException, AuthorityServiceConnectionException {

        RequestEntity<Void> request = RequestEntity
                .get(new URI(uri))
                .accept(MediaType.APPLICATION_JSON).build();
        ResponseEntity<ViafRecordData> response = null;
        try {
            response = restTemplate.exchange(request, ViafRecordData.class);
        } catch (RestClientException ex) {
            throw new AuthorityServiceConnectionException(ex);
        }
        if (response.getStatusCode() == HttpStatus.OK) {
            ViafRecordData viaf = response.getBody();
            Iterator<Data> iterator = viaf.getMainHeadings().getData().iterator();
            if (iterator.hasNext()) {
                // let's get the first entry for now
                String name = iterator.next().getText();
                ImportedAuthority authority = new ImportedAuthority();
                authority.setName(name);
                authority.setUri(viaf.getDocument().get("@about") + "");
                return authority;
            }
        }
        return null;
    }

    @Override
    public String getId() {
        return ID;
    }

    
    /**
     * Searches VIAF for authorities for given search terms.
     * VIAF API Documentation: https://www.oclc.org/developer/api/oclc-apis/viaf/authority-cluster.en.html
     * 
     * Search record may contain multiple mainheading data i.e., authority description.
     * The first description is selected for the returned record data.
     * For e.g.,
     * "data": [
                {
                    "text": "Albert, August 1881-",
                    "sources": {
                        "s": "DNB",
                        "sid": "DNB|1204691185"
                    }
                },
                {
                    "text": "August Albert German philologist",
                    "sources": {
                        "s": "WKP",
                        "sid": "WKP|Q83561432"
                    }
                }
            ],
     * 
     * @param firstName First name of authority search term
     * @param lastName Last name of authority search term
     * @param page Page number request
     * @param pageSize Records in a single page
     * @return the authority search result
     */
    @Override
    public AuthoritySearchResult searchAuthorities(String firstName, String lastName, int page, int pageSize)
            throws AuthorityServiceConnectionException {

        // VIAF search API response contains maximum of 10 records
        pageSize = Math.min(10, pageSize);
        String url = viafURL + viafSearchKeyword + this.getViafSearchString(firstName, lastName, page, pageSize);

        ResponseEntity<ViafSearchResponseWrapper> response = null;
        try {
            response = restTemplate.exchange(url, HttpMethod.GET, null, ViafSearchResponseWrapper.class);
        } catch (RestClientException ex) {
            throw new AuthorityServiceConnectionException(ex);
        }

        List<IAuthorityEntry> authorityEntries = new ArrayList<IAuthorityEntry>();
        AuthoritySearchResult searchResult = new AuthoritySearchResult();
        if (response.getStatusCode() == HttpStatus.OK) {
            ViafSearchResponse viafSearchResponse = response.getBody().getSearchRetrieveResponse();
            if (viafSearchResponse.getRecords() != null) {
                for (ViafSearchItem viafEntry : viafSearchResponse.getRecords()) {
                    ViafRecordData record = viafEntry.getRecord().getRecordData();
                    IAuthorityEntry authority = new AuthorityEntry();
                    Iterator<Data> iterator = record.getMainHeadings().getData().iterator();
                    if (iterator.hasNext()) {
                        String name = iterator.next().getText();
                        authority.setName(name);
                        authority.setUri(record.getDocument().get("@about") + "");
                        authorityEntries.add(authority);
                    }
                }
                searchResult.setFoundAuthorities(authorityEntries);
                searchResult
                        .setTotalPages((int) Math.ceil(viafSearchResponse.getNumberOfRecords() / new Float(pageSize)));
            }
        }
        return searchResult;
    }

    @Override
    public boolean isResponsibleForSearch(String source) {
        if (source.equals(VIAF)) {
            return true;
        }
        return false;
    }

    /**
     * Generates query string for the given search parameters
     * Example query string = "local.personalNames+all+"Albert Einstein"&startRecord=11&maximumRecords=10&httpAccept=application/json"
     * @param firstName First name of authority search term
     * @param lastName Last name of authority search term
     * @param page Page number request
     * @param pageSize Records in a single page
     * @return Generated query string
     */
    private String getViafSearchString(String firstName, String lastName, int page, int pageSize)
            throws AuthorityServiceConnectionException {
        String viafSearchString = "local.personalNames+all+\"";
        if (firstName != null && !firstName.isEmpty() && lastName != null && !lastName.isEmpty()) {
            viafSearchString += firstName + " " + lastName;
        } else {
            viafSearchString += firstName == null || firstName.isEmpty() ? lastName : firstName;
        }
        int startRecord = page * pageSize + 1;
        viafSearchString += "\"&startRecord=" + startRecord + "&maximumRecords=" + pageSize
                + "&httpAccept=application/json";
        return viafSearchString;
    }

}
