/*
 * Copyright (C) 2009-2013, Free University of Bozen Bolzano
 * This source code is available under the terms of the Affero General Public
 * License v3.
 * 
 * Please see LICENSE.txt for full license terms, including the availability of
 * proprietary exceptions.
 */
package it.unibz.krdb.obda.owlapi3;

import it.unibz.krdb.obda.model.OBDAModel;

import org.semanticweb.owlapi.reasoner.OWLReasoner;

public interface OBDAOWLReasoner extends OWLReasoner {
	
	public void loadOBDAModel(OBDAModel model);

}
