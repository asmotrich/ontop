package it.unibz.inf.ontop.utils.impl;

import com.google.common.collect.ImmutableSet;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import it.unibz.inf.ontop.datalog.CQIE;
import it.unibz.inf.ontop.model.term.TermFactory;
import it.unibz.inf.ontop.model.term.Variable;
import it.unibz.inf.ontop.utils.VariableGenerator;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Old-style variable generator
 *
 * TODO: remove it!
 */
public class LegacyVariableGenerator implements VariableGenerator {

    private int count;
    private final Set<Variable> knownVariables;

    private static String SUFFIX_PREFIX = "f";
    private final TermFactory termFactory;


    @AssistedInject
    private LegacyVariableGenerator(@Assisted Collection<Variable> knownVariables, TermFactory termFactory) {
        this.termFactory = termFactory;
        count = 0;
        this.knownVariables = new HashSet<>(knownVariables);
    }

    /**
     * Rule-level variable generator.
     *
     * TODO: remove it
     */
    public LegacyVariableGenerator(CQIE initialRule, TermFactory termFactory) {
        this.termFactory = termFactory;
        count = 0;
        knownVariables = initialRule.getReferencedVariables();
    }

    /**
     * Declares additional variables as known.
     */
    public void registerAdditionalVariables(Collection<Variable> additionalVariables) {
        knownVariables.addAll(additionalVariables);
    }

    /**
     * Generates a new non-conflicting variable from a previous one.
     * It will reuse its name.
     */
    public Variable generateNewVariableFromVar(Variable previousVariable) {
        Variable newVariable;
        do {
            newVariable = termFactory.getVariable(previousVariable.getName() + SUFFIX_PREFIX + (count++));
        } while(knownVariables.contains(newVariable));

        knownVariables.add(newVariable);
        return newVariable;
    }

    /**
     * Generates a new variable if a conflict is detected.
     */
    public Variable generateNewVariableIfConflicting(Variable previousVariable) {
        Variable newVariable = previousVariable;
        while(knownVariables.contains(newVariable)) {
            newVariable = termFactory.getVariable(previousVariable.getName() + SUFFIX_PREFIX + (count++));
        }

        knownVariables.add(newVariable);
        return newVariable;
    }

    /**
     * Generates a new non-conflicting variable.
     */
    public Variable generateNewVariable() {
        Variable newVariable;
        do {
            newVariable = termFactory.getVariable(SUFFIX_PREFIX + (count++));
        } while(knownVariables.contains(newVariable));

        knownVariables.add(newVariable);
        return newVariable;
    }

    /**
     * Instant snapshot of variable it knows.
     */
    public ImmutableSet<Variable> getKnownVariables() {
        return ImmutableSet.copyOf(knownVariables);
    }

    @Override
    public VariableGenerator clone() {
        return new LegacyVariableGenerator(getKnownVariables(), termFactory);
    }
}
