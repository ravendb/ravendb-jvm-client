package net.ravendb.client.serverwide;

import java.util.Date;

public class ScriptResolver {
    private String script;
    private Date lastModifiedTime;

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    public Date getLastModifiedTime() {
        return lastModifiedTime;
    }

    public void setLastModifiedTime(Date lastModifiedTime) {
        this.lastModifiedTime = lastModifiedTime;
    }
}
