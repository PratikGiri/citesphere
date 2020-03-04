package edu.asu.diging.citesphere.core.util.model.impl;

import java.util.*;

import org.apache.xml.utils.URI;
import org.hamcrest.Matcher;
import org.hibernate.validator.constraints.URL;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.web.util.UriBuilder;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.Matchers.*;

import edu.asu.diging.citesphere.core.exceptions.ZoteroHttpStatusException;
import edu.asu.diging.citesphere.core.service.ICitationConceptManager;
import edu.asu.diging.citesphere.core.service.IConceptTypeManager;
import edu.asu.diging.citesphere.core.service.impl.CitationManager;
import edu.asu.diging.citesphere.model.IUser;
import edu.asu.diging.citesphere.model.bib.IAffiliation;
import edu.asu.diging.citesphere.model.bib.ICitation;
import edu.asu.diging.citesphere.model.bib.ICitationConcept;
import edu.asu.diging.citesphere.model.bib.ICitationConceptTag;
import edu.asu.diging.citesphere.model.bib.IConceptType;
import edu.asu.diging.citesphere.model.bib.ICreator;
import edu.asu.diging.citesphere.model.bib.IPerson;
import edu.asu.diging.citesphere.model.bib.impl.Affiliation;
import edu.asu.diging.citesphere.model.bib.impl.Citation;
import edu.asu.diging.citesphere.model.bib.impl.CitationConcept;
import edu.asu.diging.citesphere.model.bib.impl.CitationConceptTag;
import edu.asu.diging.citesphere.model.bib.impl.ConceptType;
import edu.asu.diging.citesphere.model.bib.impl.Person;
import edu.asu.diging.citesphere.model.impl.User;
import edu.asu.diging.citesphere.web.forms.AffiliationForm;
import edu.asu.diging.citesphere.web.forms.CitationForm;
import edu.asu.diging.citesphere.web.forms.ConceptAssignmentForm;
import edu.asu.diging.citesphere.web.forms.PersonForm;

public class CitationHelperTest {

    @Mock
    private ICitationConceptManager conceptManager;

    @Mock
    private IConceptTypeManager typeManager;

    private ICitation updatedCitation;
    private CitationForm form;
    private IUser user;

    @InjectMocks
    private CitationHelper helperToTest;

    @Before
    public  void setUp() {

        MockitoAnnotations.initMocks(this);
        user = new User();
        user.setUsername("akhil");
        initForm();
        initUpdatedCitation();

    }

    private  void initForm() {
        form = new CitationForm();
        form.setTitle("updatedtitle");
        form.setShortTitle("updatedShortTitle");
        List<PersonForm> authors = new ArrayList<PersonForm>();
        PersonForm author = new PersonForm();
        author.setFirstName("first");
        author.setLastName("last");
        List<AffiliationForm> affiliationForms = new ArrayList<AffiliationForm>();
        AffiliationForm affiliationForm = new AffiliationForm();
        affiliationForm.setName("Test");
        affiliationForms.add(affiliationForm);
        author.setAffiliations(affiliationForms);
        authors.add(author);
        form.setAuthors(authors);
        form.setDateFreetext("2019");
        form.setVolume("1.0");
        List<PersonForm> editors = new ArrayList<PersonForm>();
        PersonForm editor = new PersonForm();
        editor.setFirstName("editor");
        editor.setLastName("editor");
        editors.add(editor);
        form.setEditors(editors);
        List<ConceptAssignmentForm> conceptTags = new ArrayList<>();
        ConceptAssignmentForm tag = new ConceptAssignmentForm();
        tag.setConceptName("testc");
        tag.setConceptUri("www.google.com");
        tag.setConceptId("CON1");
        tag.setConceptTypeId("CTY1");
        tag.setConceptTypeName("forum");
        tag.setConceptTypeUri("www.test.com");
        conceptTags.add(tag);
        form.setConceptTags(conceptTags);
    }

    private void initUpdatedCitation() {
        updatedCitation = new Citation();
        updatedCitation.setTitle("updatedtitle");
        updatedCitation.setShortTitle("updatedShortTitle");
        updatedCitation.setDateFreetext("2019");
        updatedCitation.setVolume("1.0");
        Set<IPerson> editorsSet = new LinkedHashSet<IPerson>();
        IPerson editor1 = new Person();
        editor1.setFirstName("editor");
        editor1.setLastName("editor");
        editor1.setId("1");
        editorsSet.add(editor1);
        updatedCitation.setEditors(editorsSet);
        Set<IPerson> authorsSet = new LinkedHashSet<IPerson>();
        IPerson author1 = new Person();
        author1.setFirstName("first");
        author1.setLastName("last");
        author1.setId("2");
        Set<IAffiliation> affiliations = new LinkedHashSet<IAffiliation>();
        IAffiliation affiliation = new Affiliation();
        affiliation.setName("Test");
        affiliations.add(affiliation);
        author1.setAffiliations(affiliations);
        authorsSet.add(author1);
        updatedCitation.setAuthors(authorsSet);
        Set<ICitationConceptTag> conceptTagsSet = new LinkedHashSet<ICitationConceptTag>();
        ICitationConceptTag tag1 = new CitationConceptTag();
        tag1.setConceptName("testc.com");
        tag1.setConceptUri("www.google.com");
        tag1.setLocalConceptId("CON1");
        tag1.setLocalConceptTypeId("CTY1");
        tag1.setTypeName("forum");
        tag1.setTypeUri("www.test.com");
        conceptTagsSet.add(tag1);
        updatedCitation.setConceptTags(conceptTagsSet);
    }

    @Test
    public void test_updateCitation() {
        ICitation citation;
        citation = new Citation();
        citation.setTitle("title");
        citation.setShortTitle("shortTitle");
        citation.setDateFreetext("2019");
        citation.setVolume("1.0");
        ICitationConcept concept = new CitationConcept();
        concept.setId("CON1");
        concept.setOwner(user);
        concept.setUri("www.google.com");
        IConceptType type = new ConceptType();
        type.setId("CTY1");
        type.setOwner(user);
        type.setName("forum");
        type.setUri("www.test.com");
        Mockito.when(conceptManager.save(any(ICitationConcept.class))).thenReturn(concept);
        Mockito.when(typeManager.save(any(IConceptType.class))).thenReturn(type);
        helperToTest.updateCitation(citation, form, user);
        assertTrue(equalsCitation(citation, updatedCitation));
    }

    @Test
    public void test_updateCitation_getByURIAndOwner() {
        ICitation citation;
        citation = new Citation();
        citation.setTitle("title");
        citation.setShortTitle("shortTitle");
        citation.setDateFreetext("2019");
        citation.setVolume("1.0");
        ICitationConcept concept = new CitationConcept();
        concept.setId("CON1");
        concept.setOwner(user);
        concept.setUri("www.google.com");
        IConceptType type = new ConceptType();
        type.setId("CTY1");
        type.setOwner(user);
        type.setName("forum");
        type.setUri("www.test.com");
        
        Mockito.when(conceptManager.getByUriAndOwner(Mockito.argThat(new URIMatcher()), any(IUser.class))).thenReturn(concept);

        
        Mockito.when(typeManager.getByUriAndOwner(Mockito.argThat(new URIMatcher()), any(IUser.class))).thenReturn(type);
        helperToTest.updateCitation(citation, form, user);
        assertTrue(equalsCitation(citation, updatedCitation));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void test_updateCitation_getByURIAndOwner_Invalid_URI() {
        ICitation citation;
        citation = new Citation();
        citation.setTitle("title");
        citation.setShortTitle("shortTitle");
        citation.setDateFreetext("2019");
        citation.setVolume("1.0");
        ICitationConcept concept = new CitationConcept();
        concept.setId("CON1");
        concept.setOwner(user);
        concept.setUri("www.google.com");
        IConceptType type = new ConceptType();
        type.setId("CTY1");
        type.setOwner(user);
        type.setName("forum");
        type.setUri("www.test.com");
        form.getConceptTags().iterator().next().setConceptUri("google");
        
        Mockito.when(conceptManager.getByUriAndOwner(Mockito.argThat(new URIMatcher()), any(IUser.class))).thenReturn(concept);

        
        Mockito.when(typeManager.getByUriAndOwner(Mockito.argThat(new URIMatcher()), any(IUser.class))).thenReturn(type);
        helperToTest.updateCitation(citation, form, user);
        assertTrue(equalsCitation(citation, updatedCitation));
    }
    
    
    private boolean comparePersons(Set<IPerson> a1, Set<IPerson> a2) {
        
        if (a1 != null && a2 != null) {

            if (a1.size() != a2.size())
                return false;
            Iterator<IPerson> p1 = a1.iterator();
            Iterator<IPerson> p2 = a2.iterator();
            while (p1.hasNext() && p2.hasNext()) {
                if (!comparePerson(p1.next(), p2.next())) {
                    return false;
                }
            }
            return true;
        }
        
        return false;
        
    }
    
    
  private boolean compareCreators(Set<ICreator> a1, Set<ICreator> a2) {
        
      if (a1 != null && a2 != null) {

          if (a1.size() != a2.size())
              return false;
          Iterator<ICreator> p1 = a1.iterator();
          Iterator<ICreator> p2 = a1.iterator();
          while (p1.hasNext() && p2.hasNext()) {
              if (!comparePerson(p1.next().getPerson(), p2.next().getPerson())) {
                  return false;
              }
          }

      }
        
        return false;
        
    }
  
  private boolean compareTags(Set<ICitationConceptTag> a1, Set<ICitationConceptTag> a2) {
      
      if (a1 != null && a2 != null) {

          if (a1.size() != a2.size())
              return false;
          Iterator<ICitationConceptTag> p1 = a1.iterator();
          Iterator<ICitationConceptTag> p2 = a2.iterator();
          while (p1.hasNext() && p2.hasNext()) {
              if (!compareConceptTag(p1.next(), p2.next())) {
                  return false;
              }
          }
      }
        
        return false;
        
    }
  private boolean compareStrings(String s1, String s2) {
      return s1.equals(s2);
  }

    private boolean equalsCitation(ICitation c1, ICitation c2) {
        
     if ( compareStrings(c1.getTitle(), c2.getTitle()) && compareStrings(c1.getShortTitle(), c2.getShortTitle()) && comparePersons(c1.getAuthors(), c2.getAuthors()) 
             && comparePersons(c1.getEditors(), c2.getEditors()) &&  compareCreators(c1.getOtherCreators(), c2.getOtherCreators()) && compareTags(c1.getConceptTags(), c2.getConceptTags()))
         return true;
        
        return false;
    }

    private boolean comparePerson(IPerson p1, IPerson p2) {

        if (!p1.getFirstName().equals(p2.getFirstName()) || !p1.getLastName().equals(p2.getLastName())) {
            return false;
        }
        if (p1.getAffiliations() != null && p2.getAffiliations() != null) {
            if (p1.getAffiliations().size() != p2.getAffiliations().size())
                return false;
            Iterator<IAffiliation> i1 = p1.getAffiliations().iterator();
            Iterator<IAffiliation> i2 = p2.getAffiliations().iterator();
            while (i1.hasNext() && i2.hasNext()) {
                if (!compareAffliattion(i1.next(), i2.next())) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean compareAffliattion(IAffiliation a1, IAffiliation a2) {

        if (a1.getName().equals(a2.getName())) {
            return true;
        }

        return false;

    }

    private boolean compareConceptTag(ICitationConceptTag c1, ICitationConceptTag c2) {

        if (c1.getLocalConceptId().equals(c2.getLocalConceptId())
                && c1.getLocalConceptTypeId().equals(c2.getLocalConceptTypeId())) {
            return true;
        }

        return false;
    }
    
    class URIMatcher extends ArgumentMatcher<String> {

        @Override
        public boolean matches(Object arg) {
            
            String regex = "((http?|https|ftp|file)://)?((W|w){3}.)?[a-zA-Z0-9]+\\.[a-zA-Z]+";
            String uri = (String)arg;
            
            
           if (uri.matches(regex))
               return true;
           else throw new IllegalArgumentException();
        }
    }
    
 

}
