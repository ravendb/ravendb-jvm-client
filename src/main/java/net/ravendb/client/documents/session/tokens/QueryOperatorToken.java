package net.ravendb.client.documents.session.tokens;

import net.ravendb.client.documents.queries.QueryOperator;

public class QueryOperatorToken extends QueryToken {

    private final QueryOperator _queryOperator;

    private QueryOperatorToken(QueryOperator queryOperator) {
        _queryOperator = queryOperator;
    }

    public static final QueryOperatorToken AND = new QueryOperatorToken(QueryOperator.AND);

    public static final QueryOperatorToken OR = new QueryOperatorToken(QueryOperator.OR);

    @Override
    public void writeTo(StringBuilder writer) {
        if (_queryOperator == QueryOperator.AND) {
            writer.append("and");
            return;
        }

        writer.append("or");
    }
}
