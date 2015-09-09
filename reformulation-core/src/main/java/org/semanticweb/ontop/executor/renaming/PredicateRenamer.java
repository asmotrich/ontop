package org.semanticweb.ontop.executor.renaming;

import org.semanticweb.ontop.model.AtomPredicate;
import org.semanticweb.ontop.model.DataAtom;
import org.semanticweb.ontop.model.OBDADataFactory;
import org.semanticweb.ontop.model.impl.OBDADataFactoryImpl;
import org.semanticweb.ontop.pivotalrepr.*;
import org.semanticweb.ontop.pivotalrepr.impl.ConstructionNodeImpl;

/**
 * TODO: explain
 *
 * Immutable
 */
public class PredicateRenamer implements QueryNodeTransformer {

    private final static OBDADataFactory DATA_FACTORY = OBDADataFactoryImpl.getInstance();
    private final AtomPredicate formerPredicate;
    private final AtomPredicate newPredicate;


    public PredicateRenamer(AtomPredicate formerPredicate, AtomPredicate newPredicate) {
        this.formerPredicate = formerPredicate;
        this.newPredicate = newPredicate;
    }

    @Override
    public FilterNode transform(FilterNode filterNode) throws QueryNodeTransformationException {
        return filterNode;
    }

    @Override
    public TableNode transform(TableNode tableNode) throws QueryNodeTransformationException {
        return tableNode;
    }

    @Override
    public LeftJoinNode transform(LeftJoinNode leftJoinNode) throws QueryNodeTransformationException {
        return leftJoinNode;
    }

    @Override
    public UnionNode transform(UnionNode unionNode) throws QueryNodeTransformationException {
        return unionNode;
    }

    @Override
    public OrdinaryDataNode transform(OrdinaryDataNode ordinaryDataNode) throws QueryNodeTransformationException {
        return ordinaryDataNode;
    }

    @Override
    public InnerJoinNode transform(InnerJoinNode innerJoinNode) throws QueryNodeTransformationException {
        return innerJoinNode;
    }

    @Override
    public ConstructionNode transform(ConstructionNode formerNode) throws QueryNodeTransformationException {
        DataAtom currentAtom = formerNode.getProjectionAtom();
        AtomPredicate currentPredicate = currentAtom.getPredicate();

        /**
         * Makes a replacement proposal for the construction node
         */
        if (currentPredicate.equals(formerPredicate)) {
            DataAtom newDataAtom = DATA_FACTORY.getDataAtom(newPredicate,
                    formerNode.getProjectionAtom().getVariablesOrGroundTerms());

            return new ConstructionNodeImpl(newDataAtom, formerNode.getSubstitution(),
                    formerNode.getOptionalModifiers());
        }
        else {
            return formerNode;
        }
    }

    @Override
    public GroupNode transform(GroupNode groupNode) throws QueryNodeTransformationException, NotNeededNodeException {
        return groupNode;
    }
}
