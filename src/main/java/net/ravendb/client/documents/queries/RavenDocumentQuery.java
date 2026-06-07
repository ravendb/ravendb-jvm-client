package net.ravendb.client.documents.queries;

import net.ravendb.client.documents.session.CmpXchg;
import net.ravendb.client.documents.session.MethodCall;
import net.ravendb.client.documents.session.tokens.WhereToken;

/**
 * Provides static methods for use in DocumentQuery that are translated to RQL server-side operations.
 */
public class RavenDocumentQuery {

    private RavenDocumentQuery() {
    }

    /**
     * Returns the current UTC date and time on the server. Translates to the {@code now()} RQL function.
     * For use with {@code whereEquals}, {@code whereGreaterThan}, and other DocumentQuery filter methods.
     *
     * @return method call translated server-side
     */
    public static MethodCall now() {
        return Time.NOW_INSTANCE;
    }

    /**
     * Returns the current UTC date and time on the server, adjusted by the specified offset and floor-rounded
     * to the smallest precision unit. Translates to the {@code now(offset)} RQL function.
     *
     * @param offset a duration string representing the time offset. The result is floor-rounded to the smallest
     *               unit specified.
     *               <p>
     *               Format: {@code [+|-]Ny[Nmo][Nd][Nh][Nm][Ns]} - units must appear in descending order
     *               (year to second). Not all units are required; only the ones you need. Spaces between
     *               components are allowed.
     *               <p>
     *               Each unit supports aliases:
     *               {@code [+|-]N(y|year|years)[N(mo|month|months)][N(d|day|days)][N(h|hour|hours)][N(m|min|minute|minutes)][N(s|sec|second|seconds)]}
     *               <p>
     *               Examples: {@code "+1y6mo"}, {@code "-2hours30minutes"}, {@code "1 year 6 months"},
     *               {@code "15d"} (defaults to positive).
     * @return method call translated server-side
     */
    public static MethodCall now(String offset) {
        return new Time(WhereToken.MethodsType.NOW, offset);
    }

    /**
     * Returns the start of the current UTC day (midnight) on the server. Translates to the {@code today()} RQL function.
     * For use with {@code whereEquals}, {@code whereGreaterThan}, and other DocumentQuery filter methods.
     *
     * @return method call translated server-side
     */
    public static MethodCall today() {
        return Time.TODAY_INSTANCE;
    }

    /**
     * Retrieves a compare exchange value by key for use in a query filter. Translates to the {@code cmpxchg()} RQL function.
     *
     * @param key the key of the compare exchange value
     * @return method call translated server-side
     */
    public static MethodCall cmpXchg(String key) {
        CmpXchg cmpXchg = new CmpXchg();
        cmpXchg.args = new Object[]{ key };
        return cmpXchg;
    }

    public static final class Time extends MethodCall {
        static final Time NOW_INSTANCE = new Time(WhereToken.MethodsType.NOW);
        static final Time TODAY_INSTANCE = new Time(WhereToken.MethodsType.TODAY);

        private final WhereToken.MethodsType methodType;

        private Time(WhereToken.MethodsType methodType) {
            this.methodType = methodType;
            this.args = new Object[0];
        }

        private Time(WhereToken.MethodsType methodType, String offset) {
            this.methodType = methodType;
            this.args = new Object[]{ offset };
        }

        public WhereToken.MethodsType getMethodType() {
            return methodType;
        }
    }
}
