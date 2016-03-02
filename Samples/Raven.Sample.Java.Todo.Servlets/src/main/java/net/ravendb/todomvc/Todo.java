package net.ravendb.todomvc;

import java.util.Date;

import com.mysema.query.annotations.QueryEntity;

/**
 * @author Tyczo, AIS.PL
 *
 */
@QueryEntity
public class Todo {

    private Boolean completed;
    private Date creationDate;
    private Integer id;
    private String title;

    public Todo() {
        super();
    }

    public Todo(final String title) {
        super();
        this.title = title;
        this.creationDate = new Date(System.currentTimeMillis());
    }

    public Boolean getCompleted() {
        return completed;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public Integer getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public String toString() {
        return "Todo [title=" + title + ", completed =" + completed + ", creationDate=" + creationDate + ", id=" + id
            + "]";
    }

}
