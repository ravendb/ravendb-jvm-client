package net.ravendb.client.documents.session;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class JavaScriptMapTest {

    @Test
    public void putUsesBracketNotationWithQuotedKey() {
        JavaScriptMap<String, Integer> map = new JavaScriptMap<>(0, "ratings");
        map.put("the key", 5);

        assertThat(map.getScript())
                .isEqualTo("this.ratings[\"the key\"] = args.val_0_0;");
    }

    @Test
    public void removeUsesBracketNotationWithQuotedKey() {
        JavaScriptMap<String, Integer> map = new JavaScriptMap<>(1, "ratings");
        map.remove("a.b");

        assertThat(map.getScript())
                .isEqualTo("delete this.ratings[\"a.b\"];");
    }

    @Test
    public void escapesSpecialCharactersInKey() {
        JavaScriptMap<String, Integer> map = new JavaScriptMap<>(0, "ratings");
        map.put("a\"b", 1);

        assertThat(map.getScript())
                .isEqualTo("this.ratings[\"a\\\"b\"] = args.val_0_0;");
    }
}
