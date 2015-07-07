package net.ravendb.client;

import com.mysema.query.types.Expression;
import net.ravendb.abstractions.replication.ReplicationClientConfiguration;
import net.ravendb.client.document.CustomQueryExpressionTranslator;
import net.ravendb.client.document.FailoverBehaviorSet;
import net.ravendb.client.linq.LinqPathProvider;

import java.util.ArrayList;
import java.util.List;

public class QueryConvention extends ConventionBase {
    private String identityPartsSeparator;
    private boolean saveEnumsAsIntegers;

    private List<CustomQueryExpressionTranslator> customQueryTranslators = new ArrayList<>();

    public void registerCustomQueryTranslator(CustomQueryExpressionTranslator translator) {
        customQueryTranslators.add(translator);
    }

    public LinqPathProvider.Result translateCustomQueryExpression(LinqPathProvider provider, Expression<?> expression) {
        for (CustomQueryExpressionTranslator translator: customQueryTranslators) {
            if (translator.canTransform(expression)) {
                return translator.translate(expression);
            }
        }
        return null;
    }

    public void updateFrom(ReplicationClientConfiguration configuration) {
        if (configuration == null) {
            return;
        }
        if (configuration.getFailoverBehavior() != null) {
            setFailoverBehavior(FailoverBehaviorSet.of(configuration.getFailoverBehavior()));
        }
    }

    /**
     * Gets the identity parts separator used by the HiLo generators
     */
    public String getIdentityPartsSeparator() {
        return identityPartsSeparator;
    }

    /**
     * Sets the identity parts separator used by the HiLo generators
     */
    public void setIdentityPartsSeparator(String identityPartsSeparator) {
        this.identityPartsSeparator = identityPartsSeparator;
    }

    /**
     * Saves Enums as integers and instruct the Linq provider to query enums as integer values.
     */
    public boolean isSaveEnumsAsIntegers() {
        return saveEnumsAsIntegers;
    }

    /**
     * Saves Enums as integers and instruct the Linq provider to query enums as integer values.
     * @param saveEnumsAsIntegers
     */
    public void setSaveEnumsAsIntegers(boolean saveEnumsAsIntegers) {
        this.saveEnumsAsIntegers = saveEnumsAsIntegers;
    }
}
