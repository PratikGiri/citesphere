package edu.asu.diging.citesphere.core.util.model;

import edu.asu.diging.citesphere.core.model.bib.ICitation;
import edu.asu.diging.citesphere.web.forms.CitationForm;

public interface ICitationHelper {

    void updateCitation(ICitation citation, CitationForm form);

}