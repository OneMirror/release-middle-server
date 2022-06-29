package bot.inker.onemirror.middie.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "project_version_entity")
public class ProjectVersionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id", nullable = false)
    private Integer id;

    @OneToOne
    @JoinColumn(name = "project_id")
    private ProjectEntity project;

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

    public ProjectEntity getProject() {
        return project;
    }

    public void setProject(ProjectEntity project) {
        this.project = project;
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