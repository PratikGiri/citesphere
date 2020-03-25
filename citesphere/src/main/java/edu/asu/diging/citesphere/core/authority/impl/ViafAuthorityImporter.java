package edu.asu.diging.citesphere.core.authority.impl;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import edu.asu.diging.citesphere.core.authority.IImportedAuthority;
import edu.asu.diging.citesphere.core.authority.impl.ViafResponse.Data;
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

    @Value("${_viaf_search_url}")
    private String viafsearchUrl;

    @Value("${_viaf_search_query}")
    private String viafsearchquery;

    @Value("${_viaf_page_size}")
    private String viafPageSizequery;

    @Value("${_viaf_record_start_index}")
    private String viafStartIndexQuery;

    @Value("${search_viaf_url_path2}")
    private String searchViafURLPath2;

    @Value("${_viaf_max_results}")
    private String viafMaxResults;

    @Value("${_viaf_max_page}")
    private int viafTotalPages;

    private RestTemplate restTemplate;

    @PostConstruct
    private void postConstruct() {
        restTemplate = new RestTemplate();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * edu.asu.diging.citesphere.authority.impl.AuthorityImporter#isResponsible(java
     * .lang.String)
     */
    @Override
    public boolean isResponsible(String source) {
        Pattern pattern = Pattern.compile(viafUrlRegex);
        Matcher matcher = pattern.matcher(source);

        if (matcher.matches() || ID.contains(source)) {
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

        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        HttpClient httpClient = HttpClientBuilder.create().setRedirectStrategy(new LaxRedirectStrategy()).build();
        factory.setHttpClient(httpClient);
        restTemplate.setRequestFactory(factory);

        RequestEntity<Void> request = RequestEntity.get(new URI(uri)).accept(MediaType.APPLICATION_JSON).build();
        ResponseEntity<ViafResponse> response = null;
        try {
            response = restTemplate.exchange(request, ViafResponse.class);
        } catch (RestClientException ex) {
            throw new AuthorityServiceConnectionException(ex);
        }
        if (response.getStatusCode() == HttpStatus.OK) {
            ViafResponse viaf = response.getBody();
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

    @Override
    public AuthoritySearchResult searchAuthorities(String searchString, int page, int pageSize)

            throws URISyntaxException, AuthorityServiceConnectionException {

        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        HttpClient httpClient = HttpClientBuilder.create().setRedirectStrategy(new LaxRedirectStrategy()).build();
        factory.setHttpClient(httpClient);
        restTemplate.setRequestFactory(factory);

        String startIndex = page == 0 ? "1" : Integer.toString(page * pageSize);
        String fullUrl = "";

        try {
            fullUrl = viafsearchUrl.trim() + viafsearchquery.trim() + URLEncoder.encode(searchString.trim(), "UTF-8")
                    + viafPageSizequery.trim() + viafMaxResults.trim() + viafStartIndexQuery.trim() + startIndex.trim()
                    + searchViafURLPath2.trim();
            System.out.println(fullUrl);
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError(StandardCharsets.UTF_8.toString() + " is not supported");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_RSS_XML));

        HttpEntity<ViafReply> entity = new HttpEntity<ViafReply>(headers);
        ResponseEntity<ViafReply> reply;
        reply = restTemplate.exchange(new URI(fullUrl), HttpMethod.GET, entity, ViafReply.class);
        ViafReply rep = reply.getBody();

        List<IAuthorityEntry> authorityEntries = new ArrayList<IAuthorityEntry>();
        List<Item> items = null;
        items = rep.getChannel().getItems();

        if (items != null) {
            for (Item i : items) {
                IAuthorityEntry authority = new AuthorityEntry();
                authority.setDescription(i.getTitle());
                authority.setUri(i.getLink());
                authority.setName(i.getTitle());
                authorityEntries.add(authority);
            }
        }

        AuthoritySearchResult searchResult = new AuthoritySearchResult();
        searchResult.setFoundAuthorities(authorityEntries);
        searchResult.setTotalPages(viafTotalPages);
        searchResult.setCurrentPage(page + 1);
//        try {
////
////            String url = viafsearchUrl
////                    + URLEncoder.encode(searchString + ";maximumRecords=" + pageSize + ";startRecord=" + page,
////                            StandardCharsets.UTF_8.toString())
////                    + viafsearchquery;
//
//            String url = "http://viaf.org/viaf/search?query=local.names all "+searchString+"+&amp;maximumRecords=100&amp;startRecord=1&amp;sortKeys=holdingscount&amp;httpAccept=application/rss+xml";
////            URI uri = UriComponentsBuilder.fromUriString(url.toString()).build(true).toUri();
//           // URI uri = UriComponentsBuilder.fromUriString(url).build(true).toUri();
//
//            request = RequestEntity.get(new URI(url)).accept(MediaType.APPLICATION_JSON).build();
//        } catch (Exception e) {
//            throw new URISyntaxException(e.getMessage(), e.toString());
//
//        }
//        ResponseEntity<ViafResponse> response = null;
//        try {
//            response = restTemplate.exchange(request, ViafResponse.class);
//        } catch (RestClientException ex) {
//            throw new AuthorityServiceConnectionException(ex);
//        }
//
//        List<IAuthorityEntry> authorityEntries = new ArrayList<IAuthorityEntry>();
//        if (response.getStatusCode() == HttpStatus.OK) {
//
//            ViafResponse viaf = response.getBody();
//            Iterator<Data> iterator = viaf.getMainHeadings().getData().iterator();
//            if (iterator.hasNext()) {
//
//                String name = iterator.next().getText();
//                IAuthorityEntry authority = new AuthorityEntry();
//                authority.setName(name);
//                authority.setUri(viaf.getDocument().get("@about") + "");
//                authorityEntries.add(authority);
//
//            }
//        } else {
//            throw new AuthorityServiceConnectionException(response.getStatusCode().toString());
//        }
//        return authorityEntries;
        return searchResult;
    }
}
