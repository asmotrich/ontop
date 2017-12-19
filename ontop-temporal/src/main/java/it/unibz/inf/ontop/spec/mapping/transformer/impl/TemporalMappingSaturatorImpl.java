package it.unibz.inf.ontop.spec.mapping.transformer.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.TreeTraverser;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import it.unibz.inf.ontop.datalog.TargetAtom;
import it.unibz.inf.ontop.dbschema.DBMetadata;
import it.unibz.inf.ontop.injection.TemporalIntermediateQueryFactory;
import it.unibz.inf.ontop.iq.IntermediateQuery;
import it.unibz.inf.ontop.iq.node.ConstructionNode;
import it.unibz.inf.ontop.iq.node.QueryNode;
import it.unibz.inf.ontop.model.atom.DistinctVariableOnlyDataAtom;
import it.unibz.inf.ontop.model.term.*;
import it.unibz.inf.ontop.model.term.functionsymbol.ExpressionOperation;
import it.unibz.inf.ontop.spec.mapping.Mapping;
import it.unibz.inf.ontop.spec.mapping.QuadrupleDefinition;
import it.unibz.inf.ontop.spec.mapping.TemporalMapping;
import it.unibz.inf.ontop.spec.mapping.transformer.TemporalMappingSaturator;
import it.unibz.inf.ontop.spec.ontology.TBoxReasoner;
import it.unibz.inf.ontop.temporal.iq.TemporalIntermediateQueryBuilder;
import it.unibz.inf.ontop.temporal.model.*;
import it.unibz.inf.ontop.temporal.model.impl.StaticAtomicExpressionImpl;
import it.unibz.inf.ontop.temporal.model.impl.TemporalAtomicExpressionImpl;
import it.unibz.inf.ontop.utils.ImmutableCollectors;
import it.unibz.inf.ontop.temporal.datalog.impl.DatalogMTLConversionTools;
import java.util.*;

@Singleton
public class TemporalMappingSaturatorImpl implements TemporalMappingSaturator{

    private  final TemporalIntermediateQueryFactory TIQFactory;
    private final TermFactory termFactory;
    private final DatalogMTLConversionTools datalogMTLConversionTools;

    @Inject
    private TemporalMappingSaturatorImpl(TemporalIntermediateQueryFactory TIQFactory, TermFactory termFactory, DatalogMTLConversionTools datalogMTLConversionTools) {
        this.TIQFactory = TIQFactory;
        this.termFactory = termFactory;
        this.datalogMTLConversionTools = datalogMTLConversionTools;
    }

    @Override
    public Mapping saturate(Mapping mapping, DBMetadata dbMetadata, TBoxReasoner saturatedTBox) {
        return null;
    }

    public Mapping saturate(Mapping mapping, DBMetadata dbMetadata, TemporalMapping temporalMapping, DBMetadata temporalDBMetadata, DatalogMTLProgram datalogMTLProgram){


        //List<TargetQueryParser> parsers = OntopNativeTemporalMappingParser.createParsers(datalogMTLProgram.getPrefixes());

        TemporalIntermediateQueryBuilder TIQBuilder = TIQFactory.createTemporalIQBuilder(temporalDBMetadata, temporalMapping.getExecutorRegistry());

        Queue<DatalogMTLRule> queue = new LinkedList<>();

        queue.addAll(datalogMTLProgram.getRules());

        while(!queue.isEmpty()){
            DatalogMTLRule rule = queue.poll();
            ImmutableList<AtomicExpression> atomicExpressionsList = getAtomicExpressions(rule);
            if (areAllMappingsExist(mapping,temporalMapping,atomicExpressionsList)){
                saturateRule(rule, mapping, dbMetadata, temporalMapping, temporalDBMetadata, TIQBuilder);
            }else if(!queue.isEmpty()){
                //TODO:Override compareTo for rule.getHead()
                if (queue.stream().anyMatch(qe-> qe.getHead().equals(rule.getHead()))){
                    queue.add(rule);
                }
            }else{
                //TODO: ignore "rule"
            }
        }
        return null;
    }

    private boolean areAllMappingsExist(Mapping mapping, TemporalMapping temporalMapping,
                                        ImmutableList<AtomicExpression> atomicExpressionsList){

        if (atomicExpressionsList.stream().filter(ae-> !(ae instanceof ComparisonExpression))
                .allMatch(ae -> mapping.getDefinition(ae.getPredicate()).isPresent() || temporalMapping.getDefinitions().containsKey(ae.getPredicate())))
            return true;

        return false;
    }

    private void saturateRule(DatalogMTLRule rule, Mapping mapping, DBMetadata dbMetadata, TemporalMapping temporalMapping, DBMetadata temporalDBMetadata, TemporalIntermediateQueryBuilder TIQBuilder){
        TreeTraverser treeTraverser = TreeTraverser.using(DatalogMTLExpression::getChildNodes);
        Iterable<DatalogMTLExpression> it = treeTraverser.preOrderTraversal(rule.getBody());
        Stack<DatalogMTLExpression> teStack = new Stack<>();
        it.iterator().forEachRemaining(dMTLexp -> teStack.push(dMTLexp));
        Stack<QueryNode> qnStack = new Stack<>();

        if(!teStack.empty()) {
            ImmutableMap<Variable, Term> varMap = retrieveMapForVariablesOccuringInTheHead(rule, mapping, temporalMapping);
            AtomicExpression atomicExpression;

            if (rule.getHead() instanceof TemporalAtomicExpression) {
                atomicExpression = new TemporalAtomicExpressionImpl(rule.getHead().getPredicate(), varMap.values().asList());
            } else {
                atomicExpression = new StaticAtomicExpressionImpl(rule.getHead().getPredicate(), varMap.values().asList());
            }

            TargetAtom targetAtom = datalogMTLConversionTools.convertFromDatalogDataAtom(termFactory.getFunction(atomicExpression.getPredicate(), ((List<Term>) atomicExpression.getTerms())));
            DistinctVariableOnlyDataAtom projectionAtom = targetAtom.getProjectionAtom();
            ConstructionNode constructionNode = TIQFactory.createConstructionNode(projectionAtom.getVariables(),
                    targetAtom.getSubstitution(), Optional.empty());
            TIQBuilder.init(projectionAtom, constructionNode);

            while (!teStack.isEmpty()) {
                DatalogMTLExpression currentExpression = teStack.pop();
                QueryNode newNode;

                //TODO: Coalesce Node is missing, implement it.
                if(currentExpression instanceof AtomicExpression) {

                    if (currentExpression instanceof ComparisonExpression){
                        continue;
                    } else if (currentExpression instanceof StaticAtomicExpression)
                        newNode = mapping.getDefinition(((StaticAtomicExpression) currentExpression).getPredicate()).get().getRootNode();
                    else //TemporalAtomicExpression
                        newNode = temporalMapping.getDefinitions().get(((TemporalAtomicExpression) currentExpression).getPredicate()).getQuadruple().getIntermediateQuery().getRootNode();

                    QueryNode coalesceNode = TIQFactory.createTemporalCoalesceNode();
                    TIQBuilder.addChild(coalesceNode, newNode);
                    qnStack.push(coalesceNode);

                }else if(currentExpression instanceof TemporalJoinExpression){
                    QueryNode qn1 = qnStack.pop();
                    QueryNode qn2 = qnStack.pop();
                    newNode = TIQFactory.createTemporalJoinNode();
                    TIQBuilder.addChild(newNode, qn1);
                    TIQBuilder.addChild(newNode, qn2);
                    qnStack.push(newNode);

                }else if(currentExpression instanceof StaticJoinExpression){
                    QueryNode qn1 = qnStack.pop();
                    QueryNode qn2 = qnStack.pop();
                    newNode = TIQFactory.createInnerJoinNode();
                    TIQBuilder.addChild(newNode, qn1);
                    TIQBuilder.addChild(newNode, qn2);
                    qnStack.push(newNode);

                }else if (currentExpression instanceof FilterExpression){
                    newNode = TIQFactory.createFilterNode(comparisonExpToFilterCondition(((FilterExpression) currentExpression).getComparisonExpression()));
                    QueryNode qn1 = qnStack.pop();
                    TIQBuilder.addChild(newNode, qn1);

                }else if(currentExpression instanceof UnaryTemporalExpression && currentExpression instanceof TemporalExpressionWithRange){

                    if(currentExpression instanceof BoxMinusExpression)
                         newNode = TIQFactory.createBoxMinusNode(((BoxMinusExpression) currentExpression).getRange());

                    else if(currentExpression instanceof BoxPlusExpression)
                         newNode = TIQFactory.createBoxPlusNode(((BoxPlusExpression) currentExpression).getRange());

                    else if(currentExpression instanceof DiamondMinusExpression)
                         newNode = TIQFactory.createDiamondMinusNode(((DiamondMinusExpression)currentExpression).getRange());
                    else
                        newNode = TIQFactory.createDiamondPlusNode(((DiamondPlusExpression) currentExpression).getRange());

                    QueryNode qn = qnStack.pop();
                    TIQBuilder.addChild(newNode, qn);
                    qnStack.push(newNode);

                    //TODO: fill here
                }else if(currentExpression instanceof BinaryTemporalExpression && currentExpression instanceof TemporalExpressionWithRange) {

                    if (currentExpression instanceof SinceExpression){

                    }
                    else{

                    }
                }
            }
            TIQBuilder.build();
        }else{
            //TODO:????
        }

    }

    private ImmutableExpression comparisonExpToFilterCondition(ComparisonExpression comparisonExpression){
        String operator = comparisonExpression.getPredicate().getName();
        ExpressionOperation expressionOperation = null;
        if(operator == ExpressionOperation.LT.getName())
            expressionOperation = ExpressionOperation.LT;
        else if(operator == ExpressionOperation.GT.getName())
            expressionOperation = ExpressionOperation.GT;
        else if(operator == ExpressionOperation.EQ.getName())
            expressionOperation = ExpressionOperation.EQ;
        else if(operator == ExpressionOperation.NEQ.getName())
            expressionOperation = ExpressionOperation.NEQ;

        return termFactory.getImmutableExpression(expressionOperation,comparisonExpression.getLeftOperand(), comparisonExpression.getRightOperand());
    }

    private ImmutableMap<Variable, Term> retrieveMapForVariablesOccuringInTheHead(DatalogMTLRule rule, Mapping mapping, TemporalMapping temporalMapping){
        Map <Variable, Term> varMap = new HashMap<>();
        ImmutableList<AtomicExpression> atomicExpressionsList = getAtomicExpressions(rule);
        rule.getHead().getImmutableTerms().forEach(term ->{
            if(term instanceof Variable){
                for(AtomicExpression ae :atomicExpressionsList){
                    int varIdxInBody = 0;
                    for(Term t : ae.getImmutableTerms()){
                        if (t instanceof Variable) {
                            //TODO:Override compareTo for Variable
                            if(((Variable) t).equals(term)){
                                if(mapping.getPredicates().contains(ae.getPredicate())){
                                    int varIdxInSub = 0;
                                    Optional<IntermediateQuery> iq = mapping.getDefinition(ae.getPredicate());
                                        for(ImmutableTerm subTerm : ((ConstructionNode)iq.get().getRootNode()).getSubstitution().getImmutableMap().values()){
                                            if(varIdxInBody == varIdxInSub){
                                                varMap.put((Variable) t,(NonGroundFunctionalTerm) subTerm);
                                            }
                                           varIdxInSub++;
                                    }
                                } else if (temporalMapping.getPredicates().contains(ae.getPredicate())) {
                                    int varIdxInSub = 0;
                                    QuadrupleDefinition qd = temporalMapping.getDefinitions().get(ae.getPredicate());
                                    for(ImmutableTerm subTerm : ((ConstructionNode)qd.getQuadruple().getIntermediateQuery().getRootNode()).getSubstitution().getImmutableMap().values()){
                                        if(varIdxInBody == varIdxInSub){
                                            varMap.put((Variable) t,(NonGroundFunctionalTerm) subTerm);
                                        }
                                        varIdxInSub++;
                                    }

                                }else{
                                    //TODO:throw exception;
                                }
                            }

                        }
                        varIdxInBody++;
                    }
                }
            }
        });
        return ImmutableMap.copyOf(varMap);
    }

    //TODO: fill this function
    private DBMetadata mergeStaticDBMetadataintoTemporalDBMetadata(DBMetadata staticDBMetadata, DBMetadata temporaDBMetadata){
        return null;
    }

    private ImmutableList<AtomicExpression> getAtomicExpressions(DatalogMTLRule rule){
        return TreeTraverser.using(DatalogMTLExpression::getChildNodes).postOrderTraversal(rule.getBody()).stream()
                .filter(dMTLexp -> dMTLexp instanceof AtomicExpression)
                .map(dMTLexp -> (AtomicExpression) dMTLexp)
                .collect(ImmutableCollectors.toList());
    }

}