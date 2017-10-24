package net.ravendb.client.serverwide;

import java.util.Dictionary;
import java.util.List;
import java.util.Map;

public class DatabaseTopology {
    private List<String> members;
    private List<String> promotables;
    private List<String> rehabs;

    private Map<String, String> predefinedMentors;
    private Map<String, String> demotionReasons;

    //TODO: other props


    public List<String> getMembers() {
        return members;
    }

    public void setMembers(List<String> members) {
        this.members = members;
    }

    public List<String> getPromotables() {
        return promotables;
    }

    public void setPromotables(List<String> promotables) {
        this.promotables = promotables;
    }

    public List<String> getRehabs() {
        return rehabs;
    }

    public void setRehabs(List<String> rehabs) {
        this.rehabs = rehabs;
    }

    public Map<String, String> getPredefinedMentors() {
        return predefinedMentors;
    }

    public void setPredefinedMentors(Map<String, String> predefinedMentors) {
        this.predefinedMentors = predefinedMentors;
    }

    public Map<String, String> getDemotionReasons() {
        return demotionReasons;
    }

    public void setDemotionReasons(Map<String, String> demotionReasons) {
        this.demotionReasons = demotionReasons;
    }
}
