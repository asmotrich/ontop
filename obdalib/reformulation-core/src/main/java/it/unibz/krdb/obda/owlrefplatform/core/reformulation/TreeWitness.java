package it.unibz.krdb.obda.owlrefplatform.core.reformulation;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import it.unibz.krdb.obda.model.Function;
import it.unibz.krdb.obda.model.NewLiteral;
import it.unibz.krdb.obda.owlrefplatform.core.reformulation.TreeWitnessReasonerLite.IntersectionOfConceptSets;

/**
 * TreeWitness: universal tree witnesses as in the KR 2012 paper
 *     each tree witness is deNewLiteralined by its domain, root NewLiterals and a set of \exists R.B concept 
 *           that generate a tree in the TBox canonical model to embed the tree witness part of the query
 *           
 *           roots are the NewLiterals that are mapped to the root of that tree
 *           
 *           the "tree witness part of the query" consists of all atoms in the query 
 *                       with NewLiterals in the tw domain and at least one of the NewLiterals not being a tw root
 *                       
 *     each instance also stores those atoms of the query with all NewLiterals among the tw roots
 *      
 *     this information is enough to produce the tree witness formula tw_f 
 *     
 * @author Roman Kontchakov
 *
 */

public class TreeWitness {
	private final TermCover terms;
	
	private final Set<Function> rootAtoms; // atoms of the query that contain only the roots of the tree witness
	                            // these atoms must hold true for this tree witness to be realised
	private final Collection<TreeWitnessGenerator> gens; // the \exists R.B concepts that realise the tree witness 
	                                          // in the canonical model of the TBox
	
	private final IntersectionOfConceptSets rootConcepts; // store concept for merging tree witnesses
	
	private List<List<Function>> twfs;  // tw-formula: disjunction of conjunctions of atoms

	public TreeWitness(Collection<TreeWitnessGenerator> gens, TermCover terms, Set<Function> rootAtoms, IntersectionOfConceptSets rootConcepts) {
		this.gens = gens;
		this.terms = terms;
		this.rootAtoms = rootAtoms;
		this.rootConcepts = rootConcepts;
		//this.domain = domain; // new HashSet<NewLiteral>(roots); domain.addAll(nonroots);
	}
	
	void setFormula(List<List<Function>> twfs) {
		this.twfs = twfs;
	}
	
	public List<List<Function>> getFormula() {
		return twfs;
	}
	
	public IntersectionOfConceptSets getRootConcepts() {
		return rootConcepts;
	}
	
	/**
	 * Set<NewLiteral> getRoots()
	 * 
	 * @return set of roots of the tree witness
	 */
	public Set<NewLiteral> getRoots() {
		return terms.roots;
	}
	
	/**
	 * boolean isMergeable()
	 * 
	 * @return true if all root NewLiterals are quantified variables and there is the intersection of root concepts is non-empty
	 */
	public boolean isMergeable() {
		return !rootConcepts.isEmpty();
	}
	
	/**
	 * Set<NewLiteral> getDomain()
	 * 
	 * @return the domain (set of NewLiterals) of the tree witness
	 */
	
	public Set<NewLiteral> getDomain() {
		return terms.domain;
	}
	
	public TermCover getTerms() {
		return terms;
	}
	
	/**
	 * Set<TreeWitnessGenerator> getGenerator()
	 * 
	 * @return the tree witness generators \exists R.B
	 */
	
	public Collection<TreeWitnessGenerator> getGenerators() {
		return gens;
	}
	
	
	/**
	 * Set<Function> getRootAtoms()
	 * 
	 * @return query atoms with all NewLiterals among the roots of tree witness
	 */
	
	public Set<Function> getRootAtoms() {
		return rootAtoms;
	}

	/**
	 * boolean isCompatibleWith(TreeWitness tw1)
	 * 
	 * tree witnesses are compatible if their domains intersect only on their roots
	 *     correct: tree witnesses are consistent iff their domains intersect on their **common** roots
	 * 
	 * @param tw1: a tree witness
	 * @return true if tw1 is compatible with the given tree witness
	 */
	
	public boolean isCompatibleWith(TreeWitness tw1) {
		Set<NewLiteral> NewLiterals = new HashSet<NewLiteral>(getDomain());
		NewLiterals.retainAll(tw1.getDomain());
		if (!NewLiterals.isEmpty()) {
			if (!getRoots().containsAll(NewLiterals) || !tw1.getRoots().containsAll(NewLiterals))
				return false;
		}
		return true;
	}
	
	@Override
	public String toString() {
		return "tree witness generated by " + gens + "\n    with domain " + terms + " and root atoms " + rootAtoms;
	}

	/**
	 * TermCover stores the domain and the set of roots of a tree witness
	 * 
	 * implements methods for efficient comparison and hashing
	 * 
	 * @author Roman Kontchakov
	 *
	 */
	
	public static class TermCover {
		private final Set<NewLiteral> domain; // terms that are covered by the tree witness
		private final Set<NewLiteral> roots;   // terms that are mapped onto the root of the tree witness
		
		public TermCover(Set<NewLiteral> domain, Set<NewLiteral> roots) {
			this.domain = domain;
			this.roots = roots;
		}
		
		public Set<NewLiteral> getDomain() {
			return domain;
		}
		
		public Set<NewLiteral> getRoots() {
			return roots;
		}
		
		@Override
		public String toString() {
			return "tree witness domain " + domain + " with roots " + roots;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (obj instanceof TermCover) {
				TermCover other = (TermCover)obj;
				return this.roots.equals(other.roots) && 
					   this.domain.equals(other.domain);			
			}
			return false;
		}
		
		@Override
		public int hashCode() {
			return roots.hashCode() ^ domain.hashCode(); 
		}
	}

}
