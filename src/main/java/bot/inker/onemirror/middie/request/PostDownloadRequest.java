package bot.inker.onemirror.middie.request;

import bot.inker.onemirror.middie.entity.SyncStatus;

public class PostDownloadRequest {
    public SyncStatus status;

    public String commitMessage;
    public String hash;
}
