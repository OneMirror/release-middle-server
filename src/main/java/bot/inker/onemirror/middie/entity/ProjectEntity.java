package bot.inker.onemirror.middie.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "project_entity")
public class ProjectEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id", nullable = false)
    private Integer id;

    @Column(name="name")
    private String name;

    @Column(name="status")
    @Enumerated(EnumType.STRING)
    private SyncStatus status = SyncStatus.SYNCING;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public SyncStatus getStatus() {
        return status;
    }

    public void setStatus(SyncStatus status) {
        this.status = status;
    }
}