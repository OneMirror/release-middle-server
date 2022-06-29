package bot.inker.onemirror.middie.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "project_build_entity")
public class ProjectBuildEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id", nullable = false)
    private Integer id;

    @OneToOne
    @JoinColumn(name = "project_id")
    private ProjectEntity project;

    @OneToOne
    @JoinColumn(name = "version_id")
    private ProjectVersionEntity version;

    @Column(name = "build_number")
    private Integer buildNumber;

    @Column(name="change_list", length = 32768)
    private String change;

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

    public ProjectVersionEntity getVersion() {
        return version;
    }

    public void setVersion(ProjectVersionEntity version) {
        this.version = version;
    }

    public Integer getBuildNumber() {
        return buildNumber;
    }

    public void setBuildNumber(Integer buildNumber) {
        this.buildNumber = buildNumber;
    }

    public String getChange() {
        return change;
    }

    public void setChange(String change) {
        this.change = change;
    }

    public SyncStatus getStatus() {
        return status;
    }

    public void setStatus(SyncStatus status) {
        this.status = status;
    }
}