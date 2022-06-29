package bot.inker.onemirror.middie.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "project_download_entity")
public class ProjectDownloadEntity {
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

    @OneToOne
    @JoinColumn(name = "build_id")
    private ProjectBuildEntity build;

    @Column(name="name")
    private String name;

    @Column(name="hash")
    private String hash;

    @Column(name="download_url")
    private String downloadUrl;

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

    public ProjectBuildEntity getBuild() {
        return build;
    }

    public void setBuild(ProjectBuildEntity build) {
        this.build = build;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public SyncStatus getStatus() {
        return status;
    }

    public void setStatus(SyncStatus status) {
        this.status = status;
    }
}