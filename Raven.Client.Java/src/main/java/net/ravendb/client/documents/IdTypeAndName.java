package net.ravendb.client.documents;

import net.ravendb.client.documents.commands.batches.CommandType;

public class IdTypeAndName {
    private String id;
    private CommandType type;
    private String name;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public CommandType getType() {
        return type;
    }

    public void setType(CommandType type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IdTypeAndName that = (IdTypeAndName) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (type != that.type) return false;
        return name != null ? name.equals(that.name) : that.name == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }

    public static IdTypeAndName create(String id, CommandType type, String name) {
        IdTypeAndName idTypeAndName = new IdTypeAndName();
        idTypeAndName.setId(id);
        idTypeAndName.setType(type);
        idTypeAndName.setName(name);
        return idTypeAndName;
    }
}
